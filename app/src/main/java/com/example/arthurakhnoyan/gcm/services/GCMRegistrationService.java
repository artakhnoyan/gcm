package com.example.arthurakhnoyan.gcm.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.arthurakhnoyan.gcm.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by arthurakhnoyan on 6/20/16.
 */
public class GCMRegistrationService extends IntentService {
    public static final String REGISTRATION_SUCCESS = "RegistrationSuccess";
    public static final String REGISTRATION_ERROR = "RegistrationError";
    public static final String TAG = "GCMTOKEN";
    private String responseString = null;

    public GCMRegistrationService() {
        super("");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        registerGCM(intent);
    }

    private void registerGCM(Intent getData) {
        SharedPreferences sharedPreferences = getSharedPreferences("GCM", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Intent registrationComplete = null;
        String token = null;
        try {
            InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.w("GCMRegIntentService", "token:" + token);
            registrationComplete = new Intent(REGISTRATION_SUCCESS);
            registrationComplete.putExtra("token", token);

            String oldToken = sharedPreferences.getString(TAG, "");
            if (!"".equals(token) && !oldToken.equals(token)) {
                saveTokenToServer(token, getData);
                editor.putString(TAG, token);
                editor.commit();
            } else {
                Log.w("GCMRegistrationService", "Old token");
            }
        } catch (Exception e) {
            Log.w("GCMRegIntentService", "Registration error");
            registrationComplete = new Intent(REGISTRATION_ERROR);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void saveTokenToServer(String token, Intent intent) {

        String username = intent.getStringExtra("username");
        String phone = intent.getStringExtra("phone");

        RequestBody paramPost = new FormBody.Builder()
                .add("action", "add")
                .add("tokenid", token)
                .add("username", username)
                .add("phone", phone)
                .build();

        try {
            String msgResult = getStringResultFromService_POST("http://192.168.15.129/gcm/gcm.php", paramPost);
            Log.w("ServiceResponseMsg", msgResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStringResultFromService_POST(String serviceURL, RequestBody params) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(serviceURL)
                .post(params)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: Registration failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                responseString = response.toString();
                Log.d(TAG, "onResponse: " + response.toString());
            }

        });
        return responseString;
    }
}
