package com.example.arthurakhnoyan.gcm.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.KeyboardView;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arthurakhnoyan.gcm.R;
import com.example.arthurakhnoyan.gcm.services.GCMRegistrationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("GCM", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("GCMTOKEN", null);
        if (token != null) {
            Intent intent = new Intent(this, UsersActivity.class);
            startActivity(intent);
        }

        btn = (Button) findViewById(R.id.reg);
        btn.setOnClickListener(this);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.reg:
                int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
                if (ConnectionResult.SUCCESS != resultCode) {
                    if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                        Toast.makeText(getApplicationContext(), "Google Play Service is not install/enabled in this device!", Toast.LENGTH_LONG).show();
                        GooglePlayServicesUtil.showErrorNotification(resultCode, getApplicationContext());
                    } else {
                        Toast.makeText(getApplicationContext(), "This device does not support for Google Play Service!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if(isNetworkAvailable()) {
                        String username = ((EditText) findViewById(R.id.username)).getText().toString();
                        String phone = ((EditText) findViewById(R.id.phone)).getText().toString();
                        Intent intent = new Intent(this, GCMRegistrationService.class);
                        intent.putExtra("username", username);
                        intent.putExtra("phone", phone);
                        startService(intent);

                        Intent sendingIntent = new Intent(this, UsersActivity.class);
                        startActivity(sendingIntent);
                    } else {
                        Toast.makeText(MainActivity.this, "You are not connected to internet", Toast.LENGTH_SHORT).show();
                    }

                }

                break;
        }
    }
}
