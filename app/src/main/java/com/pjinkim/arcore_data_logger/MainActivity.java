package com.pjinkim.arcore_data_logger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;



public class MainActivity extends AppCompatActivity {

    TextView _userName;
    EditText _roomNameEnter;
    Button _enterRoom;
    LinearLayout _enter;
    LinearLayout _search;
    LinearLayout _ready;
    Button _readyButton;
    static String[] pnames;

    String roomNameEnter;

    static Socket mSocket;
    Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //socket communication
        if (mSocket!=null) mSocket.disconnect();
        try {
            mSocket = IO.socket("http://192.249.18.147:80");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.connect();

        //layouts
        _enter = (LinearLayout) findViewById(R.id.enter);
        _search = (LinearLayout) findViewById(R.id.search);
        _ready = (LinearLayout) findViewById(R.id.ready);

        _userName = (TextView) findViewById(R.id.username);
        _userName.setText(LoginActivity.userName);

        _roomNameEnter = (EditText) findViewById(R.id.room_name_enter);
        _enterRoom = (Button) findViewById(R.id.btn_enter);
        _readyButton = (Button) findViewById(R.id.btn_ready);
        //Enter the existing room
        _enterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                roomNameEnter = _roomNameEnter.getText().toString().trim();
                mSocket.emit("enter",gson.toJson(new MessageData(LoginActivity.userName, roomNameEnter, "")));
                _enter.setVisibility(View.INVISIBLE);
                _search.setVisibility(View.VISIBLE);
                mSocket.on("refuse", refuse);
                mSocket.on("roomfound",whenroomfound);
            }
        });
    }

    public Emitter.Listener whenroomfound = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessageData data = gson.fromJson(args[0].toString(), MessageData.class);
                    pnames = data.pnames;
                    //Toast.makeText(MainActivity.this, pnames[0]+" "+pnames[1] ,Toast.LENGTH_LONG).show();
                    _enter.setVisibility(View.INVISIBLE);
                    _search.setVisibility(View.INVISIBLE);
                    _ready.setVisibility(View.VISIBLE);


                    //when ready button pressed
                    _readyButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mSocket.emit("ready",gson.toJson(new MessageData(LoginActivity.userName,roomNameEnter,"")));
                            mSocket.on("accept", accpet);
                        }
                    });

                }
            });
        }
    };

    public Emitter.Listener refuse = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MessageData data = gson.fromJson(args[0].toString(), MessageData.class);
                    if(LoginActivity.userName.equals(data.username)){
                        mSocket.disconnect();
                        Toast.makeText(MainActivity.this,"이 방 번호는 사용중입니다. 다른 방 번호를 선택하세요",Toast.LENGTH_LONG).show();
                        _ready.setVisibility(View.INVISIBLE);
                        _search.setVisibility(View.INVISIBLE);
                        _enter.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    };

    public Emitter.Listener accpet = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //set bundle
                    MessageData data = gson.fromJson(args[0].toString(), MessageData.class);
                    Intent intent = new Intent(MainActivity.this, GameActivity.class) ;
                    startActivity(intent);
                }
            });
        }
    };


    @Override
    public void onBackPressed() {
        if(mSocket!=null)mSocket.disconnect();
        super.onBackPressed();
    }
}