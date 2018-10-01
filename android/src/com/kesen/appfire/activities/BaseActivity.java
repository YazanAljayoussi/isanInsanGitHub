package com.kesen.appfire.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.kesen.appfire.utils.PresenceUtil;

public class BaseActivity extends AppCompatActivity {
    PresenceUtil presenceUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenceUtil = new PresenceUtil();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenceUtil.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenceUtil.onPause();
    }

}
