package com.example.arthurakhnoyan.gcm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.arthurakhnoyan.gcm.R;
import com.example.arthurakhnoyan.gcm.models.Message;

import java.util.List;

/**
 * Created by arthurakhnoyan on 6/22/16.
 */
public class ChatAdapter extends ArrayAdapter<Message> {

    public ChatAdapter(Context context, List<Message> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Message message = getItem(position);

        view = LayoutInflater.from(getContext()).inflate(R.layout.message_item, parent, false);




        if (message.getPosition()) {
            TextView myMessage = (TextView) view.findViewById(R.id.my_message);
            myMessage.setVisibility(View.VISIBLE);
            myMessage.setText(message.getMessage());
        } else {
            TextView hisMessage = (TextView) view.findViewById(R.id.his_message);
            hisMessage.setVisibility(View.VISIBLE);
            hisMessage.setText(message.getMessage());
        }

        return view;
    }
}
