package com.pjinkim.arcore_data_logger;

import android.widget.Toast;

import com.google.android.gms.common.api.Status;

import java.util.ArrayList;
import java.util.Map;

// move,i,j,null
// wall,i,j,horizontal
public class GameData {
    public String username;
    public Map<String, ArrayList<Number>> opponentinfo;
    public boolean hit;
    int kill,death;
    public GameData(String username, Map<String, ArrayList<Number>> opponentinfo, boolean hit){
        this.username=username;
        this.opponentinfo=opponentinfo;
        this.hit=hit;
        kill=death=0;
    }
}