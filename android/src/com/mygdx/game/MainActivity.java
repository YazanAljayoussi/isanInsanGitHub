package com.mygdx.game;

import android.app.Activity;
import android.os.Bundle;

import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView mTextMessage;
    private AndroidLauncher androidLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);

    }

}
