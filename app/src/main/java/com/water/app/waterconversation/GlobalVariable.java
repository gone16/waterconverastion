package com.water.app.waterconversation;


import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class GlobalVariable extends Application {

    private Boolean Open_accident = true;
    private Boolean Open_precursor = true;
    private Boolean Open_location= true;
    private Boolean Detecting = false;
    private Boolean AccidentAlarming = false;
    private Boolean PortentAlarming = false;
    private Boolean Alarming = false;
    private Boolean ok = false;

    private int AlarmAccident = 0;
    private int AlarmPortent = 0;

    private int AlarmAccidentAnswer = 0;
    private int AlarmPortentAnswer = 0;

    public static final String CHANNEL_ID = "exampleServiceChannel";


    /**
     * Setter
     */

    //設定是否開啟意外選項
    public void setOpen_accident(Boolean open_accident){
        this.Open_accident = open_accident;
    }

    //設定是否開啟前兆選項
    public void setOpen_precursor(Boolean open_precursor){
        this.Open_precursor = open_precursor;
    }

    //設定是否開啟位置選項
    public void setOpen_location(Boolean open_location){
        this.Open_location = open_location;
    }

    //更改是否偵測中
    public void setDetecting(Boolean detecting) {
        Detecting = detecting;
    }

    //更改是否按下ok(應該用不到?)
    public void setOK(Boolean OK) {
        ok = OK;
    }

    //設定是否警報中
    public void setAlarming(Boolean alarming) {
        Alarming = alarming;
    }

    //設定是否意外警報中
    public void setAccidentAlarming(Boolean accidentAlarming) {
        AccidentAlarming = accidentAlarming;
    }

    //設定是否前兆警報中
    public void setPortentAlarming(Boolean portentAlarming) {
        PortentAlarming = portentAlarming;
    }

    //設定意外警報種類
    public void setAlarmAccident(int alarmAccident) {
        AlarmAccident = alarmAccident;
    }

    //設定前兆警報的種類
    public void setAlarmPortent(int alarmPortent) {
        AlarmPortent = alarmPortent;
    }

    //設定意外回應的值
    public void setAlarmAccidentAnswer(int alarmAccidentAnswer) {
        AlarmAccidentAnswer = alarmAccidentAnswer;
    }

    //設定前兆回應的值
    public void setAlarmPortentAnswer(int alarmPortentAnswer) {
        AlarmPortentAnswer = alarmPortentAnswer;
    }



    /**
     * Getter
     */

    //得知ChannelId
    public static String getChannelId() {
        return CHANNEL_ID;
    }

    //得知是否偵測中
    public Boolean getDetecting() {
        return Detecting;
    }

    //得知警報後是否按下ok鍵
    public Boolean getOK() {
        return ok;
    }

    //顯示是否開啟意外選項
    public Boolean getOpen_accident(){
        return Open_accident;
    }

    //顯示是否開啟前兆選項
    public Boolean getOpen_precursor(){
        return Open_precursor;
    }

    //顯示是否開啟位置選項
    public Boolean getOpen_location(){
        return Open_location;
    }


    //得知是否警報中
    public Boolean getAlarming() {
        return Alarming;
    }

    //得知意外是否警報中
    public Boolean getAccidentAlarming() {
        return AccidentAlarming;
    }

    //得知前兆是否警報中
    public Boolean getPortentAlarming() {
        return PortentAlarming;
    }

    //設定意外警報種類
    public int getAlarmAccident() {
        return AlarmAccident;
    }

    //得知前兆警報的種類
    public int getAlarmPortent() {
        return AlarmPortent;
    }

    //得知意外回應的值
    public int getAlarmAccidentAnswer() {
        return AlarmAccidentAnswer;
    }

    //得知前兆回應的值
    public int getAlarmPortentAnswer() {
        return AlarmPortentAnswer;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //新增Notification Channel
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        //API大於26版本需要建立Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Example Service",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}