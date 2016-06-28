package com.example.arthurakhnoyan.gcm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.arthurakhnoyan.gcm.R;
import com.example.arthurakhnoyan.gcm.models.User;

import java.util.List;

/**
 * Created by arthurakhnoyan on 6/27/16.
 */
public class UserAdapter  extends ArrayAdapter<User>{

    public UserAdapter(Context context, List<User> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        User user = getItem(position);

        convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_item, parent, false);

        TextView username = (TextView) convertView.findViewById(R.id.user_username);
        TextView phone = (TextView) convertView.findViewById(R.id.user_phone);
        username.setText(user.getUsername());
        phone.setText(user.getPhone());

        return convertView;
    }
}
