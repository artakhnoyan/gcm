package com.example.arthurakhnoyan.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by arthurakhnoyan on 6/20/16.
 */
public class GCMTokenRefreshListenerService extends InstanceIDListenerService {
    /**
     * When token refresh, start service to get new token
     */
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, GCMRegistrationService.class);
        startService(intent);
    }
}

