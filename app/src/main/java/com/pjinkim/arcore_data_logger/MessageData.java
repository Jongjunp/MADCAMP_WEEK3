package com.pjinkim.arcore_data_logger;

import android.widget.Toast;

import com.google.android.gms.common.api.Status;

import java.util.ArrayList;

// move,i,j,null
// wall,i,j,horizontal
public class MessageData {
    public String username,roomname;
    int win,lose;
    String[] pnames = new String[1000];
    public MessageData(String username, String roomname){
        this.username=username;
        this.roomname=roomname;
        win=lose=0;
    }
}