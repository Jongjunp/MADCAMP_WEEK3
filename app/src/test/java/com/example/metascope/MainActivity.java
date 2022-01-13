package com.example.metascope;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity {

    TextView _userName;
    EditText _roomNameCreate;
    Button _createRoom;
    EditText _roomNameEnter;
    Button _enterRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String id = bundle.getString("id");

        _userName = (TextView) findViewById(R.id.username);
        _userName.setText(id);


        final String roomNameEnter = _roomNameEnter.getText().toString();

        //server connection
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.249.18.163") // 주소는 본인의 서버 주소로 설정
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        var loginService = retrofit.create(LoginService::class.java)


        //Creating the new room
        _createRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String roomNameCreate = _roomNameCreate.getText().toString();
                Intent intent = new Intent(MainActivity.this, ShowActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("roomname", roomNameCreate);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        //Enter the existing room
        _enterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String roomNameEnter = _roomNameEnter.getText().toString();

            }
        });

    }

}
