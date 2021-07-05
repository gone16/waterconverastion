package com.water.app.waterconversation.Fragment;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.water.app.waterconversation.Constants;
import com.water.app.waterconversation.DataBase.AppDatabase;
import com.water.app.waterconversation.DataBase.UpdateAccumulationBarChartAfterDBOperation;
import com.water.app.waterconversation.DataBase.UpdateAccumulationTextAfterDBOperation;
import com.water.app.waterconversation.DataBase.UserDao;
import com.water.app.waterconversation.DataBase.UserRepository;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.MyTime;
import com.water.app.waterconversation.R;
import com.water.app.waterconversation.Service.ForeService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;
import static com.water.app.waterconversation.Activity.MainActivity.DeviceId;
import static com.water.app.waterconversation.Fragment.LocationFragment.isLocationFragment;
import static com.water.app.waterconversation.Fragment.SettingFragment.isSettingFragment;

public class BillboardFragment extends Fragment implements View.OnClickListener, ForeService.CallBacks,
        UpdateAccumulationTextAfterDBOperation, UpdateAccumulationBarChartAfterDBOperation, GoogleApiClient.ConnectionCallbacks ,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "BillboardFragment";

    public static Boolean isBillBoardFragment = true;
    private boolean save = false;
    private String photo, site, user, daka, longi, lati, alti, bindnum;
    private File outputImage;
    private FirebaseFirestore fs;
    private TextView t1, t2, d1, d2;
    private GoogleApiClient googleApiClient;
    private double latitude, longitude, altitude;

    private DatabaseReference mDatabase;
    private DatabaseReference userDataBase;
    private ValueEventListener valueEventListener;
    private DataSnapshot dataSnapshotAll;

    //處理Accumulation textview的array
    int[] textArray = new int[]{0, 0, 0, 0, 0, 0};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_billboard, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        googleApiClient = new GoogleApiClient.Builder(getActivity()).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
        //連接google api
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //初始化User數據
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        DeviceId = getDeviceName();
        site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "");
        user = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), "null");
        bindnum = sharedPreferences.getString("BindNumber", "");

        //初始化UI
        setUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(getActivity(), ForeService.class);
        intent.setAction("Bind");
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        if (globalVariable.getDetecting())
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        isBillBoardFragment = true;
    }

    @Override
    public void onPause() {
        super.onPause();
//        handler.removeCallbacks(runnable);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        handler.removeCallbacks(runnable);

        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        if (globalVariable.getDetecting()) getActivity().unbindService(serviceConnection);

        isBillBoardFragment = false;
    }

    //初始化UI
    private void setUI() {
        // 各UI的R.id
        int button_ids[] = {R.id.button_daka,R.id.button_D_start, R.id.button_reset};

        // 建立 button listener
        Button mButton = null;
        for (int i = 0; i < button_ids.length; i++) {
            if ((mButton = getActivity().findViewById(button_ids[i])) != null) {
                mButton.setOnClickListener(this);
            }
        }

        //如果 isdetecting，則將 "偵測" 按鈕改字改色
        checkDetectingUI();

        // 設定累計資訊的次數的textview
        setAccumulationText();

        // This function will setup accidents and portents of BarChart.
        setupBarChart();

        checkdakarecord();

        //checkBind();

        Log.d(TAG, "setUI() completed.");

    }

    private void checkBind() {
        final GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String sharenum = sharedPreferences.getString("BindNumber", null);
        Log.d(TAG, "sharenum" + sharenum);
        if (isNetworkConnected() == true) {
            FirebaseFirestore fs = FirebaseFirestore.getInstance();
            final DocumentReference docRef = fs.collection("CTC_Bind").document(user + bindnum);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.getString("phonenumber") != null && document.getString("phonenumber").equals("null")) {
                            Log.d(TAG, "bind:尚未綁定");
                            Toast.makeText(getActivity(), "請進行綁定", Toast.LENGTH_SHORT).show();
                            gv.setBind(false);
                            gv.setFbind(false);
                        } else if (document.getString("phonenumber") != null && document.getString("phonenumber").equals(bindnum)) {
                            gv.setBind(true);
                            gv.setFbind(true);
                            Log.d(TAG, "bind:已綁定");
                        } else {
                            Toast.makeText(getActivity(), "已綁定其它帳號", Toast.LENGTH_SHORT).show();
                            gv.setBind(false);
                        }
                    } else {
                        Log.d(TAG, "bind:尚未綁定,失敗");
                    }
                }
            });
        } else {
            Toast.makeText(getActivity(), "請打開網路", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    //設定累計textview
    private void setAccumulationText() {
        UserRepository userRepository;
        userRepository = new UserRepository(getActivity().getApplicationContext());
        userRepository.setDelegate(this);

        //去Room中搜尋state，並且更新UI介面
        userRepository.getAccidentByState(Constants.ACCIDENTS.DROP);
        userRepository.getAccidentByState(Constants.ACCIDENTS.FALL);
        userRepository.getAccidentByState(Constants.ACCIDENTS.COMA);
        userRepository.getPortentByState(Constants.PORTENTS.LOST_BALANCE);
        userRepository.getPortentByState(Constants.PORTENTS.HEAVY_STEP);
        userRepository.getPortentByState(Constants.PORTENTS.SUDDENLY_WOBBING);
    }

    //更新Accumulation text的動作
    public void updateAccumulationTextAfterDBOperation(int[] result) {

        int type = result[0];
        int state = result[1];
        int size = result[2];

        Log.d(TAG, "afterDBOperation: " + type + state + size);
//        int[] textArray = new int[6];


        switch (type) {
            case Constants.TYPE.ACCIDENT:
                switch (state) {
                    case Constants.ACCIDENTS.DROP:
                        textArray[0] = size;
                        break;
                    case Constants.ACCIDENTS.FALL:
                        textArray[1] = size;
                        break;
                    case Constants.ACCIDENTS.COMA:
                        textArray[2] = size;
                        break;
                }
                break;
            case Constants.TYPE.PORTENT:
                switch (state) {
                    case Constants.PORTENTS.LOST_BALANCE:
                        textArray[3] = size;
                        break;
                    case Constants.PORTENTS.HEAVY_STEP:
                        textArray[4] = size;
                        break;
                    case Constants.PORTENTS.SUDDENLY_WOBBING:
                        textArray[5] = size;
                        break;
                }
                break;
        }

        final TextView textViewAccident = getActivity().findViewById(R.id.text_accumulation_accident);
        final TextView textViewPortent = getActivity().findViewById(R.id.text_accumulation_portent);

        try {
            textViewAccident.setText("墜落：" + textArray[0] + "次\n" +
                    "跌倒：" + textArray[1] + "次\n" +
                    "昏迷：" + textArray[2] + "次");
            textViewPortent.setText("失去平衡：" + textArray[3] + "次\n" +
                    "突然重踩：" + textArray[4] + "次\n" +
                    "突然晃動：" + textArray[5] + "次");
        } catch (Exception e) {
            Log.e(TAG, "afterDBOperation: ", e);
        }

    }

    private void setupBarChart() {
        UserRepository userRepository;
        userRepository = new UserRepository(getActivity().getApplicationContext());
        userRepository.setDelegateBar(this);

        userRepository.getAccidentByStateAndDate(1, "2019-06-24");
    }

    @Override
    public void updateAccumulationBarChart(float[] result) {
        Log.d(TAG, "updateAccumulationBarChart: ");

        try {
            BarChart barChart = getActivity().findViewById(R.id.barChart);

            List<BarEntry> barEntryList = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                barEntryList.add(new BarEntry(i + 1, new float[]{result[i], result[i + 12], result[i + 24], result[i + 36]}));
            }

//        for(int i =0; i<12; i++){
//            barEntryList.add(new BarEntry(i+1,new float[]{i%2,i%3,i%4,i%5}));
//        }

            BarDataSet barDataSet = new BarDataSet(barEntryList, "");
            barDataSet.setColors(getColors());
            barDataSet.setStackLabels(new String[]{"前兆", "墜落", "跌倒", "昏迷"});

            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.8f);

            XAxis xAxis = barChart.getXAxis();
            YAxis yAxisL = barChart.getAxisLeft();
            YAxis yAxisR = barChart.getAxisRight();

            xAxis.setAxisMinimum(0.0f);
//        xAxis.setAxisMaximum(12.0f);
            xAxis.setValueFormatter(new XAxisValueFormatter());

            yAxisL.setAxisMinimum(0.0f);
            yAxisR.setAxisMinimum(0.0f);
            yAxisL.setValueFormatter(new YAixValueFormatterForLR());
            yAxisR.setValueFormatter(new YAixValueFormatterForLR());

            // Both of below can format the bar value.
//        barData.setValueFormatter(new YAxisValueFormatter());
            barDataSet.setValueFormatter(new YAxisValueFormatter());

            barChart.getDescription().setEnabled(false);
            barChart.setTouchEnabled(true);
            barChart.setDragEnabled(false);
            barChart.setScaleEnabled(false);
            barChart.setPinchZoom(false);
            barChart.setDoubleTapToZoomEnabled(false);
            barChart.setData(barData);
            barChart.setFitBars(true); // make the x-axis fit exactly all bars
            barChart.invalidate(); // refresh
        } catch (Exception e) {
            Log.e(TAG, "updateAccumulationBarChart: ", e);
        }

        try {
            LineChart lineChart = getActivity().findViewById(R.id.lineChart);

            List<Entry> PortentsList = new ArrayList<Entry>();
            List<Entry> DropList = new ArrayList<Entry>();
            List<Entry> FallList = new ArrayList<Entry>();
            List<Entry> ComaList = new ArrayList<Entry>();


            for (int i = 0; i < 12; i++) {
                PortentsList.add(new Entry(i + 1, result[i]));
                DropList.add(new Entry(i + 1, result[i + 12]));
                FallList.add(new Entry(i + 1, result[i + 24]));
                ComaList.add(new Entry(i + 1, result[i + 36]));
            }

            LineDataSet PortentDataSet = new LineDataSet(PortentsList, "前兆");
            LineDataSet DropDataSet = new LineDataSet(DropList, "墜落");
            LineDataSet FallDataSet = new LineDataSet(FallList, "跌倒");
            LineDataSet ComaDataSet = new LineDataSet(ComaList, "昏迷");

            PortentDataSet.setCircleColor(Color.parseColor("#a1887f"));
            DropDataSet.setCircleColor(Color.parseColor("#ff3d00"));
            FallDataSet.setCircleColor(Color.parseColor("#ffea00"));
            ComaDataSet.setCircleColor(Color.parseColor("#ffab40"));

            PortentDataSet.setColors(Color.parseColor("#a1887f"));
            DropDataSet.setColors(Color.parseColor("#ff3d00"));
            FallDataSet.setColors(Color.parseColor("#ffea00"));
            ComaDataSet.setColors(Color.parseColor("#ffab40"));

            List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();


            // 危險程度低的先畫，畫折線圖的時候才不會被蓋過去
            dataSets.add(PortentDataSet);
            dataSets.add(FallDataSet);
            dataSets.add(ComaDataSet);
            dataSets.add(DropDataSet);
            LineData data = new LineData(dataSets);

            XAxis xAxisLine = lineChart.getXAxis();
            YAxis yAxisLLine = lineChart.getAxisLeft();
            YAxis yAxisRLine = lineChart.getAxisRight();

            xAxisLine.setAxisMinimum(0.0f);
//        xAxisLine.setAxisMaximum(12.0f);
            xAxisLine.setValueFormatter(new XAxisValueFormatter());

            yAxisLLine.setAxisMinimum(0.0f);
            yAxisRLine.setAxisMinimum(0.0f);
            yAxisLLine.setValueFormatter(new YAixValueFormatterForLR());
            yAxisRLine.setValueFormatter(new YAixValueFormatterForLR());

            data.setValueFormatter(new YAxisValueFormatter());


            lineChart.getDescription().setEnabled(false);
            lineChart.setTouchEnabled(true);
            lineChart.setDragEnabled(false);
            lineChart.setScaleEnabled(false);
            lineChart.setPinchZoom(false);
            lineChart.setDoubleTapToZoomEnabled(false);
            lineChart.setData(data);
            lineChart.invalidate(); // refresh
        } catch (Exception e) {
            Log.e(TAG, "updateAccumulationBarChart: ", e);
        }


    }


    private String transformHour(int hour, int index) {
        if (hour > index) {
            return String.valueOf(hour - index);
        } else {
            return String.valueOf(24 + hour - index);
        }
    }


    private class XAxisValueFormatter extends ValueFormatter {

        private DecimalFormat mFormat;

        public XAxisValueFormatter() {
            mFormat = new DecimalFormat("###,###,##0");
        }

        @Override
        public String getFormattedValue(float value) {
            MyTime myTime = new MyTime();
            int hour = Integer.valueOf(myTime.getCurrentHour());

            int index = (int) value;
//            return transformHour(hour,index);
            switch (index) {
                case 0:
                    return "";
                case 1:
                    return transformHour(hour, 12);
                case 2:
                    return transformHour(hour, 11);
                case 3:
                    return transformHour(hour, 10);
                case 4:
                    return transformHour(hour, 9);
                case 5:
                    return transformHour(hour, 8);
                case 6:
                    return transformHour(hour, 7);
                case 7:
                    return transformHour(hour, 6);
                case 8:
                    return transformHour(hour, 5);
                case 9:
                    return transformHour(hour, 4);
                case 10:
                    return transformHour(hour, 3);
                case 11:
                    return transformHour(hour, 2);
                case 12:
                    return transformHour(hour, 1);

            }
            return mFormat.format(value);
        }

    }

    // This class used to format values of each bar from float to int.
    private class YAxisValueFormatter extends ValueFormatter {

        private DecimalFormat mFormat;

        public YAxisValueFormatter() {
            mFormat = new DecimalFormat("###,###,##0");
        }

        @Override
        public String getFormattedValue(float value) {

            if (value == 0.0f) {
                return "";
            }
            return mFormat.format(value);
        }

    }

    private class YAixValueFormatterForLR extends ValueFormatter {
        private DecimalFormat mFormat;

        public YAixValueFormatterForLR() {
            mFormat = new DecimalFormat("###,###,##0");
        }

        @Override
        public String getFormattedValue(float value) {
            if (value % 1 != 0) {
                return "";
            }
            return mFormat.format(value);
        }
    }

    // This class used to format values of each bar from float to int.
    private class YAxisValueFormatterLine extends ValueFormatter {

        private DecimalFormat mFormat;

        public YAxisValueFormatterLine() {
            mFormat = new DecimalFormat("###,###,##0");
        }


        @Override
        public String getFormattedValue(float value) {

            if (value == 0.0f) {
                return "";
            }
            return mFormat.format(value);
        }

    }

    private void saveSwitchState(int id, boolean state) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(String.valueOf(id), state);
        Log.d(TAG, "saveSeekbarProgress: " + id);
        editor.apply();
    }

    //取得seekbar的初始值
    private boolean getFirstSwitchValue(int id) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean state = sharedPreferences.getBoolean(String.valueOf(id), false);
        return state;
    }

    //按下button時，全域變數需要改變
    @Override
    public void onClick(View v) {
        final GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        switch (v.getId()) {

            //打卡上班
            case R.id.button_daka:
                final Button buttonDaka = getActivity().findViewById(R.id.button_daka);
                final TextView t1 = getActivity().findViewById(R.id.time1);
                final TextView t2 = getActivity().findViewById(R.id.time2);
                final TextView d1 = getActivity().findViewById(R.id.daka1);
                final TextView d2 = getActivity().findViewById(R.id.daka2);

                //確認工地代碼不為空
                if (sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "").equals("") && !globalVariable.getDetecting()) {
                    Toast.makeText(getActivity(), "請確認工地代號後再打卡", Toast.LENGTH_SHORT).show();
                    return;
                }
                //確認user不為空
                if (sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), "").equals("") && !globalVariable.getDetecting()) {
                    Toast.makeText(getActivity(), "請確認姓名或代號後再打卡", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(globalVariable.getBind() == false){
                    Toast.makeText(getActivity(), "請先綁定再進行打卡", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (globalVariable.getDaka() == false) {
                    FirebaseFirestore fs = FirebaseFirestore.getInstance();
                    DocumentReference docRef = fs.collection("CTC_Attendance").document(getCurrentDate());
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    globalVariable.setAttendance(document.getBoolean(user));
                                } else {
                                    Log.d(TAG, "nope");
                                    globalVariable.setAttendance(false);
                                }
                            } else {
                                Log.d(TAG, "failed");
                            }
                        }
                    });
                    if (globalVariable.getAttendance() == true) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                outputImage = new File(Environment.getExternalStorageDirectory(), "image.jpg");
                                try {
                                    //判断文件是否存在，存在删除，不存在创建
                                    if (outputImage.exists()) {
                                        Log.e(TAG, "exists");
                                        outputImage.delete();
                                    }
                                    outputImage.createNewFile();
                                    Log.e(TAG, "create");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    Uri imageUri = FileProvider.getUriForFile(getContext(), "com.water.app.waterconversation.fileprovider", outputImage);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                } else {
                                    Uri imageUri = Uri.fromFile(outputImage);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                }
                                startActivityForResult(intent, 1);
                            }
                        });
                        builder.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.setTitle("提醒");
                        dialog.setMessage("名稱：" + sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), "") + "\n工地：" + sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "") + "\n是否進行打卡？");
                        dialog.show();
                    }else{
                        Toast.makeText(getActivity(),"今日未出工，請聯絡相關管理人員",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            outputImage = new File(Environment.getExternalStorageDirectory(), "image.jpg");
                            try {
                                //判断文件是否存在，存在删除，不存在创建
                                if (outputImage.exists()) {
                                    Log.e(TAG, "exists");
                                    outputImage.delete();
                                }
                                outputImage.createNewFile();
                                Log.e(TAG, "create");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Uri imageUri = FileProvider.getUriForFile(getContext(), "com.water.app.waterconversation.fileprovider", outputImage);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            } else {
                                Uri imageUri = Uri.fromFile(outputImage);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            }
                            startActivityForResult(intent, 1);
                        }
                    });
                    builder.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setTitle("提醒");
                    dialog.setMessage("名稱：" + sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), "") + "\n工地：" + sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "") + "\n是否進行打卡？");
                    dialog.show();
                }
                    break;



                //開始偵測 ;  停止偵測
            case R.id.button_D_start:

                //確認工地代碼不為空
                if (sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "").equals("") && !globalVariable.getDetecting()) {
                    Toast.makeText(getActivity(), "請確認工地代號後再開始偵測", Toast.LENGTH_SHORT).show();
                    return;
                }
                //確認user不為空
                if (sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), "").equals("") && !globalVariable.getDetecting()) {
                    Toast.makeText(getActivity(), "請確認姓名或代號後再開始偵測", Toast.LENGTH_SHORT).show();
                    return;
                }
                /*/
                if(globalVariable.getBind() == false){
                    Toast.makeText(getActivity(), "請先綁定再進行偵測", Toast.LENGTH_SHORT).show();
                    return;
                }
                /*/

                //確認警報開啟
                if (globalVariable.getOpen_accident_alarm() == false) {
                    Toast.makeText(getActivity(), "請開啟意外警報再偵測", Toast.LENGTH_SHORT).show();
                    return;
                }

                final Button buttonStart = getActivity().findViewById(R.id.button_D_start);

                //如果沒在偵測，開始偵測
                if (!globalVariable.getDetecting()) {

                    //建立確認是否以此工地代號與姓名偵測的dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            globalVariable.setDetecting(true);
                            buttonStart.setBackgroundResource(R.drawable.button_rounde);
                            buttonStart.getBackground().setColorFilter(0xFF009688, android.graphics.PorterDuff.Mode.SRC);
                            buttonStart.setText(R.string.stop_detect);

                            Intent intent = new Intent(getActivity(), ForeService.class);
                            intent.setAction("Bind");
                            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

                            startForeService();

                            Toast.makeText(getContext(), R.string.start_detect, Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            Log.d(TAG, "click no");
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setTitle("提醒");
                    dialog.setMessage("名稱：" + sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), "") + "\n工地：" + sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "") + "\n是否開始偵測？");
                    dialog.show();


                } else {
                    globalVariable.setDetecting(false);
                    buttonStart.setBackgroundResource(R.drawable.button_rounde);
                    buttonStart.getBackground().setColorFilter(null);
                    buttonStart.setText(R.string.start_detect);

                    stopService();

                    Toast.makeText(this.getContext(), R.string.stop_detect, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onClick isDetecting" + globalVariable.getDetecting());
                break;
            // 重置資料
            case R.id.button_reset:
                if (globalVariable.getDetecting()) {
                    Toast.makeText(getActivity(), "請先停止偵測", Toast.LENGTH_SHORT).show();
                    return;
                }
                //建立Alert來提醒是否要重置資料
                AlertDialog.Builder builderReset = new AlertDialog.Builder(getActivity());
                builderReset.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        if (globalVariable.getDetecting()) return;

                        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 15, 1,
                                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3),
                                new ThreadPoolExecutor.DiscardOldestPolicy());
                        threadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                AppDatabase appDatabase = Room.databaseBuilder(getContext(),
                                        AppDatabase.class, "User").build();

                                UserDao userDao = appDatabase.getUserDao();

                                userDao.deleteAll();
                                Log.i(TAG, "run: " + userDao.getAll().size());
                            }
                        });
                        setAccumulationText();
                        Log.d(TAG, "click ok");
                        Toast.makeText(getActivity(), R.string.reset_data, Toast.LENGTH_SHORT).show();
                    }
                });
                builderReset.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        Log.d(TAG, "click no");
                    }
                });
                AlertDialog dialogReset = builderReset.create();
                dialogReset.setTitle(R.string.alert_reset_title);
                dialogReset.show();
                break;
        }
    }


    //如果 isdetecting，則將 "偵測" 按鈕改字改色
    private void checkDetectingUI() {
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        Button buttonStart = getActivity().findViewById(R.id.button_D_start);
        Button buttondaka = getActivity().findViewById(R.id.button_daka);
        buttonStart.setBackgroundResource(R.drawable.button_rounde);
        if (globalVariable.getDetecting()) {
            buttonStart.setBackgroundResource(R.drawable.button_rounde);
            buttonStart.getBackground().setColorFilter(0xFF009688, android.graphics.PorterDuff.Mode.SRC);
            buttonStart.setText(R.string.stop_detect);
        }
        if (globalVariable.getDaka() == true) {
            buttondaka.setBackgroundResource(R.drawable.button_rounde);
            buttondaka.getBackground().setColorFilter(Color.parseColor("#FF3333"), PorterDuff.Mode.SRC);
            buttondaka.setText("打卡下班");
        }
    }

    private void checkdakarecord() {
        t1 = getActivity().findViewById(R.id.time1);
        t2 = getActivity().findViewById(R.id.time2);
        d1 = getActivity().findViewById(R.id.daka1);
        d2 = getActivity().findViewById(R.id.daka2);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        t1.setText(sharedPreferences.getString("sharePreferences_time1", ""));
        t2.setText(sharedPreferences.getString("sharePreferences_time2", ""));
        d1.setText(sharedPreferences.getString("sharePreferences_daka1", ""));
        d2.setText(sharedPreferences.getString("sharePreferences_daka2", ""));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Button buttonDaka = getActivity().findViewById(R.id.button_daka);
        final TextView t1 = getActivity().findViewById(R.id.time1);
        final TextView t2 = getActivity().findViewById(R.id.time2);
        final TextView d1 = getActivity().findViewById(R.id.daka1);
        final TextView d2 = getActivity().findViewById(R.id.daka2);
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        if (resultCode == RESULT_OK && requestCode == 1) {
            try {
                ExifInterface exifInterface = new ExifInterface(outputImage.getAbsolutePath());
                alti = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                longi= exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                lati = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                Bitmap bitmap = BitmapFactory.decodeFile(outputImage.getAbsolutePath(), options);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] bytes = baos.toByteArray();
                //base64 encode
                photo = Base64.encodeToString(bytes, Base64.DEFAULT);
                longitude = GetDegree(longi);
                latitude = GetDegree(lati);
                altitude = GetAlti(alti);
                if(photo != null) {
                    if (gv.getDaka() == false) {
                        gv.setDaka(true);
                        buttonDaka.setText(R.string.button_dakaOut);
                        daka = "上班";
                        t2.setText(gv.getDakatime());
                        d2.setText(gv.getDakasit());
                        t1.setText(getTableTime());
                        d1.setText(daka);
                        buttonDaka.getBackground().setColorFilter(Color.parseColor("#FF3333"), PorterDuff.Mode.SRC);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        sharedPreferences.edit().putString("sharePreferences_time1", getTableTime()).apply();
                        sharedPreferences.edit().putString("sharePreferences_daka1", daka).apply();
                        gv.setDakatime(getTableTime());
                        gv.setDakasit(daka);
                        saveInData();
                    } else {
                        gv.setDaka(false);
                        buttonDaka.setText(R.string.button_dakaIn);
                        daka = "下班";
                        t2.setText(gv.getDakatime());
                        d2.setText(gv.getDakasit());
                        t1.setText(getTableTime());
                        d1.setText(daka);
                        gv.setDakatime(getTableTime());
                        gv.setDakasit(daka);
                        buttonDaka.getBackground().setColorFilter(null);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        sharedPreferences.edit().putString("sharePreferences_time1", getTableTime()).apply();
                        sharedPreferences.edit().putString("sharePreferences_daka1", daka).apply();
                        sharedPreferences.edit().putString("sharePreferences_time2", gv.getDakatime()).apply();
                        sharedPreferences.edit().putString("sharePreferences_daka2", gv.getDakasit()).apply();
                        saveOutData();
                    }
                }else{
                    Toast.makeText(getActivity(),"未拍照完成，請重新拍照",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("Exception", e.getMessage(), e);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Double GetDegree(String stringDMS) {
        Double result = null;
        String[] DMS = stringDMS.split(",");
        String[] stringD = DMS[0].split("/",2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        Double D =D0/D1;

        String[] stringM = DMS[1].split("/",2);
        Double M0 = new Double(stringM[0]);
        Double M1 = new Double(stringM[1]);
        Double M =(M0/M1)/60;

        String[] stringS = DMS[2].split("/",2);
        Double S0 = new Double(stringS[0]);
        Double S1 = new Double(stringS[1]);
        Double S =(S0/S1)/3600;
        result = new Double(D+M+S);
        return result;
    }
    private Double GetAlti(String string) {
        Double result = null;
        String[] DMS = string.split(",");
        String[] stringD = DMS[0].split("/",2);
        Double D0 = new Double(stringD[0]);
        Double D1 = new Double(stringD[1]);
        result = new Double(D0/D1);
        return result;
    }


    private void saveInData() {
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        fs = FirebaseFirestore.getInstance();
        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        Map<String, Object> m = new HashMap<>();
        m.put("date", getCurrentDate());
        m.put("id", user);
        m.put("site", site);
        m.put("starttime", getCurrentTime());
        m.put("endtime", get1hourafter());
        m.put("photo", photo);
        m.put("timestamp", ts);
        m.put("title", daka);
        m.put("altitude", altitude);
        m.put("longitude", longitude);
        m.put("latitude", latitude);
        fs.collection("CTC_punchInOut").document(getCurrentDate()).collection(user).document(daka).set(m, SetOptions.merge());
    }

    private void saveOutData() {
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        fs = FirebaseFirestore.getInstance();
        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        Map<String, Object> m = new HashMap<>();
        m.put("date", getCurrentDate());
        m.put("id", user);
        m.put("site", site);
        m.put("starttime", gv.getDakatime());
        m.put("endtime", getCurrentTime());
        m.put("photo", photo);
        m.put("timestamp", ts);
        m.put("title", daka);
        m.put("altitude", altitude);
        m.put("longitude", longitude);
        m.put("latitude", latitude);
        fs.collection("CTC_punchInOut").document(getCurrentDate()).collection(user).document(daka).set(m, SetOptions.merge());
    }

    public String getTableTime() {
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "yyyy/MM/dd hh:mm:ss";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());
        return time;
    }

    public String getCurrentTime() {
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "hh:mm:ss:SSS";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());
        return time;
    }

    public String get1hourafter() {
        Calendar mCal = Calendar.getInstance();
        mCal.add(Calendar.HOUR, 1);
        String dataFormat = "hh:mm:ss:SSS";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());
        return time;
    }

    public String getCurrentDate() {
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


    private int[] getColors() {

        int stackSize = 4;

        // have as many colors as stack-values per entry
        int[] colors = new int[stackSize];

        colors[0] = Color.parseColor("#a1887f"); // 前兆：咖啡色
        colors[1] = Color.parseColor("#ff3d00"); // 墜落：紅色
        colors[2] = Color.parseColor("#ffea00"); // 跌倒：黃色
        colors[3] = Color.parseColor("#ffab40"); // 昏迷：橘色
        return colors;
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
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ForeService foreService;
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ForeService.LocalBinder binder = (ForeService.LocalBinder) service;
            foreService = binder.getService();

            foreService.registerClient(BillboardFragment.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };


    private void startForeService() {

        Intent intent = new Intent(this.getContext(), ForeService.class);
        intent.setAction(Constants.ACTION.START_FOREGROUND_ACTION);
        if (this.getContext() == null) return;
        ContextCompat.startForegroundService(this.getContext(), intent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this.getContext(), ForeService.class);
        serviceIntent.setAction(Constants.ACTION.STOP_FOREGROUND_ACTION);
        getActivity().unbindService(serviceConnection);
        getActivity().stopService(serviceIntent);
    }

    @Override
    public void updatePhoneChart(float x, float latitude, float longitude, float altitude) {
        if (!isBillBoardFragment) return;
        if (isLocationFragment || isSettingFragment) return;
        TextView textView = getActivity().findViewById(R.id.textView_altitude);
        textView.setText("海拔高度：" + altitude + "m");
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: ");
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Permissions ok, we get last location
        //宣告位置數值
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            Log.d(TAG, "Latitude" + location.getLatitude() + "    Longitude" + location.getLongitude());
            latitude = (float)location.getLatitude();
            longitude = (float)location.getLongitude();
            altitude = (float)location.getAltitude();
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
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);

        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)

            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, (com.google.android.gms.location.LocationListener) this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}