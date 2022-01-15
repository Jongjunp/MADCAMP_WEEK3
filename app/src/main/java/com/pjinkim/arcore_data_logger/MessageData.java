package com.pjinkim.arcore_data_logger;

import android.widget.Toast;

import com.google.android.gms.common.api.Status;

import java.util.ArrayList;

// move,i,j,null
// wall,i,j,horizontal
public class MessageData {
    public String username,roomname,content,detail,victim;
    int win,lose,length;
    double x,y,z,phi,theta;
    String[] pnames = new String[1000];
    public MessageData(String username, String roomname, String content){
        this.username=username;
        this.roomname=roomname;
        this.content=content;
        win=lose=0;
    }
}