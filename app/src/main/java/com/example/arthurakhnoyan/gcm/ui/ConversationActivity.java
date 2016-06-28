package com.example.arthurakhnoyan.gcm.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.arthurakhnoyan.gcm.BackGroundToast;
import com.example.arthurakhnoyan.gcm.adapters.ChatAdapter;
import com.example.arthurakhnoyan.gcm.R;
import com.example.arthurakhnoyan.gcm.models.Message;
import com.example.arthurakhnoyan.gcm.services.GCMPushReceiverService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConversationActivity extends AppCompatActivity implements View.OnClickListener{

    private BroadcastReceiver messageBroadcastReceiver;
    private List<Message> messages;
    private ArrayAdapter<Message> adapter;
    private ListView list;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Intent intent = getIntent();
        userId = intent.getIntExtra("user_id", -1);
        Log.d("user_id in C", "onCreate: " + userId);


        findViewById(R.id.send).setOnClickListener(this);
        list = (ListView) findViewById(R.id.messageList);

        SharedPreferences sharedPrefs = getSharedPreferences(GCMPushReceiverService.HISTORY + userId, MODE_PRIVATE);
        Gson gson = new Gson();
        String getSh = sharedPrefs.getString(GCMPushReceiverService.HISTORY + userId, null);
        Type type = new TypeToken<ArrayList<Message>>() {
        }.getType();
        messages = new ArrayList<>();
        if (getSh != null) {
            messages = gson.fromJson(getSh, type);
        }

        adapter = new ChatAdapter(getApplicationContext(), messages);

        list.setAdapter(adapter);

        messageBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("message");
                userId = intent.getIntExtra("user_id", -1);
                boolean position = intent.getBooleanExtra("position", false);
                Message receivedMessage = new Message(message, position);
                attachMEssageToAdapter(receivedMessage);
            }
        };
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void sendMessage(final String message) {
        SharedPreferences sharedPreferences = getSharedPreferences("GCM", Context.MODE_PRIVATE);
        String myToken = sharedPreferences.getString("GCMTOKEN", "");
        if (!message.trim().equals("")) {
            OkHttpClient client = new OkHttpClient();
            Log.d("IID", "sendMessage: " + userId);
            RequestBody body = new FormBody.Builder()
                    .add("action", "send")
                    .add("sender_token", myToken)
                    .add("user_id", String.valueOf(userId))
                    .add("message", message)
                    .build();

            Request request = new Request.Builder()
                    .url("http://192.168.15.129/gcm/sendNotification.php")
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new BackGroundToast(getApplicationContext(), "Something went wrong. Message didn't send. Please try again."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("ARTA", "onResponse: Message sent. Response: " + response.body().string());
                    writeToSharedPreferances(message);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Message sentMessage = new Message(message, true);
                            attachMEssageToAdapter(sentMessage);
                            ((EditText) findViewById(R.id.message)).setText("");
                        }
                    });
                    runOnUiThread(new BackGroundToast(getApplicationContext(), "Message sent"));

                }
            });
        }
    }

    private void writeToSharedPreferances(String message) {
        SharedPreferences sharedPrefs = getSharedPreferences(GCMPushReceiverService.HISTORY + userId, MODE_PRIVATE);
        Gson gson = new Gson();
        String getSh = sharedPrefs.getString(GCMPushReceiverService.HISTORY + userId, null);
        Type type = new TypeToken<ArrayList<Message>>() {
        }.getType();

        ArrayList<Message> arrayList = new ArrayList<>();
        if (getSh != null) {
            arrayList = gson.fromJson(getSh, type);
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();


        arrayList.add(new Message(message, true));

        String json = gson.toJson(arrayList);

        editor.putString(GCMPushReceiverService.HISTORY + userId, json);
        editor.commit();
        Log.d("IDDD", "writeToSharedPreferances: " + userId);
    }

    private void attachMEssageToAdapter(Message message) {
        messages.add(message);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.send:
                final String message = ((EditText) findViewById(R.id.message)).getText().toString();
                if (isNetworkAvailable()) {
                    sendMessage(message);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Send via SMS")
                            .setMessage("You haven't got internet connection. Do you want to send message via SMS?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Log.d("dialog", "onClick: " + whichButton);
                                    String phoneNumber = getIntent().getStringExtra("user_phone");
                                    if (phoneNumber != null) {
                                        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                                        smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
                                        smsIntent.setType("vnd.android-dir/mms-sms");
                                        smsIntent.setData(Uri.parse("sms:" + phoneNumber));
                                        smsIntent.putExtra("sms_body", message);
                                        startActivity(smsIntent);
                                    } else {
                                        Toast.makeText(ConversationActivity.this, "User doesn't have phone number!", Toast.LENGTH_SHORT).show();
                                    }
                                }})
                            .setNegativeButton("No", null).show();



                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w("MainActivity", "onResume");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageBroadcastReceiver, new IntentFilter(GCMPushReceiverService.HANDLE_MESSAGE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("MainActivity", "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageBroadcastReceiver);
    }

}
