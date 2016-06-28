package com.example.arthurakhnoyan.gcm;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by arthurakhnoyan on 6/21/16.
 */
public class BackGroundToast implements Runnable {
    private Context context;
    private String message;

    public BackGroundToast(Context context, String message) {
        this.context = context;
        this.message = message;
    }
    @Override
    public void run() {
        Toast.makeText(this.context, this.message, Toast.LENGTH_LONG).show();
    }
}
