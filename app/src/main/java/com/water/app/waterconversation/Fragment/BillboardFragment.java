package com.water.app.waterconversation.Fragment;

import android.arch.persistence.room.Room;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
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
import com.water.app.waterconversation.firebase.UploadData;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.water.app.waterconversation.Activity.MainActivity.DeviceId;
import static com.water.app.waterconversation.Activity.MainActivity.OpenAccidentAlarm;
import static com.water.app.waterconversation.Activity.MainActivity.OpenPortentAlarm;

public class BillboardFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, ForeService.CallBacks, UpdateAccumulationTextAfterDBOperation, UpdateAccumulationBarChartAfterDBOperation {

    private static final String TAG = "BillboardFragment";

    private Boolean isBillBoardFragment = true;
    private boolean save = false;

    private DatabaseReference mDatabase;
    private DatabaseReference userDataBase;
    private ValueEventListener valueEventListener;
    private DataSnapshot dataSnapshotAll;

    private String UserId;
    private String Site;

//    Handler handler = new Handler();

    EditText editTextName;
    EditText editTextSite;

    //處理Accumulation textview的array
    int[] textArray  = new int[]{0, 0, 0, 0, 0, 0};



//    GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_billboard, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Retrieve secondary app.
//        FirebaseApp secondary = FirebaseApp.getInstance("water-user");
// Get the database for the other app.
//        userDataBase = FirebaseDatabase.getInstance(secondary).getReference();

//        secondaryDatabase.child("user").child("user1").setValue("");

//        FirebaseApp.initializeApp(getActivity());
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//        mDatabase = FirebaseDatabase.getInstance().getReference();
//        setValueEventListener();

//        mDatabase.child("test0524").setValue("");

//        copyUserToUserDataBase();

    }





    //複製user到water-user資料庫
    private void copyUserToUserDataBase(){
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterable iterableSite = dataSnapshot.child("User").getChildren();
                for(Object anIterable: iterableSite){
                    DataSnapshot dataSnapshotSite = (DataSnapshot) anIterable;
                    userDataBase.child("User").child(dataSnapshotSite.getKey()).setValue("");
//                    if(dataSnapshotSite.getKey().equals(editTextSite.getText().toString())){
//                        SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        editor.putString("site",editTextSite.getText().toString());
//                        editor.apply();
//                        Site = sharedPreferences.getString("site","");
//                        foundSite = true;
//                        Toast.makeText(getActivity(),"已儲存工地",Toast.LENGTH_SHORT).show();
//                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }




    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //初始化User數據
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        DeviceId = getDeviceName();
        UserId = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),"");
        Site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"");

        //初始化UI
        setUI();

        Log.d(TAG, "onViewCreated: "+sharedPreferences.getString("userid","no name"));


    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(getActivity(), ForeService.class);
        intent.setAction("Bind");
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        if(globalVariable.getDetecting()) getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        if(globalVariable.getDetecting()) getActivity().unbindService(serviceConnection);

        isBillBoardFragment = false;
    }

    //初始化UI
    private void setUI() {

        FirebaseApp.initializeApp(this.getContext());
//        FirebaseApp.initializeApp(this.getActivity());

        // 各UI的R.id
        int switch_ids[] = {R.id.switch_accident, R.id.switch_precursor, R.id.switch_location,R.id.switch_accident_alarm,R.id.switch_portent_alarm};
        int button_ids[] = {R.id.button_start, R.id.button_reset}; //,R.id.button_billfragment_logout};



        // 建立 switch listener
        Switch mSwitch = null;
        for (int i = 0; i < switch_ids.length; i++) {
            if ((mSwitch = getActivity().findViewById(switch_ids[i])) != null) {
                mSwitch.setOnCheckedChangeListener(this);
                boolean state = getFirstSwitchValue(mSwitch.getId());
                mSwitch.setChecked(state);
            }
        }

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

        // This function will setup accidents and portents of LineChart.
//        setupLineChart();

        Log.d(TAG, "setUI() completed.");

    }



    //設定累計textview
    private void setAccumulationText(){
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
    public void updateAccumulationTextAfterDBOperation(int[] result){

        int type = result[0];
        int state = result[1];
        int size =  result[2];

        Log.d(TAG, "afterDBOperation: "+type +state+size);
//        int[] textArray = new int[6];


        switch (type){
            case Constants.TYPE.ACCIDENT:
                switch (state){
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
                switch (state){
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

        try{
            textViewAccident.setText("墜落："+textArray[0]+ "次\n"+
                    "跌倒："+textArray[1]+ "次\n" +
                    "昏迷："+textArray[2]+ "次");
            textViewPortent.setText("失去平衡："+textArray[3] + "次\n" +
                    "突然重踩："+textArray[4]+ "次\n" +
                    "突然晃動："+textArray[5]+"次");
        }catch (Exception e){
            Log.e(TAG, "afterDBOperation: ",e );
        }

    }

    private void setupBarChart(){
        UserRepository userRepository;
        userRepository = new UserRepository(getActivity().getApplicationContext());
        userRepository.setDelegateBar(this);

        userRepository.getAccidentByStateAndDate(1,"2019-06-24");
    }

    @Override
    public void updateAccumulationBarChart(float[] result) {
        Log.d(TAG, "updateAccumulationBarChart: ");

        try{
        BarChart barChart = getActivity().findViewById(R.id.barChart);

        List<BarEntry> barEntryList =new ArrayList<>();
        for(int i = 0; i<12 ; i++){
            barEntryList.add(new BarEntry(i+1,new float[]{result[i], result[i+12], result[i+24], result[i+36]}));
        }

//        for(int i =0; i<12; i++){
//            barEntryList.add(new BarEntry(i+1,new float[]{i%2,i%3,i%4,i%5}));
//        }

        BarDataSet barDataSet = new BarDataSet(barEntryList,"");
        barDataSet.setColors(getColors());
        barDataSet.setStackLabels(new String[]{"前兆","墜落","跌倒","昏迷"});

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.8f);

        XAxis xAxis= barChart.getXAxis();
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
        }catch (Exception e){
            Log.e(TAG, "updateAccumulationBarChart: ",e );
        }

        try {
        LineChart lineChart = getActivity().findViewById(R.id.lineChart);

        List<Entry> PortentsList = new ArrayList<Entry>();
        List<Entry> DropList = new ArrayList<Entry>();
        List<Entry> FallList = new ArrayList<Entry>();
        List<Entry> ComaList = new ArrayList<Entry>();


        for(int i=0; i<12; i++){
            PortentsList.add(new Entry(i+1, result[i]));
            DropList.add(new Entry(i + 1, result[i+12]));
            FallList.add(new Entry(i + 1, result[i+24]));
            ComaList.add(new Entry(i + 1, result[i+36]));
        }

//        for(int i =0; i<13; i++){
//            if(i==12){
//                PortentsList.add(new Entry());
//                DropList.add(new Entry());
//                FallList.add(new Entry());
//                ComaList.add(new Entry());
//            }else {
//                PortentsList.add(new Entry(i + 1, i % 2));
//                DropList.add(new Entry(i + 1, i % 3));
//                FallList.add(new Entry(i + 1, i % 4));
//                ComaList.add(new Entry(i + 1, i % 5));
//            }
//        }

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

        XAxis xAxisLine= lineChart.getXAxis();
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
        }catch (Exception e){
            Log.e(TAG, "updateAccumulationBarChart: ",e );
        }


    }


    private String transformHour(int hour, int index){
        if(hour>index){
            return String.valueOf(hour-index);
        }else {
            return String.valueOf(24+hour-index);
        }
    }

    private class XAxisValueFormatter extends ValueFormatter{

        private DecimalFormat mFormat;

        public XAxisValueFormatter() {
            mFormat = new DecimalFormat("###,###,##0");
        }
        @Override
        public String getFormattedValue(float value) {
            MyTime myTime = new MyTime();
            int hour =Integer.valueOf(myTime.getCurrentHour());

            int index = (int)value;
//            return transformHour(hour,index);
            switch (index){
                case 0:
                    return "";
                case 1:
                    return transformHour(hour,12);
                case 2:
                    return transformHour(hour,11);
                case 3:
                    return transformHour(hour,10);
                case 4:
                    return transformHour(hour,9);
                case 5:
                    return transformHour(hour,8);
                case 6:
                    return transformHour(hour,7);
                case 7:
                    return transformHour(hour,6);
                case 8:
                    return transformHour(hour,5);
                case 9:
                    return transformHour(hour,4);
                case 10:
                    return transformHour(hour,3);
                case 11:
                    return transformHour(hour,2);
                case 12:
                    return transformHour(hour,1);

            }
            return mFormat.format(value);
        }

    }

    // This class used to format values of each bar from float to int.
    private class YAxisValueFormatter extends ValueFormatter{

        private DecimalFormat mFormat;

        public YAxisValueFormatter() {
            mFormat = new DecimalFormat("###,###,##0");
        }
        @Override
        public String getFormattedValue(float value) {

            if(value == 0.0f){
                return "";
            }
            return mFormat.format(value);
        }

    }

    private class YAixValueFormatterForLR extends ValueFormatter{
        private DecimalFormat mFormat;

        public YAixValueFormatterForLR() {
            mFormat = new DecimalFormat("###,###,##0");
        }
        @Override
        public String getFormattedValue(float value) {
            if(value%1 !=0){
                return "";
            }
            return mFormat.format(value);
        }
    }

    // This class used to format values of each bar from float to int.
    private class YAxisValueFormatterLine extends ValueFormatter{

        private DecimalFormat mFormat;

        public YAxisValueFormatterLine() {
            mFormat = new DecimalFormat("###,###,##0");
        }


        @Override
        public String getFormattedValue(float value) {

            if(value == 0.0f){
                return "";
            }
            return mFormat.format(value);
        }

    }



    private void writeNewUser(String id, String idDevice, String date, String time, float latitude, float longitude,
                              float accX, float accY, float accZ, int accident, int accidentAns, int portent,
                              int portentAns, String site, float altitude,float heartRate) {
        UploadData user = new UploadData(id,idDevice,date,time,latitude,longitude,accX,accY,accZ,accident,accidentAns,portent,portentAns,site,altitude,heartRate);

//        mDatabase.child("daliyData").

        mDatabase.child("test").child(id).child(time).setValue(user);
    }


    // switch 變化時，全域變數需要改變
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        switch (buttonView.getId()) {
            case R.id.switch_accident:
                if (isChecked) {
                    globalVariable.setOpen_accident(true);
                    saveSwitchState(R.id.switch_accident,true);
                    Toast.makeText(this.getContext(), R.string.open_accident, Toast.LENGTH_SHORT).show();
                } else {
                    globalVariable.setOpen_accident(false);
                    saveSwitchState(R.id.switch_accident,false);
                    Toast.makeText(this.getContext(), R.string.close_accident, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onCheckedChanged accident: " + globalVariable.getOpen_accident());
                break;
            case R.id.switch_precursor:
                if (isChecked) {
                    globalVariable.setOpen_precursor(true);
                    saveSwitchState(R.id.switch_precursor,true);
                    Toast.makeText(this.getContext(), R.string.open_precursor, Toast.LENGTH_SHORT).show();
                } else {
                    globalVariable.setOpen_precursor(false);
                    saveSwitchState(R.id.switch_precursor,false);
                    Toast.makeText(this.getContext(), R.string.close_precursor, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onCheckedChanged precursor:" + globalVariable.getOpen_precursor());
                break;
            case R.id.switch_location:
                if (isChecked) {
                    globalVariable.setOpen_location(true);
                    saveSwitchState(R.id.switch_location,true);
                    Toast.makeText(this.getContext(), R.string.open_location, Toast.LENGTH_SHORT).show();
                } else {
                    globalVariable.setOpen_location(false);
                    saveSwitchState(R.id.switch_location,false);
                    Toast.makeText(this.getContext(), R.string.close_location, Toast.LENGTH_SHORT).show();

                }
                Log.d(TAG, "onCheckedChanged location:" + globalVariable.getOpen_location());
                break;
            case R.id.switch_accident_alarm:
                if(isChecked){
                    OpenAccidentAlarm = true;
                    saveSwitchState(R.id.switch_accident_alarm,true);
                    Toast.makeText(this.getContext(), R.string.open_accident_alarm, Toast.LENGTH_SHORT).show();

                }else{
                    Toast.makeText(this.getContext(), R.string.close_accident_alarm, Toast.LENGTH_SHORT).show();
                    OpenAccidentAlarm = false;
                    saveSwitchState(R.id.switch_accident_alarm,false);
                }
                break;
            case R.id.switch_portent_alarm:
                if (isChecked){
                    OpenPortentAlarm = true;
                    saveSwitchState(R.id.switch_portent_alarm,true);
                    Toast.makeText(this.getContext(), R.string.open_portent_alarm, Toast.LENGTH_SHORT).show();
                }else {
                    OpenPortentAlarm =false;
                    saveSwitchState(R.id.switch_portent_alarm,false);
                    Toast.makeText(this.getContext(), R.string.close_Portent_alarm, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void saveSwitchState(int id,boolean state){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(String.valueOf(id),state);
        Log.d(TAG, "saveSeekbarProgress: "+ id);
        editor.apply();
    }

    //取得seekbar的初始值
    private boolean getFirstSwitchValue(int id){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean state = sharedPreferences.getBoolean(String.valueOf(id),false);
        return state;
    }

    //按下button時，全域變數需要改變
    @Override
    public void onClick(View v) {
        final GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        switch (v.getId()) {

            //開始偵測 ;  停止偵測
            case R.id.button_start:
                final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                final SharedPreferences.Editor editor = sharedPreferences.edit();

                //確認工地代碼不為空
                if(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"").equals("") && !globalVariable.getDetecting()){
                    Toast.makeText(getActivity(),"請確認工地代號後再開始偵測",Toast.LENGTH_SHORT).show();
                    return;
                }
                //確認user不為空
                if(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),"").equals("") && !globalVariable.getDetecting()){
                    Toast.makeText(getActivity(),"請確認姓名或代號後再開始偵測",Toast.LENGTH_SHORT).show();
                    return;
                }


                final Button buttonStart = getActivity().findViewById(R.id.button_start);

                //如果沒在偵測，開始偵測
                if (!globalVariable.getDetecting()) {

                    //建立確認是否以此工地代號與姓名偵測的dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            globalVariable.setDetecting(true);
//                    mDatabase.removeEventListener(valueEventListener);
                            buttonStart.setTextColor(getResources().getColor(R.color.color_stop_detect));
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
                    dialog.setMessage("名稱："+sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),"")+"\n工地："+sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"")+"\n手環："+sharedPreferences.getString("macAddress","無配對")+"\n是否開始偵測？");
                    dialog.show();



                } else {
                    globalVariable.setDetecting(false);
                    buttonStart.setTextColor(getResources().getColor(R.color.colorAccent));
                    buttonStart.setText(R.string.start_detect);

                    stopService();

                    Toast.makeText(this.getContext(), R.string.stop_detect, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onClick isDetecting" + globalVariable.getDetecting());
                break;


            // 重置資料
            case R.id.button_reset:
                if(globalVariable.getDetecting()) {
                    Toast.makeText(getActivity(),"請先停止偵測",Toast.LENGTH_SHORT).show();
                    return;
                }
                //建立Alert來提醒是否要重置資料
                AlertDialog.Builder builderReset = new AlertDialog.Builder(getActivity());
                builderReset.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button

                        if(globalVariable.getDetecting()) return;

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 15, 1,
                                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3),
                                new ThreadPoolExecutor.DiscardOldestPolicy());
                        threadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                AppDatabase appDatabase = Room.databaseBuilder(getContext(),
                                        AppDatabase.class, "User").build();

                                UserDao userDao = appDatabase.getUserDao();
//                List<User> list = userDao.findPortentByState(1);
//                list.size();

                                userDao.deleteAll();
                                Log.i(TAG, "run: "+userDao.getAll().size());
//                    new QueryAsyncTask(userDao).execute(user);
                            }
                        });

                        setAccumulationText();

                        Log.d(TAG, "click ok");
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
                Toast.makeText(this.getContext(), R.string.reset_data, Toast.LENGTH_SHORT).show();
                break;
//            case R.id.button_billfragment_logout:
//                AlertDialog.Builder builderLogout = new AlertDialog.Builder(getActivity());
//                builderLogout.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        if(globalVariable.getIsDetecting()){
//                            Toast.makeText(getContext(),"請先停止偵測",Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//                        Intent intentLogout = new Intent(getActivity(), LoginActivity.class);
//                        startActivity(intentLogout);
//                        Log.d(TAG, "click ok");
//                    }
//                });
//                builderLogout.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                });
//                AlertDialog dialogLogout = builderLogout.create();
//                dialogLogout.setTitle(R.string.alert_logout);
//                dialogLogout.show();
//                break;
        }
    }

    // 驗證site跟user是否有註冊過
    private void setValueEventListener(){

        valueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshotAll = dataSnapshot;
                if(!save) return;
                boolean foundName =false,
                        foundSite= false;

                if(dataSnapshot == null){
                    Toast.makeText(getActivity(), "請確認網路狀態", Toast.LENGTH_SHORT).show();
                    return;
                }

                Iterable iterableSite = dataSnapshot.child("Site").getChildren();
                for(Object anIterable: iterableSite){
                    DataSnapshot dataSnapshotSite = (DataSnapshot) anIterable;
                    if(dataSnapshotSite.getKey().equals(editTextSite.getText().toString())){
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getResources().getString(R.string.sharePreferences_site),editTextSite.getText().toString());
                        editor.apply();
                        Site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"");
                        foundSite = true;
                        Toast.makeText(getActivity(),"已儲存工地",Toast.LENGTH_SHORT).show();
                    }
                }

                if(!foundSite) {
                    Toast.makeText(getActivity(),"請輸入正確的工地", Toast.LENGTH_SHORT).show();
                    save = false;
                    return;
                }

                Iterable iterable =  dataSnapshot.child("User").getChildren();
                for(Object anIterable: iterable){
                    DataSnapshot dataSnapshot1 = (DataSnapshot) anIterable;
                    Log.d(TAG, "user" + dataSnapshot1.getKey());
                    if(dataSnapshot1.getKey().equals(editTextName.getText().toString())){
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getResources().getString(R.string.sharePreferences_user), editTextName.getText().toString());
//                                editor.putString("site",editTextSite.getText().toString());
                        editor.apply();
                        UserId = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),"");
//                                Site = sharedPref.getString("site","");
                        foundName = true;
                        Toast.makeText(getActivity(),"已儲存名稱",Toast.LENGTH_SHORT).show();
                    }
                }

                if(!foundName){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            userDataBase.child("User").child(editTextName.getText().toString()).setValue("");
                            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getResources().getString(R.string.sharePreferences_user), editTextName.getText().toString());
                            editor.putString(getResources().getString(R.string.sharePreferences_site),editTextSite.getText().toString());
                            editor.apply();
                            UserId = sharedPref.getString(getResources().getString(R.string.sharePreferences_user),"");
                            Site = sharedPref.getString(getResources().getString(R.string.sharePreferences_site),"");

                            Log.d(TAG, "已註冊新名稱"+UserId);
                        }
                    });
                    builder.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            Log.d(TAG, "取消");
                            save=false;
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setTitle("此名稱尚未註冊，是否註冊此名稱？");
                    dialog.show();
                    Toast.makeText(getContext(), "尚未註冊的名稱", Toast.LENGTH_LONG).show();
                }

//                        mDatabase.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        userDataBase.addValueEventListener(valueEventListener);
    }

    //如果 isdetecting，則將 "偵測" 按鈕改字改色
    private void checkDetectingUI() {
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        Button buttonStart = getActivity().findViewById(R.id.button_start);
        if (globalVariable.getDetecting()) {
            buttonStart.setText(R.string.stop_detect);
            buttonStart.setTextColor(getResources().getColor(R.color.color_stop_detect));
        }
    }

    public String getCurrentTime(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "kk:mm:ss:SSS";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());

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

    /** Defines callbacks for service binding, passed to bindService() */
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

        ContextCompat.startForegroundService(this.getContext(), intent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this.getContext(), ForeService.class);
        serviceIntent.setAction(Constants.ACTION.STOP_FOREGROUND_ACTION);
        getActivity().unbindService(serviceConnection);
        getActivity().stopService(serviceIntent);
//        getActivity().startService(serviceIntent);
    }

    @Override
    public void updatePhoneChart(float x, float latitude, float longitude,float altitude) {
        if (!isBillBoardFragment) return;
//        TextView textView = getActivity().findViewById(R.id.textview_test);
//        textView.setText("x:" + x + "\nlatitude:" + latitude + "\nlongitude:" + longitude + "\naltitude" + altitude);
    }
}
