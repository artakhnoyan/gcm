package com.example.arthurakhnoyan.gcm.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.arthurakhnoyan.gcm.ui.ConversationActivity;
import com.example.arthurakhnoyan.gcm.R;
import com.example.arthurakhnoyan.gcm.models.Message;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by arthurakhnoyan on 6/20/16.
 */
public class GCMPushReceiverService extends GcmListenerService {
    public static final String HANDLE_MESSAGE = "com.example.arthurakhnoyan.gcm.HANDLE_MESSAGE";
    public static final String HISTORY = "history";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d("data", "onMessageReceived: " + data.toString());
        String message = data.getString("message");
        String user = data.getString("user");
        String userId = data.getString("user_id");
        writeToSharedPreferances(message, userId);
        sendToActivity(message,userId, false);
        sendNotification(userId, user, message);
        Log.d("IDDD", "onMessageReceived: " + userId);
    }

    private void sendToActivity(String message, String userId, boolean position) {
        Intent intent = new Intent(HANDLE_MESSAGE);
        intent.putExtra("user_id", userId);
        intent.putExtra("message", message);
        intent.putExtra("position", position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void writeToSharedPreferances(String message, String userId) {
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


        arrayList.add(new Message(message, false));

        String json = gson.toJson(arrayList);

        editor.putString(GCMPushReceiverService.HISTORY + userId, json);
        editor.commit();
    }

    private void sendNotification(String userId, String user, String message) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("user_id", Integer.valueOf(userId));
        Log.d("not_user_id", "sendNotification: " + userId);
        int requestCode = 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder noBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(user)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(sound);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, noBuilder.build());
    }
}

