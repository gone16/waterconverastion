package com.water.app.waterconversation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class PortentAlarmReceiver extends Activity {

    private String TAG = "PortentAlarmReceiver";


    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;
    private TextView textViewTitle;
    private String nowState;
    private Button buttonOK, buttonPortent1, buttonPortent2, buttonPortent3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //註冊Receiver來改變狀態
//        registerReceiver(mMessageReceiver, new IntentFilter(Constants.ACTION.ALARM_BY_BROADCAST));

        //設定初始的警報聲以及喚醒螢幕
        setupMedia_wakeLock();

        //設定畫面
        setContentView(R.layout.receiver_portent_alarm);

        //設定前兆畫面的title
        textViewTitle = (TextView) findViewById(R.id.textView_alarm_portent_title);
        textViewTitle.setTextSize(30);

        //根據intent傳遞來的state來決定要顯示什麼文字
        String textState = getIntent().getAction();
        setAlarmText(textState);

        //設定button 以及其按鈕事件
        setupButton();

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
//            String textState = getIntent().getAction();
            Bundle bundle = intent.getExtras();
            String textState = bundle.getString("state");
            Log.d(TAG, "onReceive: nowstate" + nowState);
            Log.d(TAG, "onReceive: textstate :" + textState);

            if (nowState == null) nowState = textState;
            if (textState.equals(Constants.ACTION.ALARM_ACCIDENTS_DROP)) {
                setAlarmText(textState);
                nowState = textState;
                return;
            }
            if (textState.equals(Constants.ACTION.ALARM_ACCIDENTS_FALL)) {
                if (nowState.equals(Constants.ACTION.ALARM_ACCIDENTS_DROP)) return;
                setAlarmText(textState);
                nowState = textState;
                return;
            }


            Log.d(TAG, "onReceive: " + textState);
        }

    };

    private void setAlarmText(String text) {

        if (text != null) {
            if (text.equals(Constants.ACTION.ALARM_ACCIDENTS_DROP)) {
                textViewTitle.setTextColor(Color.RED);
                textViewTitle.setText("墜落");
                Log.d(TAG, "Drop:");
            } else if (text.equals(Constants.ACTION.ALARM_ACCIDENTS_FALL)) {
                textViewTitle.setTextColor(Color.RED);
                textViewTitle.setText("跌倒");
                Log.d(TAG, "Fall:");
            } else if (text.equals(Constants.ACTION.ALARM_ACCIDENTS_COMA)) {
                textViewTitle.setTextColor(Color.RED);
                textViewTitle.setText("昏迷");
                Log.d(TAG, "Coma:");
            } else if (text.equals(Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP)) {
                textViewTitle.setTextColor(Color.MAGENTA);
                textViewTitle.setText("危險前兆(不穩或失衡、突然重踩、突然晃動)");
                Log.d(TAG, "Portents:");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer.isPlaying()) {
            GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
            mMediaPlayer.stop();
            mWakeLock.release();
            globalVariable.setPortentAlarming(false);
            finish();
        }

    }

    private void setupMedia_wakeLock(){

        mMediaPlayer = MediaPlayer.create(this, R.raw.beeee);
        mMediaPlayer.setLooping(true);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Alarm:Alarm");
        mWakeLock.acquire(10*60*1000L /*10 minutes*/);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        // soundPool.play(AlarmSound,1,1,0,5,1);
        mMediaPlayer.setVolume(1.0f, 1.0f);
        mMediaPlayer.start();
    }

    private void setupButton(){

        buttonOK = findViewById(R.id.button_alarm_portent_imOK);
        buttonPortent1 = findViewById(R.id.button_alarm_portent_1);
        buttonPortent2 = findViewById(R.id.button_alarm_portent_2);
        buttonPortent3 = findViewById(R.id.button_alarm_portent_3);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
                globalVariable.setOK(true);
                mMediaPlayer.stop();
                mWakeLock.release();
                globalVariable.setPortentAlarming(false);
                finish();
            }
        });

        buttonPortent1.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        }));

        buttonPortent2.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        }));

        buttonPortent3.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        }));
    }
}