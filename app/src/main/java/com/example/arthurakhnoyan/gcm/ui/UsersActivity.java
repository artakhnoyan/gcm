package com.example.arthurakhnoyan.gcm.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.arthurakhnoyan.gcm.BackGroundToast;
import com.example.arthurakhnoyan.gcm.R;
import com.example.arthurakhnoyan.gcm.adapters.UserAdapter;
import com.example.arthurakhnoyan.gcm.models.User;
import com.example.arthurakhnoyan.gcm.services.GCMPushReceiverService;
import com.example.arthurakhnoyan.gcm.services.GCMRegistrationService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UsersActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private BroadcastReceiver messageBroadcastReceiver;
    private ListView listView;
    private List<User> users;
    private ArrayAdapter<User> adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        users = new ArrayList<>();
        getUsers();

        messageBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(GCMRegistrationService.REGISTRATION_SUCCESS)) {
                    String token = intent.getStringExtra("token");
                    Toast.makeText(getApplicationContext(), "You are connected", Toast.LENGTH_LONG).show();
                } else if (intent.getAction().equals(GCMRegistrationService.REGISTRATION_ERROR)) {

                    Toast.makeText(getApplicationContext(), "Something went wrong. Please try connect again.", Toast.LENGTH_LONG).show();
                }
            }
        };
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        User user = (User) parent.getItemAtPosition(position);
        Log.d("user_id", "onItemClick: " + user.getId());
        Intent intent = new Intent(UsersActivity.this, ConversationActivity.class);
        intent.putExtra("user_id", user.getId());
        intent.putExtra("user_phone", user.getPhone());
        startActivity(intent);
    }

    private void getUsers() {
        SharedPreferences sharedPreferences = getSharedPreferences("GCM", Context.MODE_PRIVATE);
        String myToken = sharedPreferences.getString("GCMTOKEN", "");
        OkHttpClient client = new OkHttpClient();
        FormBody body = new FormBody.Builder()
                .add("action", "get_users")
                .add("token", myToken)
                .build();
        Request request = new Request.Builder()
                .url("http://192.168.15.129/gcm/getUsers.php")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new BackGroundToast(getApplicationContext(), "Something went wrong with getting users");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                new BackGroundToast(getApplicationContext(), "Users");
                try {
                    users = new Gson().fromJson(response.body().string(), new TypeToken<ArrayList<User>>() {
                    }.getType());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new UserAdapter(getApplicationContext(), users);
                        listView = (ListView) findViewById(R.id.users);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener(UsersActivity.this);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }


    @Override
    public void onRefresh() {
        getUsers();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        System.exit(0);
    }
}
