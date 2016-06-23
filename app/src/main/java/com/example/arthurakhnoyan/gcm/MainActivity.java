package com.example.arthurakhnoyan.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private BroadcastReceiver messageBroadcastReceiver;
    List<Message> messages;
    ArrayAdapter<Message> adapter;
    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isNetworkAvailable()) {
            Toast.makeText(MainActivity.this, "yes", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "no", Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.send).setOnClickListener(this);
        list = (ListView) findViewById(R.id.messageList);

        SharedPreferences sharedPrefs = getSharedPreferences(GCMPushReceiverService.HISTORY, MODE_PRIVATE);
        Gson gson = new Gson();
        String getSh = sharedPrefs.getString(GCMPushReceiverService.HISTORY, null);
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
                boolean position = intent.getBooleanExtra("position", false);
                Message receivedMessage = new Message(message, position);
                attachMEssageToAdapter(receivedMessage);
            }
        };


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(GCMRegistrationService.REGISTRATION_SUCCESS)) {
                    String token = intent.getStringExtra("token");
                    Toast.makeText(getApplicationContext(), "You are connected", Toast.LENGTH_LONG).show();
                    checkUserPhoneNumber(token);
                } else if (intent.getAction().equals(GCMRegistrationService.REGISTRATION_ERROR)) {

                    Toast.makeText(getApplicationContext(), "Something went wrong. Please try connect again.", Toast.LENGTH_LONG).show();
                }
            }
        };


        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (ConnectionResult.SUCCESS != resultCode) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Toast.makeText(getApplicationContext(), "Google Play Service is not install/enabled in this device!", Toast.LENGTH_LONG).show();
                GooglePlayServicesUtil.showErrorNotification(resultCode, getApplicationContext());
            } else {
                Toast.makeText(getApplicationContext(), "This device does not support for Google Play Service!", Toast.LENGTH_LONG).show();
            }
        } else {
            Intent intent = new Intent(this, GCMRegistrationService.class);
            startService(intent);
        }
    }

    private void attachMEssageToAdapter(Message message) {
        messages.add(message);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w("MainActivity", "onResume");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(GCMRegistrationService.REGISTRATION_SUCCESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(GCMRegistrationService.REGISTRATION_ERROR));
        LocalBroadcastManager.getInstance(this).registerReceiver(messageBroadcastReceiver, new IntentFilter(GCMPushReceiverService.HANDLE_MESSAGE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("MainActivity", "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageBroadcastReceiver);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.send:
                String message = ((EditText) findViewById(R.id.message)).getText().toString();
                if (isNetworkAvailable()) {
                    sendMessage(message);
                } else {
                    SharedPreferences sharedPreferences = getSharedPreferences("phone_number", MODE_PRIVATE);
                    String phoneNumber = sharedPreferences.getString("phone", null);
                    if (phoneNumber != null) {
                        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                        smsIntent.setType("vnd.android-dir/mms-sms");
                        smsIntent.putExtra("address", phoneNumber);
                        smsIntent.putExtra("sms_body", message);
                        startActivity(smsIntent);
                    } else {
                        Toast.makeText(MainActivity.this, "User doesn't have phone number!", Toast.LENGTH_SHORT).show();
                    }


                }
                break;
        }
    }

    private void checkUserPhoneNumber(String token) {

        SharedPreferences sharedPreferences = getSharedPreferences("phone_number", MODE_PRIVATE);
        String phone_number = sharedPreferences.getString("phone", null);

        if (phone_number == null) {

            OkHttpClient client = new OkHttpClient();
            FormBody body = new FormBody.Builder()
                    .add("action", "phone_number")
                    .add("token", token)
                    .build();

            Request request = new Request.Builder()
                    .url("http://192.168.15.129/gcm/phone_number.php")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new BackGroundToast(getApplicationContext(), "Something went wrong. Please connect again."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String resp = response.body().string();
                    Log.d("Phone", "onResponse: " + resp);
                    SharedPreferences sharedPreferences = getSharedPreferences("phone_number", MODE_PRIVATE);
                    sharedPreferences.edit().putString("phone", resp).commit();
                }
            });
        } else {
            Log.d("MainActivity", "checkUserPhoneNumber: phone number is exist in sh!");
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void sendMessage(final String message) {
        SharedPreferences sh = getSharedPreferences("GCM", Context.MODE_PRIVATE);
        String token = sh.getString(GCMRegistrationService.TAG, "");
        if (!message.trim().equals("")) {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("action", "send")
                    .add("token", token)
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
                    Log.d("MainActivity", "onResponse: Message sent. Response: " + response.toString());
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
        SharedPreferences sharedPrefs = getSharedPreferences(GCMPushReceiverService.HISTORY, MODE_PRIVATE);
        Gson gson = new Gson();
        String getSh = sharedPrefs.getString(GCMPushReceiverService.HISTORY, null);
        Type type = new TypeToken<ArrayList<Message>>() {
        }.getType();

        ArrayList<Message> arrayList = new ArrayList<>();
        if (getSh != null) {
            arrayList = gson.fromJson(getSh, type);
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();


        arrayList.add(new Message(message, true));

        String json = gson.toJson(arrayList);

        editor.putString(GCMPushReceiverService.HISTORY, json);
        editor.commit();
    }
}
