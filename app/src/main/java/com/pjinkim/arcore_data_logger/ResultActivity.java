package com.pjinkim.arcore_data_logger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    String username;
    String killername;
    int killnum;
    boolean win;

    TextView _userName;
    TextView _killerName;
    TextView _winLoseMessage;
    Button _returnToMain;

    //message
    String WIN = "WINNER WINNER CHICKEN DINNER!";
    String LOSE = "BETTER LUCK NEXT TIME";

    public static Activity resultActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        MainActivity.mSocket.disconnect();
        //unwrap intent & bundle
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        username = bundle.getString("username");
        killnum = bundle.getInt("killnum");
        killername = bundle.getString("killer");
        win = bundle.getBoolean("win");

        _killerName = (TextView) findViewById(R.id.killername);
        _userName = (TextView) findViewById(R.id.username);
        _winLoseMessage = (TextView) findViewById(R.id.winlosemsg);
        _returnToMain = (Button) findViewById(R.id.btn_return);

        _killerName.setText("날 죽인사람은 \"" + killername+"\"");
        _userName.setText(username);
        if (win) {
            _winLoseMessage.setText(WIN);
        }
        else {
            _winLoseMessage.setText(LOSE);
        }

        _returnToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }
}