package com.water.app.waterconversation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class DangerReceiver extends Activity {

    private String TAG = "DangerReceiver";

    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;
    private TextView textViewTitle,textid;
    private String nowState;
    private Button button_dalarm_OK, button1;
    private WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlobalVariable gv = (GlobalVariable)getApplicationContext();

        //註冊Receiver來改變狀態
        registerReceiver(LReceiver, new IntentFilter(Constants.ACTION.Receive_ACCIDENT));

        //設定初始的警報聲以及喚醒螢幕
        setupMedia_wakeLock();

        //設定畫面
        setContentView(R.layout.danger_receiver);

        //設定畫面的title
        textViewTitle = (TextView) findViewById(R.id.textView_dalarm_title);

        //設定id
        textid = (TextView)findViewById(R.id.dangerID);
        textid.setText("ID:"+gv.getReceiveID());

        //設定button 以及其按鈕事件
        setupButton();

        //根據intent傳遞來的state來決定要顯示什麼文字
        String d = getIntent().getAction();
        setAlarmText(d);

        //設定網頁
        wv = (WebView) findViewById(R.id.webView);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.loadUrl("https://www.google.com");
    }


    private BroadcastReceiver LReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String d =bundle.getString("state");
            Log.d(TAG, "onReceive: textstate :"+ d); //讀取MyFirebase內intent的data

            //如果接收到的==常數
            if(nowState == null) nowState = d;
            if(d.equals(Constants.ACTION.Receive_ACCIDENTS_DROP)){
                setAlarmText(d);
                nowState = d;
                return;
            }
            if(d.equals(Constants.ACTION.Receive_ACCIDENTS_FALL)){
                if(nowState.equals(Constants.ACTION.Receive_ACCIDENTS_DROP)) return;
                setAlarmText(d);
                nowState = d;
                return;
            }
        }
    };

    private void setAlarmText(String text) {
        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        if(text != null){
            switch (text){

                //更新為墜落UI
                case Constants.ACTION.Receive_ACCIDENTS_DROP:
                    textViewTitle.setText(getString(R.string.danger_info)+"墜落");
                    button1.setTextColor(getResources().getColor(R.color.color_danger));
                    button1.setText(R.string.danger_danger);
                    globalVariable.setReceiveAlarming(true);
                    globalVariable.setReceiveAccident(Constants.FCMtype.Drop);
                    globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.DropUnknown);
                    Log.d(TAG, "Drop");
                    break;

                //更新為跌倒UI
                case Constants.ACTION.Receive_ACCIDENTS_FALL:
                    textViewTitle.setText(getString(R.string.danger_info)+"跌倒");
                    button1.setTextColor(getResources().getColor(R.color.color_danger));
                    button1.setText(R.string.danger_danger);
                    globalVariable.setReceiveAlarming(true);
                    globalVariable.setReceiveAccident(Constants.FCMtype.Fall);
                    globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.FallUnknown);
                    Log.d(TAG, "Fall");
                    break;

                //更新為昏迷UI
                case Constants.ACTION.Receive_ACCIDENTS_COMA:
                    textViewTitle.setText(getString(R.string.danger_info)+"昏迷");
                    button1.setTextColor(getResources().getColor(R.color.color_danger));
                    button1.setText(R.string.danger_danger);
                    globalVariable.setReceiveAlarming(true);
                    globalVariable.setReceiveAccident(Constants.FCMtype.Coma);
                    globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.ComaUnknown);
                    Log.d(TAG, "Coma");
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(LReceiver);
        if (mMediaPlayer.isPlaying()) {
            GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
            mMediaPlayer.stop();
            mWakeLock.release();
            globalVariable.setReceiveAlarming(false);
            finish();
        }

    }

    private void setupMedia_wakeLock() {

        mMediaPlayer = MediaPlayer.create(this, R.raw.beeee);
        mMediaPlayer.setLooping(true);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Alarm:Alarm");
        mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
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

    //按鈕
    private void setupButton() {

        button_dalarm_OK = findViewById(R.id.button_dalarm_OK);
        button1 = findViewById(R.id.button_dalarm_1);

        // 安全 點擊事件
        button_dalarm_OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
                if (!globalVariable.getReceiveAlarming()) return;
                switch (globalVariable.getReceiveAccident()){
                    case Constants.FCMtype.Drop:
                        globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.DropSafe);
                        break;
                    case Constants.FCMtype.Fall:
                        globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.FallSafe);
                        break;
                    case Constants.FCMtype.Coma:
                        globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.ComaSafe);
                        break;
                }
                mMediaPlayer.stop();
                mWakeLock.release();
                globalVariable.setReceiveAlarming(false);
                finish();
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();

                if (!globalVariable.getReceiveAlarming()) return;
                switch (globalVariable.getReceiveAccident()) {
                    case Constants.FCMtype.Drop:
                        globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.Drop);
                        break;
                    case Constants.FCMtype.Fall:
                        globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.Fall);
                        break;
                    case Constants.FCMtype.Coma:
                        globalVariable.setReceiveAccidentAnswer(Constants.FCMtype.Coma);
                        break;
                }
                mMediaPlayer.stop();
                mWakeLock.release();
                globalVariable.setReceiveAlarming(false);
                finish();
            }
        });
    }
}