package com.water.app.waterconversation.Service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.golife.customizeclass.CareMeasureHR;
import com.golife.customizeclass.SetCareSetting;
import com.golife.database.table.TablePulseRecord;
import com.golife.database.table.TableSleepRecord;
import com.golife.database.table.TableSpO2Record;
import com.golife.database.table.TableStepRecord;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.goyourlife.gofitsdk.GoFITSdk;
import com.water.app.waterconversation.Activity.MainActivity;
import com.water.app.waterconversation.AlarmReceiver;
import com.water.app.waterconversation.CSVDataBean;
import com.water.app.waterconversation.Constants;
import com.water.app.waterconversation.DataBase.AppDatabase;
import com.water.app.waterconversation.DataBase.User;
import com.water.app.waterconversation.DataBase.UserDao;
import com.water.app.waterconversation.Fragment.BillboardFragment;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;
import com.water.app.waterconversation.firebase.UploadData;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.water.app.waterconversation.Activity.MainActivity.DeviceId;
import static com.water.app.waterconversation.Activity.MainActivity.OpenAccidentAlarm;
import static com.water.app.waterconversation.Activity.MainActivity.OpenPortentAlarm;
import static com.water.app.waterconversation.Activity.MainActivity._goFITSdk;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityComa;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityDrop;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityFall;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityHeavyStep;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityLostBalance;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivitySuddenlyWobbing;
import static com.water.app.waterconversation.GlobalVariable.CHANNEL_ID;


public class ForeService extends Service implements SensorEventListener , GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener {

    private static final String TAG = "ForeService";


    //宣告物件
    private SensorManager sensorManager; // 感測器 manager
    private Sensor sensor;               // 感測器物件

    private GoogleApiClient googleApiClient;    //利用google api 可以得到位置
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // 更新GPS的毫秒數


    //傳回Activity的數值
    private float x, y, z;              //加速規數值
    private float latitude,longitude,altitude;   //經緯度

    //設定間隔時間
    long LastUpdateTime;
    long LastUploadTime;
    private final int Update_Interval_Time = 100;
    private final int Upload_Interval_Time = 60000;

    //給x,y,z初值
    private float Xval,Yval,Zval = 0.0f;
    private ArrayList<Float> dangerList = new ArrayList<Float>(); //0.5秒內的5個數值陣列
    private  ArrayList<Float> portentList = new ArrayList<Float>(); //5分鐘內數值陣列
    private ArrayList<Float> portentSumList = new ArrayList<>();
    private ArrayList<CSVDataBean> csvDataBeanArrayList = new ArrayList<CSVDataBean>();
    private ArrayList<CSVDataBean> csvDataBeanArrayList2 = new ArrayList<CSVDataBean>();
    private ArrayList<UploadData> firebaseArrayList = new ArrayList<>();
    private ArrayList<UploadData> firebaseArrayList2 = new ArrayList<>();
    private Map<String,UploadData> firebaseMap = new HashMap<>();
    private Map<String,UploadData> firebaseMap2 = new HashMap<>();
    private int csvListChanger =0;
    private int count_coma =0;
    private int count_upload = 0;
    private int count_portent = 0;
    private int count_accident = 0;
    private int count_upload_coma = 0;
    private long last_upload_time;
    private int csvtime = 600*30;
    private int firebaseListChanger =0;
    private final int firebasetime = 600;

    private int drop_count, fall_count = 0;

    private int comatime = 300;

    private DatabaseReference mDatabase;

    private int heartRate = 0;

    private String UserId;
    private String Site;

    // Variable for Golife wrist
    private String mMacAddress = null;
    private String mPairingCode = null;
    private String mPairingTime = null;
    private String mProductID = null;
    SetCareSetting mCareSettings;
//    private String UserId = "";
//    private String DeviceId;
//    private String Site = "";

    //排除第一個加速規的值
    private Boolean isValueInitiate = false;

    // 與Activity溝通的Binder
    private final IBinder mBinder = new LocalBinder();



    public class LocalBinder extends Binder {
        public ForeService getService() {
            return ForeService.this;
        }
    }

    CallBacks callbacks; //callback function(與Activity溝通)
    Handler handler = new Handler(); //負責控制callback 的速度

    //執行Callbacks和handler的執行緒
    Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {
            callbacks.updatePhoneChart(x,latitude,longitude,altitude);
            handler.postDelayed(this, 1000);
        }
    };

    //MainActivity註冊的管道
    public void registerClient(BillboardFragment billboardFragment) {
        this.callbacks = (CallBacks) billboardFragment;

        handler.postDelayed(serviceRunnable, 0);
        Log.d("Register", "Client");
    }

    //定義Activity要更新的資料
    public interface CallBacks {
        void updatePhoneChart(float x,float latitude,float longitude,float altitude);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate: ");
        
        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        UserId = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),"");
        Site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"");
//        DeviceId = android.os.Build.SERIAL;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand: ");
        try {

            //開啟前景通知
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("動作與位置偵測中")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);

            //註冊sensor
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener((SensorEventListener) this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

            //連接google api
            if (googleApiClient != null) {
                googleApiClient.connect();
            }

            // 前景服務運行中，更改狀態為 isDetecting
            GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
            globalVariable.setDetecting(true);

            // 連接Golife手環
            connectGolife();


//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            int drop = sharedPreferences.getInt("drop",0);
//            editor.putInt("drop",drop+1);
//            editor.apply();
//            Log.d(TAG, "drop save "+sharedPreferences.getInt("drop",0));

//            getAllDataEcecutor();
//            new InsertAsyncTask(userDao).execute(user);
//                db.getUserDao().addData(user);
//            userViewModel.insert(user);
//            User user1 = db.getUserDao().findAccidentByState(1);
//            List<User> allUserList = db.getUserDao().getAll();
//            Log.d(TAG, "run: "+allUserList.get(0));
//            Log.d(TAG, "run: "+allUserList.get(1));
//            Log.d(TAG,"run:"+allUserList);
//            Log.d(TAG, "run: "+user1);


        } catch (Exception e) {
            //發生錯誤，更改狀態 isDetecting = false
            GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
            globalVariable.setDetecting(false);
            Toast.makeText(this, "發生錯誤，暫停偵測", Toast.LENGTH_SHORT).show();
            handler.removeCallbacksAndMessages(null);  //移除執行緒
//            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, (LocationListener) this); //停止gps偵測
//            if(googleApiClient != null)  googleApiClient.disconnect(); //與google api斷開
            stopForeground(true); //停止前景服務
            stopSelf(); //移除本身
            Log.e(TAG, "onStartCommand: ", e);
        }

        return START_STICKY;
    }

    private class InsertAsyncTask extends AsyncTask<User,Void,Void>{

        UserDao mUserDao;

        public InsertAsyncTask(UserDao mUserDao){
            this.mUserDao = mUserDao;
        }

        @Override
        protected Void doInBackground(User... users) {
            mUserDao.addData(users[0]);
            Log.i(TAG, "doInBackground: add user"+users[0]);
            return null;
        }
    }

    private class QueryAsyncTask extends AsyncTask<User, Integer, Integer> {

        UserDao mUserDao;

        public QueryAsyncTask(UserDao mUserDao){
            this.mUserDao = mUserDao;
        }

        @Override
        protected Integer doInBackground(User... users) {
            List user = mUserDao.getAll();
//            mUserDao.getAll();
            Log.i(TAG, "doInBackground: all user"+mUserDao.getAll());
            return user.size();
        }
    }

    private void getAllDataEcecutor(){
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 15, 1,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3),
                new ThreadPoolExecutor.DiscardOldestPolicy());
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "User").build();
                    User user = new User(UserId,DeviceId,Site,getCurrentDate(),
                            getCurrentTime(),longitude,latitude,
                            2.35324f,1.32453f,0.342143f,1,1,2,2);
                    AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "User").build();

                    UserDao userDao = appDatabase.getUserDao();
                    userDao.addData(user);
                    Log.i(TAG, "run: "+userDao.getAll().size());
//                    new QueryAsyncTask(userDao).execute(user);
                }
            });

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        if(csvListChanger == 0){
            writeCsv(csvDataBeanArrayList);
        }else {
            writeCsv(csvDataBeanArrayList2);
        }

        try {
            if (firebaseListChanger == 0) {
                for (int i = 0; i < firebaseArrayList.size(); i++) {
                    String user_date_time = firebaseArrayList.get(i).date + "_" + firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                    mDatabase.child(Site).child(user_date_time).setValue(firebaseArrayList.get(i));
//                mDatabase.child("dailyData").child(id).child(time).setValue(user);
                }
            } else {
                for (int i = 0; i < firebaseArrayList2.size(); i++) {
                    String user_date_time = firebaseArrayList2.get(i).date + "_" + firebaseArrayList2.get(i).time + "_" + firebaseArrayList.get(i).id;
                    mDatabase.child(Site).child(user_date_time).setValue(firebaseArrayList2.get(i));
//                mDatabase.child("dailyData").child(id).child(time).setValue(user);
                }
            }
        }catch (Exception e){
            Log.e(TAG, "onDestroy: ",e );
        }

        Log.d(TAG, "onDestroy: destroy handler");
        handler.removeCallbacks(serviceRunnable);
        handler.removeCallbacksAndMessages(null);  //移除執行緒
        sensorManager.unregisterListener(this,sensor);

        if(googleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, (LocationListener) this); //停止gps偵測
            googleApiClient.disconnect(); //與google api斷開
        }
        stopForeground(true); //停止前景服務
        stopSelf(); //移除本身
    }

    private void stopForeService(){
        handler.removeCallbacksAndMessages(null);  //移除執行緒
        if(googleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, (LocationListener) this); //停止gps偵測
            googleApiClient.disconnect(); //與google api斷開
        }
        stopForeground(true); //停止前景服務
        stopSelf(); //移除本身
    }


    /**
     * 實作 當sensor發生變化時觸發
     * @param event 事件
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        if(!globalVariable.getDetecting()) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long CurrentUpdateTime = System.currentTimeMillis();
            long TimeInterval = CurrentUpdateTime - LastUpdateTime;
            if (TimeInterval < Update_Interval_Time) return;
            LastUpdateTime = CurrentUpdateTime;

            //這次的值減掉上次的值，才是加速度
            x = event.values[0] - Xval;
            Xval = event.values[0];
            y = event.values[1] - Yval;
            Yval = event.values[1];
            z = event.values[2] - Zval;
            Zval = event.values[2];

            //第一個值不準確，跳過
            if (!isValueInitiate) {
                isValueInitiate = true;
                return;
            }

            //取得演算法結果
            calculateAlgos(x, y, z);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void calculateAlgos (float x,float y, float z){
        float svmSqaure = (x*x)+(y*y)+(z*z);
        float svmVal = (float) Math.pow(svmSqaure,0.5);
        judgeDanger(svmVal);
//        judgePortents(svmVal);
    }

    /**
     * 判斷危險是否要發出警報(墜落、跌倒、昏迷)
     * @param svm
     */
    private void judgeDanger(float svm){

        GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();

        boolean danger = false;

        //陣列保持五個數值在裡面 (保留0.5秒間的數值)
        if(dangerList.size()<5){
            dangerList.add(svm);
        }else {
            dangerList.remove(0);
            dangerList.add(svm);
        }

        //算出0.5秒間的平均值
        float svmVal_sum = 0.0f;
        if(dangerList.size()<5) return;
        for (int i=0; i<5; i++){
            svmVal_sum = svmVal_sum + dangerList.get(i);
        }
        float svmVal_average = svmVal_sum/5;

        //超過門檻值或小於門檻值之判斷
        if(svmVal_average>22 + sensitivityDrop){
//            if(! globalVariable.getAlarming()){
////                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                int drop = sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_drop),0);
//                editor.putInt(getResources().getString(R.string.sharePreferences_drop),drop+1);
//                editor.apply();
//                Log.d(TAG, "drop save "+sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_drop),0));
//            }
            alarmAccidents(Constants.ACTION.ALARM_ACCIDENTS_DROP); //墜落
            saveData(Constants.ACTION.ALARM_ACCIDENTS_DROP);
            if(drop_count==0){
                saveDataBase(Constants.ACTION.ALARM_ACCIDENTS_DROP,svm);
            }
            drop_count++;
            if (drop_count>5){
                drop_count=0;
            }

            danger = true;

//            saveData(Constants.ACCIDENTS.DROP);
        }else if(svmVal_average>16 + sensitivityFall){
//            if(! globalVariable.getAlarming()){
////                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                int fall = sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_fall),0);
//                editor.putInt(getResources().getString(R.string.sharePreferences_fall),fall+1);
//                editor.apply();
//                Log.d(TAG, "fall save "+sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_fall),0));
//            }
            alarmAccidents(Constants.ACTION.ALARM_ACCIDENTS_FALL); //跌倒
            saveData(Constants.ACTION.ALARM_ACCIDENTS_FALL);
            if(fall_count==0){
                saveDataBase(Constants.ACTION.ALARM_ACCIDENTS_FALL,svm);
            }
            fall_count++;
            if (fall_count>5){
                fall_count=0;
            }
            danger = true;

//            saveData(Constants.ACCIDENTS.FALL);
        }else if(svmVal_average > 10 + ((sensitivityLostBalance)/3) + ((sensitivityHeavyStep)/3)+((sensitivitySuddenlyWobbing)/3)){
            int randomPortent = (int)(Math.random()*3+1);
            switch (randomPortent){
                case Constants.PORTENTS.LOST_BALANCE:
//                    if(! globalVariable.getAlarming()){
////                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        int lostbalance = sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_lost_balance),0);
//                        editor.putInt(getResources().getString(R.string.sharePreferences_lost_balance),lostbalance+1);
//                        editor.apply();
//                        Log.d(TAG, "lostbalance save "+sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_lost_balance),0));
//                    }
                    alarmPortents(Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE); //失去平衡(危險前兆)
                    saveData(Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE);
                    saveDataBase(Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE,svm);
                    break;
                case Constants.PORTENTS.HEAVY_STEP:
//                    if(! globalVariable.getAlarming()){
////                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        int heavytstep = sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_heavy_step),0);
//                        editor.putInt(getResources().getString(R.string.sharePreferences_heavy_step),heavytstep+1);
//                        editor.apply();
//                        Log.d(TAG, "heavytstep save "+sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_heavy_step),0));
//                    }
                    alarmPortents(Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP); //突然重踩(危險前兆)
                    saveData(Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP);
                    saveDataBase(Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP,svm);
                    break;
                case Constants.PORTENTS.SUDDENLY_WOBBING:
//                    if(! globalVariable.getAlarming()){
////                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        int suddenlywobbing = sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_suddenly_wobbing),0);
//                        editor.putInt(getResources().getString(R.string.sharePreferences_suddenly_wobbing),suddenlywobbing+1);
//                        editor.apply();
//                        Log.d(TAG, "suddenlywobbing save "+sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_suddenly_wobbing),0));
//                    }
                    alarmPortents(Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING); //突然晃動(危險前兆)
                    saveData(Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING);
                    saveDataBase(Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING,svm);
                    break;
            }

            danger = true;
        }
        else if(svmVal_average<0.5 + sensitivityComa){
            count_coma++;
//            Log.d(TAG, "sensitivity:"+sensitivityComa);
//            Log.d(TAG, "coma:"+count_coma);
        }else count_coma=0;

        if(count_coma>=comatime){
            count_coma =0;
            alarmAccidents(Constants.ACTION.ALARM_ACCIDENTS_COMA);//昏迷
            saveData(Constants.ACTION.ALARM_ACCIDENTS_COMA);
            saveDataBase(Constants.ACTION.ALARM_ACCIDENTS_COMA,svm);
//            saveData(Constants.ACCIDENTS.COMA);
//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            int coma = sharedPreferences.getInt(getResources().getString(R.string.sharePreferences_coma),0);
//            editor.putInt(getResources().getString(R.string.sharePreferences_coma),coma+1);
//            editor.apply();
            danger = true;
//            count_coma=0;
        }
        if(!danger) saveData("normal");
//        Log.d(TAG, "judgeDanger: coma: "+count_coma);
    }

    //用3000筆資料做比對
    private void judgePortents(float svm){

        if(portentList.size()<3000){
            portentList.add(svm);
        }else {
            portentList.remove(0);
            portentList.add(svm);
        }

        float svmVal_sum = 0.0f;
        if(portentList.size()<3000) return;
        for (int i=0; i<3000; i++){
            svmVal_sum = svmVal_sum + portentList.get(i);
        }

        float svmVal_average = svmVal_sum/3000;

        if(portentSumList.size()<3000){
            portentSumList.add(svmVal_average);
        }else {
            portentSumList.remove(0);
            portentSumList.add(svmVal_average);
        }

        float svmVal_average_average = 0.0f;
        if(portentSumList.size()<3000) return;
        for (int i=0; i<3000; i++){
            svmVal_average_average = svmVal_average_average + portentSumList.get(i);
        }


        if(svmVal_average_average>0.1){
            alarmPortents(Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP);
        }

    }

    private void alarmAccidents(String type){
        if(!OpenAccidentAlarm) return;
        GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
        if(globalVariable.getAlarming()) {
            Intent intent1 = new Intent(Constants.ACTION.ALARM_BY_BROADCAST);
            Bundle bundle = new Bundle();
            bundle.putString("state",type);
            intent1.putExtras(bundle);
            sendBroadcast(intent1);
            return;
        }
        PendingIntent pi;
        AlarmManager am;
//        Log.d(TAG, "alarmAccidents: "+type);
        switch (type){
            case Constants.ACTION.ALARM_ACCIDENTS_DROP:
//                if(isAlarming) break;
                Intent intent1 = new Intent(this, AlarmReceiver.class);
                intent1.setAction(Constants.ACTION.ALARM_ACCIDENTS_DROP);
                pi = PendingIntent.getActivity(this, 0, intent1, 0);
                am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;

            case Constants.ACTION.ALARM_ACCIDENTS_FALL:
//                if(isAlarming) break;
                Intent intent2 = new Intent(this, AlarmReceiver.class);
                intent2.setAction(Constants.ACTION.ALARM_ACCIDENTS_FALL);
                pi = PendingIntent.getActivity(this, 0, intent2, 0);
                am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
            case Constants.ACTION.ALARM_ACCIDENTS_COMA:
//               if(isAlarming) break;
                Intent intent3 = new Intent(this, AlarmReceiver.class);
                intent3.setAction(Constants.ACTION.ALARM_ACCIDENTS_COMA);
                pi = PendingIntent.getActivity(this, 0, intent3,0);
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
        }
    }

    //前兆警報
    private void alarmPortents(String type){
        if(!OpenPortentAlarm) return;

        //如果已經在警報了，就返回
        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        if(globalVariable.getAlarming()) return;

        PendingIntent pi;
        AlarmManager am;

        switch (type){
            case Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE:
                Intent intentLostBalance = new Intent(this, AlarmReceiver.class);
                intentLostBalance.setAction(Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE);
                pi = PendingIntent.getActivity(this, 0, intentLostBalance,0);
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
            case Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP:
                Intent intentHeavyStep = new Intent(this, AlarmReceiver.class);
                intentHeavyStep.setAction(Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP);
                pi = PendingIntent.getActivity(this, 0, intentHeavyStep,0);
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
            case Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING:
                Intent intentSuddenlyWobbing = new Intent(this, AlarmReceiver.class);
                intentSuddenlyWobbing.setAction(Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING);
                pi = PendingIntent.getActivity(this, 0, intentSuddenlyWobbing,0);
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
        }
    }

    //存資料到csv中，根據action決定要存的值
    private void saveData(String action){
        GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
        count_upload++;
//        Log.d(TAG, "count_upload:"+count_upload);

        switch (action){

            //有可能偵測結果為安全，但是其實是User還沒回覆
            case "normal":
                if(count_upload>firebasetime){


                if(globalVariable.getOK()){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }else if(globalVariable.getAlarmPortentAnswer() !=0 || globalVariable.getAlarmAccidentAnswer() !=0){
                    saveDataState(Constants.ACCIDENTS.NORMAL,globalVariable.getAlarmAccidentAnswer(),Constants.PORTENTS.NORMAL,globalVariable.getAlarmPortentAnswer());
                }
                else {
//                    if(globalVariable.getAlarming()){
//                        saveDataState(globalVariable.getAlarmAccident(),globalVariable.getAlarmAccidentAnswer(),globalVariable.getAlarmPortent(),globalVariable.getAlarmPortentAnswer());
//                    }
                    if(count_upload > firebasetime){
                        saveDataState(Constants.ACCIDENTS.NORMAL,globalVariable.getAlarmAccidentAnswer(),Constants.PORTENTS.NORMAL,globalVariable.getAlarmPortentAnswer());
                    }
                }
                }
                break;

            //墜落時存的資料
            case Constants.ACTION.ALARM_ACCIDENTS_DROP:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存墜落資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.ACCIDENTS.UNKNOWN,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //回應跌倒，則存ans為跌倒
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.FALL){
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.ACCIDENTS.FALL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //回應昏迷，則存ans為昏迷
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.COMA){
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.ACCIDENTS.COMA,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //其餘為偵測亦為墜落的資料(可能正在警報的情況下又墜落)
                else {
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.ACCIDENTS.UNKNOWN,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                count_accident++;
                break;

            //跌倒時存的資料
            case Constants.ACTION.ALARM_ACCIDENTS_FALL:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存跌倒資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.ACCIDENTS.UNKNOWN,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //回應墜落，則存ans為墜落
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.DROP){
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.ACCIDENTS.DROP,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //回應昏迷，則存ans為昏迷
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.COMA){
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.ACCIDENTS.COMA,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //其餘為偵測亦為跌倒的資料(可能正在警報的情況下又跌倒)
                else {
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.ACCIDENTS.UNKNOWN,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                count_accident++;
                break;

            //昏迷時存的資料
            case Constants.ACTION.ALARM_ACCIDENTS_COMA:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                    count_coma = 0;
                }
                //正在警報的話，存昏迷資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.UNKNOWN,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //回應墜落，則存ans為墜落
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.DROP){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.DROP,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //回應跌倒，則存ans為跌倒
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.FALL){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.FALL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //其餘為偵測亦為跌倒的資料(可能正在警報的情況下又跌倒)
                else {
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.UNKNOWN,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                count_accident++;
                break;

            //突然失去平衡的資料
            case Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存突然重踩資料
                else if (globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE,Constants.PORTENTS.UNKNOWN);
                }
                //回應失突然重踩，則存ans為突然重踩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.LOST_BALANCE){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE,Constants.PORTENTS.HEAVY_STEP);
                }
                //回應突然不穩，則存ans為突然不穩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.SUDDENLY_WOBBING){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.FALL,Constants.PORTENTS.LOST_BALANCE,Constants.PORTENTS.SUDDENLY_WOBBING);
                }
                //其餘為偵測亦為突然重踩的資料(可能正在警報的情況下又重踩)
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE,Constants.PORTENTS.UNKNOWN);
                }
                count_portent++;
                break;

            //突然重踩存的資料
            case Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存突然重踩資料
                else if (globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP,Constants.PORTENTS.UNKNOWN);
                }
                //回應失去平衡，則存ans為失去平衡
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.LOST_BALANCE){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP,Constants.PORTENTS.LOST_BALANCE);
                }
                //回應突然不穩，則存ans為突然不穩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.SUDDENLY_WOBBING){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.FALL,Constants.PORTENTS.HEAVY_STEP,Constants.PORTENTS.SUDDENLY_WOBBING);
                }
                //其餘為偵測亦為突然重踩的資料(可能正在警報的情況下又重踩)
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP,Constants.PORTENTS.UNKNOWN);
                }
                count_portent++;
                break;
            //突然晃動的資料
            case Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存突然晃動資料
                else if (globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING,Constants.PORTENTS.UNKNOWN);
                }
                //回應失去平衡，則存ans為失去平衡
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.LOST_BALANCE){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING,Constants.PORTENTS.LOST_BALANCE);
                }
                //回應突然重踩，則存ans為突然重踩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.SUDDENLY_WOBBING){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.ACCIDENTS.FALL,Constants.PORTENTS.SUDDENLY_WOBBING,Constants.PORTENTS.HEAVY_STEP);
                }
                //其餘為偵測亦為突然重踩的資料(可能正在警報的情況下又重踩)
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING,Constants.PORTENTS.UNKNOWN);
                }
                count_portent++;
                break;
        }
        if(!globalVariable.getAlarming()){
            globalVariable.setAlarmAccidentAnswer(Constants.ACCIDENTS.NORMAL);
            globalVariable.setAlarmPortentAnswer(Constants.PORTENTS.NORMAL);
        }
        globalVariable.setOK(false);
    }

    //決定關鍵數據(accident, accidentAns, portent, portentAns)
    private void saveDataState(int accident, int accidentAns, int portent, int portentAns){
        if(Site.equals(null) || UserId.equals(null) ) return;
        saveCsv(UserId,DeviceId,getCurrentDate(),getCurrentTime(),latitude,longitude,x,y,z,accident,accidentAns,portent,portentAns,Site,altitude,heartRate);
        saveFireBase(UserId,DeviceId,getCurrentDate(),getCurrentTime(),latitude,longitude,x,y,z,accident,accidentAns,portent,portentAns,Site,altitude,heartRate);
    }

    //將csvList存入.csv檔
    private void saveCsv(String id, String idDevice, String date, String time, float latitude, float longitude, float accX, float accY, float accZ, int accident, int accidentAns, int portent, int portentAns, String site, float altitude,int heartRate){
        CSVDataBean apacheBean = new CSVDataBean();
        apacheBean.setId(id);
        apacheBean.setIdDevice(idDevice);
        apacheBean.setDate(date);
        apacheBean.setTime(time);
        apacheBean.setLatitude(latitude);
        apacheBean.setLongitude(longitude);
        apacheBean.setAccX(accX);
        apacheBean.setAccY(accY);
        apacheBean.setAccZ(accZ);
        apacheBean.setAccident(accident);
        apacheBean.setAccidentAns(accidentAns);
        apacheBean.setPortent(portent);
        apacheBean.setPortentAns(portentAns);
        apacheBean.setSite(site);
        apacheBean.setAltitude(altitude);
        apacheBean.setHeartRate(heartRate);
        if(csvListChanger == 0){
            csvDataBeanArrayList.add(apacheBean);
//            Log.d(TAG, "saveCsvList1:" +csvDataBeanArrayList.size() );
        }
        if(csvListChanger == 1){
            csvDataBeanArrayList2.add(apacheBean);
//            Log.d(TAG, "saveCsvList2:" +csvDataBeanArrayList2.size());
        }

        if(csvDataBeanArrayList.size()>=csvtime){
            Log.d(TAG, "write 1");
            csvListChanger = 1;
            writeCsv(csvDataBeanArrayList);
        }if(csvDataBeanArrayList2.size()>=csvtime){
            Log.d(TAG, "write 2");
            csvListChanger = 0;
            writeCsv(csvDataBeanArrayList2);
        }
    }

    private void writeCsv(ArrayList<CSVDataBean> mList) {
//        Log.d(TAG, "writeCsv: ");
        try {
//            MediaScannerConnection mediaScannerConnection = new MediaScannerConnection(ForeService.this,null);
//            mediaScannerConnection.connect();
//            File file = new File(this.getFilesDir().getAbsolutePath() + File.separator + "new_test_file" + ".csv");
            String filename = getCurrentDate()+"-"+getCurrentTime();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator +filename+ ".csv");
//            mediaScannerConnection.scanFile(ForeService.this, new String[] { filename+".csv" }, null, null);
//            Log.d(TAG, "writeCsv: "+this.getFilesDir().getAbsolutePath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("id", "deviceId", "date", "time", "latitude", "longitude","accX","accY","accZ","accident","accidentAns","portent","portentAns","site","altitude","heartRate"));


            for (int i = 0; i < mList.size(); i++) {
                csvPrinter.printRecord(
                        mList.get(i).getId(),
                        mList.get(i).getIdDevice(),
                        mList.get(i).getDate(),
                        mList.get(i).getTime(),
                        mList.get(i).getLatitude(),
                        mList.get(i).getLongitude(),
                        mList.get(i).getAccX(),
                        mList.get(i).getAccY(),
                        mList.get(i).getAccZ(),
                        mList.get(i).getAccident(),
                        mList.get(i).getAccidentAns(),
                        mList.get(i).getPortent(),
                        mList.get(i).getPortentAns(),
                        mList.get(i).getSite(),
                        mList.get(i).getAltitude());
                        mList.get(i).getHeartRate();
            }
            csvPrinter.printRecord();
            csvPrinter.flush();
//            MediaScannerConnection.scanFile(ForeService.this, new String[] { filename }, null, null);
            if(csvListChanger==0){
                csvDataBeanArrayList2.clear();
            }else csvDataBeanArrayList.clear();
            Log.d(TAG, "writeCsv: success");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFireBase(String id, String idDevice, String date, String time, float latitude, float longitude, float accX, float accY, float accZ, int accident, int accidentAns, int portent, int portentAns, String site, float altitude, int heartRate){
        UploadData uploadData = new UploadData(id,idDevice,date,time,latitude,longitude,accX,accY,accZ,accident,accidentAns,portent,portentAns,site,altitude,heartRate);


        Log.d(TAG, "upload: "+count_upload + ", portent"+count_portent + ", accident" + count_accident );



        boolean accidenting = false;

        if(count_accident>0){
            accidenting = true;
            if(firebaseListChanger ==0){
                firebaseListChanger = 1;
                count_portent =0;
                count_upload = 0;
                count_accident =0;
                firebaseArrayList.add(uploadData);
                for(int i=0; i<firebaseArrayList.size(); i++){
                    if(i==firebaseArrayList.size()-1){
                        if(firebaseArrayList.get(i).portent >0){
                            firebaseArrayList2.add(uploadData);
                            break;
                        }
                    }
                    String user_date_time = firebaseArrayList.get(i).date+"_"+firebaseArrayList.get(i).time+"_"+firebaseArrayList.get(i).id;
                    mDatabase.child(Site).child(user_date_time).setValue(firebaseArrayList.get(i));

                    //                mDatabase.child("dailyData").child(id).child(time).setValue(user);
                }
                Log.d(TAG, "saveFireBase1: "+firebaseArrayList.get(firebaseArrayList.size()-1).accident);
                firebaseArrayList.clear();
                accidenting = false;
            }

            else{
                firebaseListChanger = 0;
                count_portent =0;
                count_upload = 0;
                count_accident = 0;
                firebaseArrayList2.add(uploadData);
                for(int i=0; i<firebaseArrayList2.size(); i++){
                    if(i==firebaseArrayList2.size()-1){
                        if(firebaseArrayList2.get(i).portent >0){
                            firebaseArrayList.add(uploadData);
                            break;
                        }
                    }
                    String user_date_time = firebaseArrayList2.get(i).date+"_"+firebaseArrayList2.get(i).time+"_"+firebaseArrayList2.get(i).id;
                    mDatabase.child(Site).child(user_date_time).setValue(firebaseArrayList2.get(i));
                    //                mDatabase.child("dailyData").child(id).child(time).setValue(user);
                }
                Log.d(TAG, "saveFireBase2: "+firebaseArrayList);
                firebaseArrayList2.clear();
                accidenting = false;
            }
        }

        else if(count_portent>0 && count_accident==0){
            if(firebaseListChanger ==0){
                Log.d(TAG, "save portent");
                firebaseArrayList.add(uploadData);
            }

            else{
                Log.d(TAG, "save portent2");
                firebaseArrayList2.add(uploadData);
            }
        }
//        else {
//            long CurrentUpdateTime = System.currentTimeMillis();
//            long TimeInterval = CurrentUpdateTime - last_upload_time;
//            Log.e(TAG, "saveFireBase  time: " + TimeInterval);
//            if (TimeInterval < firebasetime) return;
//            last_upload_time = CurrentUpdateTime;

        if(count_upload > firebasetime && count_accident==0){
            Log.d(TAG, "saveFireBase!!!" + firebasetime);
            if (firebaseListChanger == 0) {
//                syncGolife();
                Log.d(TAG, "saveFireBase1: " + count_upload);
                if (count_portent == 0) firebaseArrayList.add(uploadData);

                firebaseListChanger = 1;
                count_portent = 0;
                count_upload = 0;
                for (int i = 0; i < firebaseArrayList.size(); i++) {
                    String user_date_time = firebaseArrayList.get(i).date + "_" + firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                    mDatabase.child(Site).child(user_date_time).setValue(firebaseArrayList.get(i));
                    //                mDatabase.child("dailyData").child(id).child(time).setValue(user);
                }
                firebaseArrayList.clear();
            }

            else{
//                syncGolife();
                Log.d(TAG, "saveFireBase2: " + count_upload);
                if (count_portent == 0) firebaseArrayList2.add(uploadData);

                firebaseListChanger = 0;
                count_portent = 0;
                count_upload = 0;

                for (int i = 0; i < firebaseArrayList2.size(); i++) {
                    String user_date_time = firebaseArrayList2.get(i).date + "_" + firebaseArrayList2.get(i).time + "_" + firebaseArrayList2.get(i).id;
                    mDatabase.child(Site).child(user_date_time).setValue(firebaseArrayList2.get(i));
                    //                mDatabase.child("dailyData").child(id).child(time).setValue(user);
                }

                firebaseArrayList2.clear();
            }
        }
//        }



//        if(firebaseListChanger ==0){
//            firebaseMap.put(date+"_"+time,uploadData);
////            firebaseArrayList.add(uploadData);
//        }
//
//        if(firebaseListChanger ==1){
//            firebaseMap2.put(date+"_"+time,uploadData);
////            firebaseArrayList2.add(uploadData);
//        }
//
//        if(firebaseMap.size()>=firebasetime){
//            firebaseListChanger = 1;
//            mDatabase.child("test").child(UserId).setValue(firebaseMap);
//
//            firebaseMap.clear();
//        }if(firebaseMap2.size()>=firebasetime){
//            firebaseListChanger = 0;
//            mDatabase.child("test").child(UserId).setValue(firebaseMap2);
//
//            firebaseMap2.clear();
//        }


//        if(firebaseArrayList.size()>=firebasetime){
//            firebaseListChanger = 1;
//            for(int i=0; i<firebaseArrayList.size(); i++){
//                String date_time = firebaseArrayList.get(i).date+"_"+firebaseArrayList.get(i).time;
//                mDatabase.child("test").child(firebaseArrayList.get(i).id).child(date_time).setValue(firebaseArrayList.get(i));
////                mDatabase.child("dailyData").child(id).child(time).setValue(user);
//            }
//            firebaseArrayList.clear();
//        }if(firebaseArrayList2.size()>=firebasetime){
//            firebaseListChanger = 0;
//            for(int i=0; i<firebaseArrayList2.size(); i++){
//                String date_time = firebaseArrayList2.get(i).date+"_"+firebaseArrayList2.get(i).time;
//                mDatabase.child("test").child(firebaseArrayList2.get(i).id).child(date_time).setValue(firebaseArrayList2.get(i));
////                mDatabase.child("dailyData").child(id).child(time).setValue(user);
//            }
//            firebaseArrayList2.clear();
//        }
    }

    private void saveDataBase(String action,float svm){
        switch(action){
            case Constants.ACTION.ALARM_ACCIDENTS_DROP:
                InsertDataBase(svm,Constants.ACCIDENTS.DROP,Constants.PORTENTS.NORMAL);
                break;
            case Constants.ACTION.ALARM_ACCIDENTS_FALL:
                InsertDataBase(svm,Constants.ACCIDENTS.FALL,Constants.PORTENTS.NORMAL);
                break;
            case Constants.ACTION.ALARM_ACCIDENTS_COMA:
                InsertDataBase(svm,Constants.ACCIDENTS.COMA,Constants.PORTENTS.NORMAL);
                break;
            case Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE:
                InsertDataBase(svm,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE);
                break;
            case Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP:
                InsertDataBase(svm,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP);
                break;
            case Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING:
                InsertDataBase(svm,Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING);
                break;

        }
    }

    private void InsertDataBase(float svm, final int accident, final int portent){
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 15, 1,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "User").build();
                User user = new User(UserId,DeviceId,Site,getCurrentDate(),
                        getCurrentTime(),longitude,latitude,
                        x,y,z,accident,4,portent,4);
                AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "User").build();

                UserDao userDao = appDatabase.getUserDao();
//                List<User> list = userDao.findPortentByState(1);
//                list.size();

                userDao.addData(user);
                Log.i(TAG, "run: "+userDao.getAll().size());
//                    new QueryAsyncTask(userDao).execute(user);
            }
        });
    }

    public String getCurrentTime(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "HH:mm:ss:SSS";
        String[] hour =  dataFormat.split(":");

//        if(hour[0] == "24"){
//            dataFormat = "00"+":"+hour[1]+":"+hour[2]+":"+hour[3];
//        }
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);

        String time = df.format(mCal.getTime());
//        Log.e(TAG, "getCurrentTime: "+ time);

        return time;
    }

    public String getCurrentDate(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "yyyy-MM-dd";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String date = df.format(mCal.getTime());

        return date;
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    /**
     * 當連上google api時觸發
     * @param bundle bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: ");
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.i(TAG, "onConnected: start get gps");
        // Permissions ok, we get last location
        //宣告位置數值
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            Log.d(TAG, "Latitude"+ location.getLatitude()+ "    Longitude"+ location.getLongitude());
            latitude = (float) location.getLatitude();
            longitude = (float) location.getLongitude();
//            locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //開始對位置進行更新
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    //當位置變化時進行更新
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            latitude = (float) location.getLatitude();
            longitude = (float) location.getLongitude();
            altitude = (float) location.getAltitude();

        }
    }

    void connectGolife(){
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_connect");

            // Demo - get connect information from local storage
            if (mMacAddress == null || mPairingCode == null || mPairingTime == null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor pe = sp.edit();
                mMacAddress = sp.getString("macAddress", "");
                mPairingCode = sp.getString("pairCode", "");
                mPairingTime = sp.getString("pairTime", "");
                mProductID = sp.getString("productID", "");
                pe.apply();
            }

            // Demo - doConnectDevice API
            _goFITSdk.doConnectDevice(mMacAddress, mPairingCode, mPairingTime, mProductID, new GoFITSdk.GenericCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "doConnectDevice() : onSuccess()");
                    showToast("Connect complete");

//                    Preference pPref = (Preference) findPreference("demo_connect_status");
                    // Demo - isBLEConnect API
                    boolean isConnect = _goFITSdk.isBLEConnect();
                    String summary = isConnect ? "已連接：" : "未連接：";
                    demoSettingHRTimingMeasure("on,00:00,23:59,1");
                    _goFITSdk.doFindMyCare(3);
                    syncGolife();
                }

                @Override
                public void onFailure(int errorCode, String errorMsg) {
                    Log.e(TAG, "doConnectDevice() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                    showToast("doConnectDevice() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                }
            });
        }
        else {
            showToast("SDK Instance invalid, needs `SDK init`");
        }
    }

    private void syncGolife(){
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_sync");

            // Demo - doSyncFitnessData API
            _goFITSdk.doSyncFitnessData(new GoFITSdk.SyncCallback() {
                @Override
                public void onCompletion() {
                    Log.i(TAG, "doSyncFitnessData() : onCompletion()");
                    showToast("Sync complete!\nDetail fitness data show in `Logcat`");
                }

                @Override
                public void onProgress(String message, int progress) {
//                    Log.i(TAG, "doSyncFitnessData() : onProgress() : message = " + message + ", progress = " + progress);
//                    Preference pPref = (Preference) findPreference("demo_function_sync");
//                    String summary = String.format("%d", progress);
//                    pPref.setSummary(summary);
                }

                @Override
                public void onFailure(int errorCode, String errorMsg) {
                    Log.e(TAG, "doSyncFitnessData() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
//                    showToast("doSyncFitnessData() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                }

                @Override
                public void onGetFitnessData(ArrayList<TableStepRecord> stepRecords, ArrayList<TableSleepRecord> sleepRecords, ArrayList<TablePulseRecord> hrRecords, ArrayList<TableSpO2Record> spo2Records) {
//                    for (TableStepRecord step : stepRecords) {
//                        Log.i(TAG, "doSyncFitnessData() : onGetFitnessData() : step = " + step.toJSONString());
//                    }
//
//                    for (TableSleepRecord sleep : sleepRecords) {
//                        Log.i(TAG, "doSyncFitnessData() : onGetFitnessData() : sleep = " + sleep.toJSONString());
//                    }


                    TablePulseRecord tablePulseRecord= hrRecords.get(hrRecords.size()-1);
                    heartRate = tablePulseRecord.getPulse();
                    Log.d(TAG, "last data:  "+tablePulseRecord.getPulse());

//                    for (TablePulseRecord hr : hrRecords) {
//                        Log.i(TAG, "doSyncFitnessData() : onGetFitnessData() : hr = " + hr.toJSONString());
//                        Log.d(TAG, "HR: "+hr.getPulse()+", time: "+hr.getTimestamp());
//                    }

//                    for (TableSpO2Record spo2 : spo2Records) {
//                        Log.i(TAG, "doSyncFitnessData() : onGetFitnessData() : spo2 = " + spo2.toJSONString());
//                    }
                }
            });
        }
        else {
            showToast("SDK Instance invalid, needs `SDK init`");
        }
    }

    private void disconnectGolife(){
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_disconnect");
            showToast("Device Disconnect");

        }
        else {
            showToast("SDK Instance invalid, needs `SDK init`");
        }
    }

    void demoSettingHRTimingMeasure(String userInput) {
        if (mCareSettings == null) {
            mCareSettings = _goFITSdk.getNewCareSettings();
        }
        String[] separated = userInput.split(",");
        if (separated.length == 4) {
            // Demo - HR timing measure setting
            CareMeasureHR careMeasureHR = mCareSettings.getDefaultMeasureHR();
            careMeasureHR.setRepeatDays(convertRepeatDay(127));
            if (separated[0].equals("on") || separated[0].equals("off")) {
                boolean enable = separated[0].equals("on") ? true : false;
                careMeasureHR.setEnableMeasureHR(enable);
            }
            else {
                showToast("Error Format (invalid input : must be `on` or `off`)");
                return;
            }

            int startMin = convertHHmmToMin(separated[1]);
            if (startMin >= 0 && startMin <= 1439) {
                careMeasureHR.setStartMin((short) startMin);
            }
            else {
                showToast("Error Format (invalid time format)");
            }

            int endMin = convertHHmmToMin(separated[2]);
            if (endMin >= 0 && endMin <= 1439) {
                careMeasureHR.setEndMin((short) endMin);
            }
            else {
                showToast("Error Format (invalid time format)");
            }

            try {
                int intervalMin = Integer.valueOf(separated[3]);
                careMeasureHR.setInterval((short)intervalMin);
            }
            catch (NumberFormatException e) {
                showToast("Error Format (not number format)");
                return;
            }

            mCareSettings.setMeasureHR(careMeasureHR);
            demoSetSettingToDevice();
        }
        else {
            showToast("Error Format (invalid parameter counts)");
        }
    }

    byte[] convertRepeatDay(int days) {
        byte[] repeatDays = {0, 0, 0, 0, 0, 0, 0};
        try {
            repeatDays[0] = (byte) (((days & 0x01) == 1) ? 1 : 0);
            repeatDays[1] = (byte) ((((days >> 1) & 0x01) == 1) ? 1 : 0);
            repeatDays[2] = (byte) ((((days >> 2) & 0x01) == 1) ? 1 : 0);
            repeatDays[3] = (byte) ((((days >> 3) & 0x01) == 1) ? 1 : 0);
            repeatDays[4] = (byte) ((((days >> 4) & 0x01) == 1) ? 1 : 0);
            repeatDays[5] = (byte) ((((days >> 5) & 0x01) == 1) ? 1 : 0);
            repeatDays[6] = (byte) ((((days >> 6) & 0x01) == 1) ? 1 : 0);
        } catch (Exception e) {
            for (int i = 0; i < repeatDays.length; i++) {
                repeatDays[i] = 0;
            }
        }

        return repeatDays;
    }

    int convertHHmmToMin(String HHmm) {
        try {
            String[] timestamp = HHmm.split(":");
            int hour = Integer.parseInt(timestamp[0]);
            int minute = Integer.parseInt(timestamp[1]);
            return (hour * 60 + minute);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    void demoSetSettingToDevice() {
        // Demo - doSetSetting API
        _goFITSdk.doSetSettings(mCareSettings, new GoFITSdk.SettingsCallback() {
            @Override
            public void onCompletion() {
                Log.i(TAG, "doSetSettings() : onCompletion()");
                showToast("Setting OK");
//                Preference pPref = (Preference) findPreference("demo_function_setting");
//                String summary = "Setting OK";
//                pPref.setSummary(summary);
            }

            @Override
            public void onProgress(String message) {
                Log.i(TAG, "doSetSettings() : onProgress() : message = " + message);
            }

            @Override
            public void onFailure(int errorCode, String errorMsg) {
                Log.e(TAG, "doSetSettings() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                showToast("doSetSettings() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
            }
        });

    }

    void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}
