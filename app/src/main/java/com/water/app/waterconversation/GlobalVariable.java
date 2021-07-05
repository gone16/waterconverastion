package com.water.app.waterconversation;


import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.water.app.waterconversation.firebase.MachineData;

public class GlobalVariable extends Application {

    private Boolean Open_accident = true;
    private Boolean Open_precursor = true;
    private Boolean Open_location= true;
    private Boolean Open_accident_alarm = false;
    private Boolean Detecting = false;
    private Boolean AccidentAlarming = false;
    private Boolean PortentAlarming = false;
    private Boolean ReceiveAlarming = false;
    private Boolean Alarming = false;
    private Boolean ok = false;
    private Boolean Daka = false;
    public Boolean isFirebaseSet=false;
    private Boolean isMachineOperating = false;
    private Boolean OperateEnable = false;
    private Boolean Attendance = false;
    private Boolean Bind = false;
    private Boolean Fbind = false;


    private int AlarmAccident = 0;
    private int AlarmPortent = 0;
    private int ReceiveAccident = 0;

    private int AlarmAccidentAnswer = 0;
    private int AlarmPortentAnswer = 0;
    private int ReceiveAccidentAnswer = 0;
    private int ChildID = 0;
    private double Fb_time = 0;

    private String ReceiveID;
    private String Token;
    private String machineName;
    private String machineRadius;
    private String codeMS;
    private String codeMI;
    private String Dakatime;
    private String Dakasit;

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

    //設定是否開啟意外警報
    public void setOpen_accident_alarm(Boolean open_accident_alarm) {this.Open_accident_alarm = open_accident_alarm; }

    //更改是否偵測中
    public void setDetecting(Boolean detecting) {
        Detecting = detecting;
    }

    //更改是否出工
    public void setAttendance(Boolean attendance) {Attendance = attendance;}

    //更改是否打卡
    public void setDaka(Boolean daka) {Daka = daka;}

    //更改是否打卡
    public void setDakatime(String dakatime) {Dakatime = dakatime;}

    //更改是否打卡
    public void setDakasit(String dakasit) {Dakasit = dakasit;}

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

    //得知前兆是否警報中
    public void setReceiveAlarming(Boolean receiveAlarming) { ReceiveAlarming = receiveAlarming; }

    //設定意外警報種類
    public void setAlarmAccident(int alarmAccident) { AlarmAccident = alarmAccident;}

    //設定獲取推播種類
    public void setReceiveAccident(int receiveAccident) { ReceiveAccident = receiveAccident; }

    //設定childID
    public void setChildID(int childID) { ChildID = childID;}

    public void setFb_time(double fb_time) { Fb_time = fb_time;}

    //取得推播id
    public void setReceiveID(String receiveID) {ReceiveID = receiveID;}

    //設定意外回應的值
    public void setReceiveAccidentAnswer(int receiveAccidentAnswer) {
        ReceiveAccidentAnswer = receiveAccidentAnswer;
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
    public void setFirebaseSet(Boolean firebaseSet) { isFirebaseSet = firebaseSet;}

    //
    public void setToken(String token) { Token = token;}

    //機具操作碼
    public void setCodeMs(String codeMs) { codeMS = codeMs;}

    //機具定位碼
    public void setCodeMi(String codeMi) { codeMI = codeMi;}

    //機具是否在操作
    public void setIsMachineOperating(Boolean machineOperating) {
        isMachineOperating = machineOperating;
    }

    //機具能否操作
    public void setOperateEnable(Boolean operateEnable) {
        OperateEnable = operateEnable;
    }

    //機具名字
    public void setMachineName(String machinename) { machineName = machinename;}

    //機具名字
    public void setMachineRadius(String machineradius) { machineRadius = machineradius;}

    //第一次綁定
    public void setFbind(Boolean fbind) { Fbind = fbind;}

    //綁定與否
    public void setBind(Boolean bind) { Bind = bind;}

    /**
     * Getter
     */

    //得知ChannelId
    public static String getChannelId() { return CHANNEL_ID; }

    //得知token
    public String getToken() { return Token;}

    //得知是否已上班
    public Boolean getDaka() {return Daka;}

    //得知是否今日出工
    public Boolean getAttendance() { return Attendance;}

    //得知是否已上班
    public String getDakatime() {return Dakatime;}

    //得知是否已上班
    public String getDakasit() {return Dakasit;}

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

    //是否開啟意外警報
    public Boolean getOpen_accident_alarm() {return  Open_accident_alarm;}


    //得知是否警報中
    public Boolean getAlarming() {
        return Alarming;
    }

    //得知意外是否警報中
    public Boolean getAccidentAlarming() {
        return AccidentAlarming;
    }

    //得知是否操作機具中
    public Boolean getIsMachineOperating() { return isMachineOperating; }

    //得知是否操作機具中
    public Boolean getOperateEnable() {
        return OperateEnable;
    }


    //得知前兆是否警報中
    public Boolean getPortentAlarming() {
        return PortentAlarming;
    }

    //得知是否推播警報中
    public Boolean getReceiveAlarming() {
        return ReceiveAlarming;
    }

    //設定意外警報種類
    public int getAlarmAccident() {
        return AlarmAccident;
    }

    //得知前兆警報的種類
    public int getAlarmPortent() {
        return AlarmPortent;
    }

    //得知推播警報種類
    public int getReceiveAccident(){return ReceiveAccident;}

    //得知推播警報種類
    public int getChildID(){return ChildID;}

    public double getFb_time(){return Fb_time;}

    //得知推播id
    public String getReceiveID(){return ReceiveID;}

    //得知機具名字
    public String getMachineName(){return machineName;}

    //得知機具名字
    public String getMachineRadius(){return machineRadius;}

    //得知意外回應的值
    public int getAlarmAccidentAnswer() {
        return AlarmAccidentAnswer;
    }

    //得知前兆回應的值
    public int getAlarmPortentAnswer() {
        return AlarmPortentAnswer;
    }

    //得知推播警報回應的值
    public int getReceiveAccidentAnswer() {
        return ReceiveAccidentAnswer;
    }

    //得知機具操作碼
    public String getCodeMs() {
        return codeMS;
    }

    //得知機具定位碼
    public String getCodeMi() {
        return codeMI;
    }

    //得知第一次綁定
    public Boolean getFbind() { return Fbind;}

    //得知綁定與否
    public Boolean getBind() { return Bind;}


    public Boolean getFirebaseSet() { return isFirebaseSet; }
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