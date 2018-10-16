package com.kesen.appfire.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.Toast;

import com.cjt2325.cameralibrary.JCameraView;
import com.cjt2325.cameralibrary.ResultCodes;
import com.cjt2325.cameralibrary.listener.ClickListener;
import com.cjt2325.cameralibrary.listener.ErrorListener;
import com.cjt2325.cameralibrary.listener.JCameraListener;
import com.cjt2325.cameralibrary.listener.RecordStartListener;
import com.cjt2325.cameralibrary.util.DeviceUtil;
import com.kesen.echo.R;
import com.kesen.appfire.model.constants.MessageType;
import com.kesen.appfire.utils.BitmapUtils;
import com.kesen.appfire.utils.DirManager;
import com.kesen.appfire.utils.IntentUtils;

import java.io.File;

import me.zhanghai.android.systemuihelper.SystemUiHelper;

public class CameraActivity extends AppCompatActivity {
    private JCameraView jCameraView;
    private Chronometer chronometer;
    SystemUiHelper uiHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        chronometer = findViewById(R.id.chronometer);
        uiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY);

        jCameraView = (JCameraView) findViewById(R.id.jcameraview);
        jCameraView.setSaveVideoPath(DirManager.generateFile(MessageType.SENT_VIDEO).getPath());
        jCameraView.setFeatures(JCameraView.BUTTON_STATE_BOTH);
        jCameraView.setTip(getString(R.string.camera_tip));

        //set media quality
        jCameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_MIDDLE);
        jCameraView.setErrorLisenter(new ErrorListener() {
            @Override
            public void onError() {
                Log.i("CJT", "camera error");
                Intent intent = new Intent();
                setResult(ResultCodes.CAMERA_ERROR_STATE, intent);
                finish();
            }

            @Override
            public void AudioPermissionError() {
                Toast.makeText(CameraActivity.this, R.string.audio_permission_error, Toast.LENGTH_SHORT).show();
            }
        });

        jCameraView.setJCameraLisenter(new JCameraListener() {
            @Override
            public void captureSuccess(Bitmap bitmap) {

                File outputFile = DirManager.generateFile(MessageType.SENT_IMAGE);

                BitmapUtils.convertBitmapToJpeg(bitmap, outputFile);

                String path = outputFile.getPath();
                Intent intent = new Intent();
                intent.putExtra(IntentUtils.EXTRA_PATH_RESULT, path);
                setResult(ResultCodes.IMAGE_CAPTURE_SUCCESS, intent);
                finish();
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {

                Intent intent = new Intent();
                intent.putExtra(IntentUtils.EXTRA_PATH_RESULT, url);
                setResult(ResultCodes.VIDEO_RECORD_SUCCESS, intent);
                finish();
            }

            @Override
            public void quit() {

            }
        });

        jCameraView.setRecordStartListener(new RecordStartListener() {
            @Override
            public void onStart() {
                chronometer.setBase(SystemClock.currentThreadTimeMillis());
                chronometer.start();
            }

            @Override
            public void onStop() {
                chronometer.stop();
            }
        });

        jCameraView.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                CameraActivity.this.finish();
            }
        });


        Log.i("CJT", DeviceUtil.getDeviceModel());
    }

    @Override
    protected void onStart() {
        super.onStart();

        //hiding system bars
        uiHelper.hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        jCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        jCameraView.onPause();
    }
}