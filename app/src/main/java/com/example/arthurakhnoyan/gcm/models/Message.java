package com.example.arthurakhnoyan.gcm.models;

/**
 * Created by arthurakhnoyan on 6/22/16.
 */
public class Message {

    private String message;
    private boolean position;

    public Message (String message, boolean position) {
        this.message = message;
        this.position = position;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getPosition() {
        return position;
    }

    public void setPosition(boolean position) {
        this.position = position;
    }
}
