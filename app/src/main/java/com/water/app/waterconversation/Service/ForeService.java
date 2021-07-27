package com.water.app.waterconversation.Service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.TensorFlowLite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
import static com.water.app.waterconversation.Activity.MainActivity.OpenPortentAlarm;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityComa;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityDrop;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityFall;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityPortent;
import static com.water.app.waterconversation.GlobalVariable.CHANNEL_ID;


public class ForeService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener {

    private static final String TAG = "ForeService";


    //宣告物件
    private SensorManager sensorManager; // 感測器 manager
    private Sensor sensor, sensorRotate, sensorgyo,sensorro; // 感測器物件

    private GoogleApiClient googleApiClient;    //利用google api 可以得到位置
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // 更新GPS的毫秒數


    //傳回Activity的數值
    private float x, y, z;              //加速規數值
    private float latitude,longitude,altitude;   //經緯度

    //設定間隔時間
    long LastUpdateTime;
    long LastUploadTime;
    private final int Update_Interval_Time = 100;
    private final int Upload_Interval_Time = 30000;

    //門檻值初始值
    private final float Threshold_Drop = 24;
    private final float Threshold_Fall = 19;
    private final float Threshold_Coma = 0.1f;
    private final float Threshold_Lost_Balance = 12;

    //給x,y,z初值
    private float Xval,Yval,Zval,Pval,Rval,yv,zv,SVMo,SVM,svmi= 0.0f;
    private double mRoll, mPitch = 0.0;
    private int count;
    private ArrayList<Float> dangerList = new ArrayList<Float>(); //0.5秒內的5個數值陣列
    private ArrayList<Float> svmlist = new ArrayList<Float>(); //0.5秒內的5個數值陣列
    private ArrayList<Float> ylist = new ArrayList<Float>(); //0.5秒內的5個數值陣列
    private ArrayList<Float> zlist = new ArrayList<Float>(); //0.5秒內的5個數值陣列
    private ArrayList<Integer> clist = new ArrayList<Integer>(); //0.5秒內的5個數值陣列
    private List<Float> plist = new ArrayList<Float>(); //0.5秒內的5個數值陣列
    private ArrayList<Float> portentList = new ArrayList<Float>(); //5分鐘內數值陣列
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
    private int count_receive = 0;
    private int count_mstart = 0;
    private int csvtime = 300*60;
    private int arraysize = 20; //y,z,svm的矩陣常數
    private int firebaseListChanger =0;
    private final double firebasetime = 0.1;

    private int drop_count = 0;
    private int fall_count = 0;

    private int comatime = 1000;

    private DatabaseReference mDatabase;

    private String UserId;
    private String Site;

    private Interpreter tflite;


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

        try{
            tflite = new Interpreter(loadModelFile());
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction().equals("buttonclick")){
            count_mstart = 1;
        }else {
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
                sensorManager.registerListener((SensorEventListener) this, sensor, 100);
                sensorRotate = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                sensorManager.registerListener((SensorEventListener) this, sensorRotate, 100);

                //連接google api
                if (googleApiClient != null) {
                    googleApiClient.connect();
                }

                // 前景服務運行中，更改狀態為 isDetecting
                GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
                globalVariable.setDetecting(true);

            } catch (Exception e) {
                //發生錯誤，更改狀態 isDetecting = false
                GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
                globalVariable.setDetecting(false);
                Toast.makeText(this, "發生錯誤，暫停偵測", Toast.LENGTH_SHORT).show();
                handler.removeCallbacksAndMessages(null);  //移除執行緒
                stopForeground(true); //停止前景服務
                stopSelf(); //移除本身
                Log.e(TAG, "onStartCommand: ", e);
            }
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
                    String user_time = firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                    mDatabase.child(Site+"_MEMBER").child(firebaseArrayList.get(i).date).child(user_time).setValue(firebaseArrayList.get(i));
                }
            } else {
                for (int i = 0; i < firebaseArrayList2.size(); i++) {
                    String user_time = firebaseArrayList2.get(i).time + "_" + firebaseArrayList2.get(i).id;
                    mDatabase.child(Site+"_MEMBER").child(firebaseArrayList2.get(i).date).child(user_time).setValue(firebaseArrayList2.get(i));
            }
            }
        }catch (Exception e){
            Log.e(TAG, "onDestroy: ",e );
        }

        Log.d(TAG, "onDestroy: destroy handler");
        handler.removeCallbacks(serviceRunnable);
        handler.removeCallbacksAndMessages(null);  //移除執行緒
        sensorManager.unregisterListener(this,sensor);
        sensorManager.unregisterListener(this,sensorRotate);

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

    public static float[] mAccelerometer = null;
    public static float[] mGeomagnetic = null;
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
            Xval = event.values[0];
            Yval = event.values[1];
            yv = Yval;
            Zval = event.values[2];
            zv = Zval;
            //第一個值不準確，跳過
            if (!isValueInitiate) {
                isValueInitiate = true;
                return;
            }
            mAccelerometer = event.values;
            //取得演算法結果
            SVM =(float) Math.pow(Xval*Xval+Yval*Yval+Zval*Zval,0.5);
            calculateAlgos(Xval, Yval, Zval);
            calculateSVM(Xval, Yval, Zval);

        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){

            Xval = event.values[0];
            Yval = event.values[1];
            Zval = event.values[2];
            mGeomagnetic = event.values;
        }

        if (mAccelerometer != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);

            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // at this point, orientation contains the azimuth(direction), pitch and roll values.
                double azimuth = 180 * orientation[0] / Math.PI;
                double pitch = 180 * orientation[1] / Math.PI;
                double roll = 180 * orientation[2] / Math.PI;
                mPitch = pitch;
                mRoll = roll;
                Pval = orientation[1];
                Rval = orientation[2];
                SVMo = (float) Math.pow(Pval*Pval+Rval*Rval,0.5);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //變更為計算兩筆SVM差值
    public void calculateAlgos (float x,float y, float z){
        float svmSqaure = (x*x)+(y*y)+(z*z);
        float svmVal = (float) Math.pow(svmSqaure,0.5); //計算該秒SVM
        float svmd = (float) Math.abs(svmVal-svmi);//計算與前一筆SVM差值
        svmi = svmVal;//變為前一筆SVM
        judgeDanger(svmd);
    }
    public void calculateSVM (float x,float y, float z){
        float svmSqaure = (x*x)+(y*y)+(z*z);
        SVM =(float) Math.pow(svmSqaure,0.5);
        tense(SVM);
    }

    //建立丟入模型的shape
    private void tense(float SVM) {
        //陣列保持10個數值在裡面 (保留1秒間的數值)
        if (ylist.size() < arraysize) { //arraysize改為常數，如果之後要更改從上面改全部的就好
            ylist.add(yv);
        } else {
            ylist.remove(0);
            ylist.add(yv);
        }
        if (zlist.size() < arraysize) {
            zlist.add(zv);
        } else {
            zlist.remove(0);
            zlist.add(zv);
        }
        if (svmlist.size() < arraysize) {
            svmlist.add(SVM);
        } else {
            svmlist.remove(0);
            svmlist.add(SVM);
        }
        //添加y,z,svm數值進list
        if (ylist.size() == arraysize && zlist.size() == arraysize && svmlist.size() == arraysize) {
            plist.addAll(ylist);
            plist.addAll(zlist);
            plist.addAll(svmlist);
            Integer prediction =(int) doInference(plist);//進行判斷
            plist.clear();
            judgeD(prediction);//階層門檻
        } else {
            return;
        }
    }

    //進行判斷
    private float doInference(List<Float> plist) {

        float[][][] inputval = new float[1][3][20]; //Inputshape
        float[][] outputval = new float[1][3]; //outputsape

        //將前面陣列變更為符合inputshape之形狀
        int m = 0;
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 20; k++) {
                    float ar = plist.get(m++);
                    inputval[i][j][k] = ar;
                }
            }
        }
        tflite.run(inputval, outputval); //進行分析

        //輸出機率
        float iii = outputval[0][0]; //安全機率
        float jjj = outputval[0][1]; //墜落
        float kkk = outputval[0][2]; //跌倒
        Integer ans = 0;

        //墜落可能性大於跌倒與安全
        if(jjj > iii && jjj > kkk){
            ans = 1;
        //跌倒可能性大於墜落與安全
        }if(kkk > iii && kkk > jjj ){
            ans = 2;
        }else{;
        }
        Log.d(TAG,"ans="+ans);
        return ans;
    }

    //讀取導入模型
    private MappedByteBuffer loadModelFile() throws IOException{
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("cmodel(1).tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }


    private void judgeD(Integer prediction) {
        GlobalVariable gv = (GlobalVariable) getApplicationContext();
        //p=1 count+
        if(prediction == 1){
            drop_count=drop_count+1;
        }
        //p=2 count+
        if(prediction == 2 ){
            fall_count=fall_count+1;
        }
        //p=0 如果count=0不再扣，不等於０扣
        if(prediction == 0) {
            if (fall_count < 1) {
                fall_count = 0;
            }
            if (drop_count < 1) {
                drop_count = 0;
            } else {
                drop_count = drop_count-1;
                fall_count = fall_count-2; //扣2誤判比較少
            }
        }

        if (drop_count > 6) {
            alarmAccidents(Constants.ACTION.ALARM_ACCIDENTS_DROP); //墜落
            saveData(Constants.ACTION.ALARM_ACCIDENTS_DROP);
        }
        if (fall_count > 5) {
            alarmAccidents(Constants.ACTION.ALARM_ACCIDENTS_FALL); //跌倒
            saveData(Constants.ACTION.ALARM_ACCIDENTS_FALL);
        } else {
            gv.setOK(true);//後面儲存才會是正常
            saveData("normal");
        }
    }
    //判斷昏迷
    private void judgeDanger(float svmd) {

        //陣列保持10個數值在裡面 (保留1秒間的數值)
        if (dangerList.size() < 10) {
            dangerList.add(svmd);
        } else {
            dangerList.remove(0);
            dangerList.add(svmd);
        }
        //算出0.5秒間的平均值
        float svmVal_sum = 0.0f;
        if (dangerList.size() < 10) return;
        for (int i = 0; i < 10; i++) {
            svmVal_sum = svmVal_sum + dangerList.get(i);
        }
        float svmVal_average = svmVal_sum / 10;

        if (svmVal_average < Threshold_Coma + sensitivityComa) {
            count_coma++;
        } else count_coma = 0;
        count=count_coma;
        Log.d(TAG,"coma"+Threshold_Coma + sensitivityComa);
        if (count_coma >= comatime) {
            alarmAccidents(Constants.ACTION.ALARM_ASK);//昏迷
            count_coma++;
        }
        if (count_coma >= comatime+300) {
            count_coma = 0;
            alarmAccidents(Constants.ACTION.ALARM_ACCIDENTS_COMA);//昏迷
            saveData(Constants.ACTION.ALARM_ACCIDENTS_COMA);
            saveDataBase(Constants.ACTION.ALARM_ACCIDENTS_COMA, svmd);
        }
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
        GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
        if(!globalVariable.getOpen_accident_alarm()) return;
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
        switch (type){
            case Constants.ACTION.ALARM_ACCIDENTS_DROP:
                Intent intent1 = new Intent(this, AlarmReceiver.class);
                intent1.setAction(Constants.ACTION.ALARM_ACCIDENTS_DROP);
                pi = PendingIntent.getActivity(this, 0, intent1, 0);
                am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;

            case Constants.ACTION.ALARM_ACCIDENTS_FALL:
                Intent intent2 = new Intent(this, AlarmReceiver.class);
                intent2.setAction(Constants.ACTION.ALARM_ACCIDENTS_FALL);
                pi = PendingIntent.getActivity(this, 0, intent2, 0);
                am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
            case Constants.ACTION.ALARM_ASK:
                Intent intent3 = new Intent(this, AlarmReceiver.class);
                intent3.setAction(Constants.ACTION.ALARM_ASK);
                pi = PendingIntent.getActivity(this, 0, intent3,0);
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+100, pi);
                globalVariable.setAlarming(true);
                break;
            case Constants.ACTION.ALARM_ACCIDENTS_COMA:
                Intent intent4 = new Intent(this, AlarmReceiver.class);
                intent4.setAction(Constants.ACTION.ALARM_ACCIDENTS_COMA);
                pi = PendingIntent.getActivity(this, 0, intent4,0);
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
        if (globalVariable.getReceiveAccidentAnswer() != 0) {
            count_receive ++;
            saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
        }
        if(count_mstart ==1){
            count_mstart ++ ;
            saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
        }

        switch (action){

            //有可能偵測結果為安全，但是其實是User還沒回覆
            case "normal":
                if(count_upload>firebasetime){

                if(globalVariable.getOK()){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }else if(globalVariable.getAlarmPortentAnswer() !=0 || globalVariable.getAlarmAccidentAnswer() !=0){
                    saveDataState(globalVariable.getAlarmAccidentAnswer(),globalVariable.getAlarmPortentAnswer());
                }
                else {
                    if(count_upload > firebasetime){
                        saveDataState(globalVariable.getAlarmAccidentAnswer(),globalVariable.getAlarmPortentAnswer());
                    }
                }
                }
                break;

            //墜落時存的資料
            case Constants.ACTION.ALARM_ACCIDENTS_DROP:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()){
                    saveDataState(Constants.ACCIDENTS.DROPSAFE,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存墜落資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.DROPUNKNOWN,Constants.PORTENTS.NORMAL);
                }
                //回應跌倒，則存ans為跌倒
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.FALL){
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.PORTENTS.NORMAL);
                }
                //回應昏迷，則存ans為昏迷
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.COMA){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.PORTENTS.NORMAL);
                }
                //其餘為偵測亦為墜落的資料(可能正在警報的情況下又墜落)
                else {
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.PORTENTS.NORMAL);
                }
                count_accident++;
                drop_count=0;
                break;

            //跌倒時存的資料
            case Constants.ACTION.ALARM_ACCIDENTS_FALL:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.FALLSAFE,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存跌倒資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.FALLUNKNOWN,Constants.PORTENTS.NORMAL);
                }
                //回應墜落，則存ans為墜落
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.DROP){
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.PORTENTS.NORMAL);
                }
                //回應昏迷，則存ans為昏迷
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.COMA){
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.PORTENTS.NORMAL);
                }
                //其餘為偵測亦為跌倒的資料(可能正在警報的情況下又跌倒)
                else {
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.PORTENTS.NORMAL);
                }
                count_accident++;
                fall_count=0;
                break;

            //昏迷時存的資料
            case Constants.ACTION.ALARM_ACCIDENTS_COMA:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.COMASAFE,Constants.PORTENTS.NORMAL);
                    count_coma = 0;
                }
                //正在警報的話，存昏迷資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.COMAUNKNOWN,Constants.PORTENTS.NORMAL);
                }
                //回應墜落，則存ans為墜落
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.DROP){
                    saveDataState(Constants.ACCIDENTS.DROP,Constants.PORTENTS.NORMAL);
                }
                //回應跌倒，則存ans為跌倒
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.FALL){
                    saveDataState(Constants.ACCIDENTS.FALL,Constants.PORTENTS.NORMAL);
                }
                //其餘為偵測亦為跌倒的資料(可能正在警報的情況下跌倒之類)
                else {
                    saveDataState(Constants.ACCIDENTS.COMA,Constants.PORTENTS.NORMAL);
                }
                count_accident++;
                break;

            //昏迷時存的資料
            case Constants.ACTION.ALARM_ASK:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存正常資料
                else if(globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                    count_coma = 0;
                }
                //回應休息，則存ans為休息
                else if(globalVariable.getAlarmAccidentAnswer() == Constants.ACCIDENTS.BREAK){
                    saveDataState(Constants.ACCIDENTS.BREAK,Constants.PORTENTS.NORMAL);
                    count_coma = 0;
                }
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                    count_coma = 0;
                }
                break;

            //突然失去平衡的資料
            case Constants.ACTION.ALARM_PORTENTS_LOST_BALALNCE:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存失去平衡資料
                else if (globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE);
                }
                //回應失突然重踩，則存ans為突然重踩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.LOST_BALANCE){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP);
                }
                //回應突然不穩，則存ans為突然不穩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.SUDDENLY_WOBBING){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING);
                }
                //其餘為偵測亦為失去平衡的資料(可能正在警報的情況下又失去平衡)
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE);
                }
                count_portent++;
                break;

            //突然重踩存的資料
            case Constants.ACTION.ALARM_PORTENTS_HEAVY_STEP:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存突然重踩資料
                else if (globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP);
                }
                //回應失去平衡，則存ans為失去平衡
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.LOST_BALANCE){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE);
                }
                //回應突然不穩，則存ans為突然不穩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.SUDDENLY_WOBBING){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING);
                }
                //其餘為偵測亦為突然重踩的資料(可能正在警報的情況下又重踩)
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP);
                }
                count_portent++;
                break;
            //突然晃動的資料
            case Constants.ACTION.ALARM_PORTENTS_SUDDENLY_WOBBING:

                //回應"沒事"時，存全部正常的資料
                if(globalVariable.getOK()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.NORMAL);
                }
                //正在警報的話，存突然晃動資料
                else if (globalVariable.getAlarming()) {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING);
                }
                //回應失去平衡，則存ans為失去平衡
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.LOST_BALANCE){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.LOST_BALANCE);
                }
                //回應突然重踩，則存ans為突然重踩
                else if(globalVariable.getAlarmPortentAnswer() == Constants.PORTENTS.HEAVY_STEP){
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.HEAVY_STEP);
                }
                //其餘為偵測亦為突然晃動的資料(可能正在警報的情況下又晃)
                else {
                    saveDataState(Constants.ACCIDENTS.NORMAL,Constants.PORTENTS.SUDDENLY_WOBBING);
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
    private void saveDataState(int accident, int portent) {
        if (Site.equals(null) || UserId.equals(null)) return;
        saveCsv(getCurrentDate(), getCurrentTime(), Xval, Yval, Zval,SVM,SVMo,count,accident);
        //saveFireBase(UserId, DeviceId, getCurrentDate(), getCurrentTime(), latitude, longitude, x, y, z, accident,portent, Site, altitude,heartRate, gettoken(), getMachine(), getMachineName(), getReceiveAns());
    }
     //將csvList存入.csv檔
     private void saveCsv(String date, String time, float accX, float accY, float accZ,float SVM,float SVMo, int count, int accident){
        CSVDataBean apacheBean = new CSVDataBean();
        apacheBean.setDate(date);
        apacheBean.setTime(time);
        apacheBean.setAccX(accX);
        apacheBean.setAccY(accY);
        apacheBean.setAccZ(accZ);
        apacheBean.setSVM(SVM);
        apacheBean.setSVMo(SVMo);
        apacheBean.setCount(count);
        apacheBean.setAccident(accident);
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
//       Log.d(TAG, "writeCsv: ");
         try {
             String filename = getCurrentDate()+"-"+getCurrentTime();
             File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator +filename+ ".csv");
//           mediaScannerConnection.scanFile(ForeService.this, new String[] { filename+".csv" }, null, null);
//           Log.d(TAG, "writeCsv: "+this.getFilesDir().getAbsolutePath());
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("date", "time","accX","accY","accZ","SVM","SVMo","count","accident"));


             for (int i = 0; i < mList.size(); i++) {
                 csvPrinter.printRecord(
                         //mList.get(i).getId(),
                         //mList.get(i).getIdDevice(),
                         mList.get(i).getDate(),
                         mList.get(i).getTime(),
                         mList.get(i).getAccX(),
                         mList.get(i).getAccY(),
                         mList.get(i).getAccZ(),
                         mList.get(i).getSVM(),
                         mList.get(i).getSVMo(),
                         mList.get(i).getCount(),
                         mList.get(i).getAccident());
                }
                csvPrinter.printRecord();
                csvPrinter.flush();
                if(csvListChanger==0){
                    csvDataBeanArrayList2.clear();
                }else csvDataBeanArrayList.clear();
                Log.d(TAG, "writeCsv: success");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

     private void saveFireBase(String id, String idDevice, String date, String time, float latitude, float longitude, float accX, float accY, float accZ, int accident, int portent,
                               String site, float altitude, String token,Boolean machine,String machineName,String receiveAns) {
         UploadData uploadData = new UploadData(id, idDevice, date, time, latitude, longitude, accX, accY, accZ, accident, portent, site, altitude, token, machine,machineName, receiveAns);
         Log.d(TAG, "upload: " + count_upload + ", portent" + count_portent + ", accident" + count_accident+",receive"+count_receive+",start"+count_mstart);
         GlobalVariable gv = (GlobalVariable) getApplicationContext();

         boolean accidenting = false;
         if (count_accident > 0) {
             accidenting = false;
             if (firebaseListChanger == 0) {
                 firebaseListChanger = 1;
                 count_portent = 0;
                 count_upload = 0;
                 count_accident = 0;
                 count_receive = 0;
                 count_mstart = 0;
                 firebaseArrayList.add(uploadData);
                 for (int i = 0; i < firebaseArrayList.size(); i++) {
                     if (i == firebaseArrayList.size() - 1) {
                         if (firebaseArrayList.get(i).portent > 0) {
                             firebaseArrayList2.add(uploadData);
                             break;
                         }
                     }
                     String user_time = firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList.get(i).date).child(user_time).setValue(firebaseArrayList.get(i));
                 }
                 Log.d(TAG, "saveFireBase1: " + firebaseArrayList.get(firebaseArrayList.size() - 1).accident);
                 firebaseArrayList.clear();
                 accidenting = false;
             } else {
                 firebaseListChanger = 0;
                 count_portent = 0;
                 count_upload = 0;
                 count_accident = 0;
                 count_mstart = 0;
                 firebaseArrayList2.add(uploadData);
                 for (int i = 0; i < firebaseArrayList2.size(); i++) {
                     if (i == firebaseArrayList2.size() - 1) {
                         if (firebaseArrayList2.get(i).portent > 0) {
                             firebaseArrayList.add(uploadData);
                             break;
                         }
                     }
                     String user_time =firebaseArrayList2.get(i).time + "_" + firebaseArrayList2.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList2.get(i).date).child(user_time).setValue(firebaseArrayList2.get(i));
                 }
                 Log.d(TAG, "saveFireBase2: " + firebaseArrayList);
                 firebaseArrayList2.clear();
                 accidenting = false;
             }
         } else if (count_portent > 0 && count_accident == 0) {
             if (firebaseListChanger == 0) {
                 firebaseArrayList.add(uploadData);
             } else {
                 firebaseArrayList2.add(uploadData);
             }
         }

         //收到推播後
         if (count_receive > 0) {
             if (firebaseListChanger == 0) {
                 firebaseArrayList.add(uploadData);
                 firebaseListChanger = 1;
                 count_portent = 0;
                 count_upload = 0;
                 count_receive = 0;
                 count_mstart = 0;
                 for (int i = 0; i < firebaseArrayList.size(); i++) {
                     String user_time = firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList.get(i).date).child(user_time).setValue(firebaseArrayList.get(i));
                 }
                 gv.setReceiveAccidentAnswer(Constants.FCMtype.Normal);
                 firebaseArrayList.clear();
             } else {
                 firebaseArrayList2.add(uploadData);
                 firebaseListChanger = 0;
                 count_portent = 0;
                 count_upload = 0;
                 count_receive = 0;
                 count_mstart = 0;
                 for (int i = 0; i < firebaseArrayList2.size(); i++) {
                     String user_time = firebaseArrayList2.get(i).time + "_" + firebaseArrayList2.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList2.get(i).date).child(user_time).setValue(firebaseArrayList2.get(i));
                     }
                 gv.setReceiveAccidentAnswer(Constants.FCMtype.Normal);
                 firebaseArrayList2.clear();
             }
         }

         //機具按下操作鈕
         if (count_mstart > 1) {
             if (firebaseListChanger == 0) {
                 firebaseArrayList.add(uploadData);
                 firebaseListChanger = 1;
                 count_portent = 0;
                 count_upload = 0;
                 count_mstart = 0;
                 count_receive = 0;
                 for (int i = 0; i < firebaseArrayList.size(); i++) {
                     String user_time = firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList.get(i).date).child(user_time).setValue(firebaseArrayList.get(i));
                 }
                 firebaseArrayList.clear();
             } else {
                 firebaseArrayList2.add(uploadData);
                 firebaseListChanger = 0;
                 count_portent = 0;
                 count_upload = 0;
                 count_receive = 0;
                 count_mstart = 0;
                 for (int i = 0; i < firebaseArrayList2.size(); i++) {
                     String user_time = firebaseArrayList2.get(i).time + "_" + firebaseArrayList2.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList2.get(i).date).child(user_time).setValue(firebaseArrayList2.get(i));
                 }
                 firebaseArrayList2.clear();
             }
         }

         //時間超過
         if (count_upload > firebasetime && count_accident == 0) {
             Log.d(TAG, "saveFireBase!!!" + gv.getFb_time());
             if (firebaseListChanger == 0) {
                 Log.d(TAG, "saveFireBase1: " + count_upload);
                 if (count_portent == 0) firebaseArrayList.add(uploadData);

                 firebaseListChanger = 1;
                 count_portent = 0;
                 count_upload = 0;
                 count_receive = 0;
                 count_mstart = 0;
                 for (int i = 0; i < firebaseArrayList.size(); i++) {
                     String user_time = firebaseArrayList.get(i).time + "_" + firebaseArrayList.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList.get(i).date).child(user_time).setValue(firebaseArrayList.get(i));
                 }
                 firebaseArrayList.clear();
             } else {
                 Log.d(TAG, "saveFireBase2: " + count_upload);
                 if (count_portent == 0) firebaseArrayList2.add(uploadData);

                 firebaseListChanger = 0;
                 count_portent = 0;
                 count_upload = 0;
                 count_receive = 0;
                 count_mstart = 0;

                 for (int i = 0; i < firebaseArrayList2.size(); i++) {
                     String user_time = firebaseArrayList2.get(i).time + "_" + firebaseArrayList2.get(i).id;
                     mDatabase.child(Site+"_MEMBER").child(firebaseArrayList2.get(i).date).child(user_time).setValue(firebaseArrayList2.get(i));
                 }
                 firebaseArrayList2.clear();
             }
         }
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

                userDao.addData(user);
                Log.i(TAG, "run: "+userDao.getAll().size());
            }
        });
    }

    public String getCurrentTime(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "HH:mm:ss:SSSS";

        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());

        return time;
    }

    public String  getReceiveAns() {
        GlobalVariable gv = (GlobalVariable) getApplicationContext();
        Integer ireceiveAns = (gv.getReceiveAccidentAnswer());
        String receiveID = (gv.getReceiveID());
        if (ireceiveAns==0) {
            String  receiveAns = null;
            return receiveAns;
        } else {
            String receiveAns = "{\"id\" : \""+receiveID+"\",\"accident\" : "+ireceiveAns+"}";
            count_receive=1;
            return receiveAns;
        }
    }

    public String gettoken(){
        GlobalVariable gv = (GlobalVariable)getApplicationContext();
        String token = (gv.getToken());
        return token;
    }

    public Boolean getMachine(){
        GlobalVariable gv = (GlobalVariable)getApplicationContext();
        boolean machine =false;
        if(gv.getIsMachineOperating()==true){
            machine = true;
        }if(gv.getIsMachineOperating()==false){
            machine = false;
        }
        return machine;
    }

    public String getMachineName(){
        GlobalVariable gv = (GlobalVariable)getApplicationContext();
        String machineName = "";
        if(gv.getIsMachineOperating()==true){
            machineName = gv.getMachineName();
        }if(gv.getIsMachineOperating()==false){
            machineName = "none";
        }
        return machineName;
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
            altitude = (float) location.getAltitude();
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
        GlobalVariable gv = (GlobalVariable) getApplicationContext();
        if (location != null) {
            latitude = (float) location.getLatitude();
            longitude = (float) location.getLongitude();
            altitude = (float) location.getAltitude();

        }
    }
}