package com.water.app.waterconversation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.water.app.waterconversation.Service.ForeService;

public class AlarmReceiver extends Activity {

    private String TAG = "AlarmReceiver";
    private Integer c;



    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;
    private TextView textViewTitle;
    private String nowState;
    private Button buttonOK, button1,button2;


    private Handler handleTimeout = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //註冊Receiver來改變狀態
        registerReceiver(mMessageReceiver, new IntentFilter(Constants.ACTION.ALARM_BY_BROADCAST));

        //設定初始的警報聲以及喚醒螢幕
        setupMedia_wakeLock();

        //設定畫面
        setContentView(R.layout.receiver_alarm);

        //設定警報的title
        textViewTitle = (TextView)findViewById(R.id.textView_alarm_title);
        textViewTitle.setTextSize(36);

        //設定button
        setupButton();

        //根據intent傳遞來的state來決定要顯示的警報文字
        String textState = getIntent().getAction();
        setAlarmUI(textState);

    }


/**
    private Runnable timesup = new Runnable() {
        @Override
        public void run() {
            GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
            if(globalVariable.getAccidentAlarming()){
                globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.TIMES_UP);
            }else if(globalVariable.getPortentAlarming()){
                globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.TIMES_UP);
            }

            finish();
        }
    };
*/
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
//            String textState = getIntent().getAction();
            Bundle bundle = intent.getExtras();
            String textState =bundle.getString("state");
            Log.d(TAG, "onReceive: nowstate"+ nowState);
            Log.d(TAG, "onReceive: textstate :"+textState);

            if(nowState == null) nowState = textState;
            if(textState.equals(Constants.ACTION.ALARM_ACCIDENTS_DROP)){
                    setAlarmUI(textState);
                    nowState = textState;
                    return;
            }
            if(textState.equals(Constants.ACTION.ALARM_ACCIDENTS_FALL)){
                if(nowState.equals(Constants.ACTION.ALARM_ACCIDENTS_DROP)) return;
                    setAlarmUI(textState);
                    nowState = textState;
                return;
            }
            Log.d(TAG, "onReceive: "+textState);
        }

    };


    /**
     * 更新警報UI
     * @param text title
     */
    private void setAlarmUI(String text){
        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        if(text != null){
            switch (text){

                    //更新為墜落UI
                case Constants.ACTION.ALARM_ACCIDENTS_DROP:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_drop));
                    textViewTitle.setText(R.string.drop);
                    button1.setTextColor(getResources().getColor(R.color.color_fall));
                    button1.setText(R.string.fall);
                    button2.setTextColor(getResources().getColor(R.color.color_coma));
                    button2.setText(R.string.coma);
                    globalVariable.setAccidentAlarming(true);
                    globalVariable.setAlarmAccident(Constants.ACCIDENTS.DROP);
                    globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.DROPUNKNOWN);
                    Log.d(TAG, "Drop");
                    break;

                    //更新為跌倒UI
                case Constants.ACTION.ALARM_ACCIDENTS_FALL:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_fall));
                    textViewTitle.setText(R.string.fall);
                    button1.setTextColor(getResources().getColor(R.color.color_drop));
                    button1.setText(R.string.drop);
                    button2.setTextColor(getResources().getColor(R.color.color_coma));
                    button2.setText(R.string.coma);
                    globalVariable.setAccidentAlarming(true);
                    globalVariable.setAlarmAccident(Constants.ACCIDENTS.FALL);
                    globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.FALLUNKNOWN);
                    Log.d(TAG, "Fall");
                    break;

                    //更新為詢問UI
                case Constants.ACTION.ALARM_ASK:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_coma));
                    textViewTitle.setText(R.string.normal);
                    button1.setTextColor(getResources().getColor(R.color.color_drop));
                    button1.setText(R.string.nodis30);
                    button2.setTextColor(getResources().getColor(R.color.color_fall));
                    button2.setText(R.string.nodis60);
                    globalVariable.setAccidentAlarming(true);
                    globalVariable.setAlarmAccident(Constants.ACCIDENTS.ASK);
                    globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.ASK);
                    Log.d(TAG, "ASK");
                    break;

                    //更新為昏迷UI
                case Constants.ACTION.ALARM_ACCIDENTS_COMA:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_coma));
                    textViewTitle.setText(R.string.coma);
                    button1.setTextColor(getResources().getColor(R.color.color_drop));
                    button1.setText(R.string.drop);
                    button2.setTextColor(getResources().getColor(R.color.color_fall));
                    button2.setText(R.string.fall);
                    globalVariable.setAccidentAlarming(true);
                    globalVariable.setAlarmAccident(Constants.ACCIDENTS.COMA);
                    globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.COMAUNKNOWN);
                    Log.d(TAG, "Coma");
                    break;

                    //更新為失去平衡UI
                case Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_lost_balance));
                    textViewTitle.setText(R.string.lost_balance);
                    button1.setTextColor(getResources().getColor(R.color.color_heavy_step));
                    button1.setText(R.string.heavy_step);
                    button2.setTextColor(getResources().getColor(R.color.color_suddenly_wobbing));
                    button2.setText(R.string.suddenly_wobbing);
                    globalVariable.setPortentAlarming(true);
                    globalVariable.setAlarmPortent(Constants.PORTENTS.LOST_BALANCE);
                    globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.UNKNOWN);
                    Log.d(TAG, "Lost Balance");
                    break;

                    //更新為突然重踩UI
                case Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_heavy_step));
                    textViewTitle.setText(R.string.heavy_step);
                    button1.setTextColor(getResources().getColor(R.color.color_lost_balance));
                    button1.setText(R.string.lost_balance);
                    button2.setTextColor(getResources().getColor(R.color.color_suddenly_wobbing));
                    button2.setText(R.string.suddenly_wobbing);
                    globalVariable.setPortentAlarming(true);
                    globalVariable.setAlarmPortent(Constants.PORTENTS.HEAVY_STEP);
                    globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.UNKNOWN);
                    Log.d(TAG, "Heavy step");
                    break;

                    //更新為突然晃動UI
                case Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING:
                    textViewTitle.setTextColor(getResources().getColor(R.color.color_suddenly_wobbing));
                    textViewTitle.setText(R.string.suddenly_wobbing);
                    button1.setTextColor(getResources().getColor(R.color.color_lost_balance));
                    button1.setText(R.string.lost_balance);
                    button2.setTextColor(getResources().getColor(R.color.color_heavy_step));
                    button2.setText(R.string.heavy_step);
                    globalVariable.setPortentAlarming(true);
                    globalVariable.setAlarmPortent(Constants.PORTENTS.SUDDENLY_WOBBING);
                    globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.UNKNOWN);
                    Log.d(TAG, "suddenly wobbing");
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if(mMediaPlayer.isPlaying()){
//            GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
//            mMediaPlayer.stop();
//            mWakeLock.release();
//            globalVariable.setPortentAlarming(false);
//            globalVariable.setAccidentAlarming(false);
//            globalVariable.setAlarming(false);
//            finish();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMessageReceiver);
        Log.d(TAG, "onDestroy: ");
        if(mMediaPlayer.isPlaying()){
            GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
            mMediaPlayer.stop();
            mWakeLock.release();
            globalVariable.setPortentAlarming(false);
            globalVariable.setAccidentAlarming(false);
            globalVariable.setAlarming(false);
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

    /**
     * 設定按鈕
     */
    private void setupButton(){

        buttonOK = findViewById(R.id.button_alarm_imOK);
        button1 = findViewById(R.id.button_alarm_1);
        button2 = findViewById(R.id.button_alarm_2);

        // 安全 點擊事件
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
                if(!globalVariable.getAlarming()) return;
                globalVariable.setOK(true);
                mMediaPlayer.stop();
                mWakeLock.release();
                globalVariable.setAlarmAccident(Constants.ACCIDENTS.NORMAL);
                globalVariable.setAlarmPortent(Constants.PORTENTS.NORMAL);
                globalVariable.setPortentAlarming(false);
                globalVariable.setAccidentAlarming(false);
                globalVariable.setAlarming(false);
                globalVariable.setPortentAlarming(false);
                finish();
            }
        });

        // 第一按鈕點擊事件
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();

                if(!globalVariable.getAlarming()) return;

                switch (globalVariable.getAlarmAccident()){
                    case Constants.ACCIDENTS.DROP:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.FALL);
                        break;
                    case Constants.ACCIDENTS.FALL:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.DROP);
                        break;
                    case Constants.ACCIDENTS.ASK:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.NORMAL);
                        pauseService();
                        setCount();
                        c = 60;
                        break;
                    case Constants.ACCIDENTS.COMA:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.DROP);
                        break;
                }

                switch (globalVariable.getAlarmPortent()){
                    case Constants.PORTENTS.LOST_BALANCE:
                        globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.HEAVY_STEP);
                        break;
                    case Constants.PORTENTS.HEAVY_STEP:
                        globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.LOST_BALANCE);
                        break;
                    case Constants.PORTENTS.SUDDENLY_WOBBING:
                        globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.LOST_BALANCE);
                        break;
                }

                mMediaPlayer.stop();
                mWakeLock.release();
                globalVariable.setPortentAlarming(false);
                globalVariable.setAccidentAlarming(false);
//                globalVariable.setAlarmAccident(Constants.ACCIDENTS.NORMAL);
//                globalVariable.setAlarmPortent(Constants.PORTENTS.NORMAL);
                globalVariable.setAlarming(false);
                finish();
            }
        });

        //第二按鈕點擊事件
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();

                if(!globalVariable.getAlarming()) return;

                switch (globalVariable.getAlarmAccident()){
                    case Constants.ACCIDENTS.DROP:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.COMA);
                        break;
                    case Constants.ACCIDENTS.FALL:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.COMA);
                        break;
                    case Constants.ACCIDENTS.ASK:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.NORMAL);
                        pauseService();
                        c = 30;
                        setCount();
                        break;
                    case Constants.ACCIDENTS.COMA:
                        globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.FALL);
                        break;
                }

                switch (globalVariable.getAlarmPortent()){
                    case Constants.PORTENTS.LOST_BALANCE:
                        globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.SUDDENLY_WOBBING);
                        break;
                    case Constants.PORTENTS.HEAVY_STEP:
                        globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.SUDDENLY_WOBBING);
                        break;
                    case Constants.PORTENTS.SUDDENLY_WOBBING:
                        globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.HEAVY_STEP);
                        Log.d(TAG, "wobbing" + globalVariable.getAlarmPortentAnswer());
                        break;
                }

                mMediaPlayer.stop();
                mWakeLock.release();
                globalVariable.setPortentAlarming(false);
                globalVariable.setAccidentAlarming(false);
//                globalVariable.setAlarmAccident(Constants.ACCIDENTS.NORMAL);
//                globalVariable.setAlarmPortent(Constants.PORTENTS.NORMAL);
                globalVariable.setAlarming(false);
                finish();
            }
        });
    }

    private void setCount() {
        if (c == 30) {
            CountDownTimer c = new CountDownTimer(3600000, 3000000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    Toast.makeText(getApplicationContext(), "倒數10分", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFinish() {
                    startForeService();
                }
            };
        } else {
            CountDownTimer c = new CountDownTimer(1800000, 1500000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    Toast.makeText(getApplicationContext(), "倒數5分", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFinish() {
                    startForeService();
                }
            };
        }
    }


    private void pauseService() {
        Intent serviceIntent = new Intent(this.getApplicationContext(), ForeService.class);
        serviceIntent.setAction(Constants.ACTION.STOP_FOREGROUND_ACTION);
        getApplicationContext().stopService(serviceIntent);
    }

    private void startForeService() {

        Intent intent = new Intent(this.getApplicationContext(), ForeService.class);
        intent.setAction(Constants.ACTION.START_FOREGROUND_ACTION);
        if (this.getApplicationContext() == null) return;
        ContextCompat.startForegroundService(this.getApplicationContext(), intent);
    }

    public void onBackPressed() {
        return;
    }

    //    @Override
//    public void onCreate(Context context, Intent intent) {
//        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
//        vibrator.vibrate(2000);
//
//        Notification notification = new Notification.Builder(context)
//                .setContentTitle("Alarm is on")
//                .setContentText("You had set up the alarm")
//                .setSmallIcon(R.mipmap.ic_launcher).build();
//
//        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
//        notificationManager.notify(0,notification);
//
//        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
//
//        Ringtone r = RingtoneManager.getRingtone(context,alarm);
//        r.play();
//    }
}
