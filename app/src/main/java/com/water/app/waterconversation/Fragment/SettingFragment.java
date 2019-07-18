package com.water.app.waterconversation.Fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.golife.customizeclass.CareMeasureHR;
import com.golife.customizeclass.SetCareSetting;
import com.golife.database.table.TablePulseRecord;
import com.golife.database.table.TableSleepRecord;
import com.golife.database.table.TableSpO2Record;
import com.golife.database.table.TableStepRecord;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.goyourlife.gofitsdk.GoFITSdk;
import com.water.app.waterconversation.Activity.BluetoothDeviceListActivity;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

import java.util.ArrayList;

import static com.water.app.waterconversation.Activity.MainActivity._goFITSdk;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityComa;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityDrop;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityFall;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityHeavyStep;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityLostBalance;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivitySuddenlyWobbing;

public class SettingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private String TAG = this.getClass().getSimpleName();

    private DatabaseReference userDataBase;
    private ValueEventListener valueEventListener;
    private DataSnapshot dataSnapshotAll;
    private boolean save = false;
    EditText editTextName, editTextSite;

    private String mMacAddress = null;
    private String mPairingCode = null;
    private String mPairingTime = null;
    private String mProductID = null;
    private String mDeviceName = null;
//    private String UserId = null;

    SetCareSetting mCareSettings;



    int i = 0;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(getActivity());

        // 取得user database 資料
        FirebaseApp water_user = FirebaseApp.getInstance("water-user");
        userDataBase = FirebaseDatabase.getInstance(water_user).getReference();
        setValueEventListener();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int button_ids[] = {R.id.button_settingfragment_save, R.id.button_setting_pair_wristband, R.id.button_setting_comfirm_wristband,
                R.id.button_setting_disconnect};

        int seekbar_ids[] = {R.id.seekBar_setting_drop,R.id.seekBar_setting_fall,R.id.seekBar_setting_coma,
                R.id.seekBar_setting_lost_balance,R.id.seekBar_setting_heavy_step,R.id.seekBar_setting_suddenly_wobbing};

        // 建立 switch listener
        SeekBar seekBar = null;
        for (int i = 0; i < seekbar_ids.length; i++) {
            if ((seekBar = getActivity().findViewById(seekbar_ids[i])) != null) {
                seekBar.setOnSeekBarChangeListener(this);
                int firstValue = getFirstValue(seekBar.getId());
                Log.d(TAG, "onViewCreated: "+ firstValue);
                seekBar.setProgress(firstValue);
            }
        }

        // 建立 button click listener
        Button button = null;
        for (int i=0; i<button_ids.length; i++){
            if((button = getActivity().findViewById(button_ids[i])) != null){
                button.setOnClickListener(this);
            }
        }
//
//        Button button1 = getActivity().findViewById(R.id.button);
//        button1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                disconnectGolife();

//                syncGolife();
//                demoSettingHRTimingMeasure("on,00:00,23:59,1");
//                if (_goFITSdk != null) {
//                    Log.i(TAG, "demo_function_setting");
//
////                    Preference pPref = (Preference) findPreference("demo_function_setting");
////                    pPref.setSummary("");
//
//                    if (mCareSettings == null) {
//                        mCareSettings = _goFITSdk.getNewCareSettings();
//                    }
//                    String instructions = "format : [on/off], [HH:mm(startTime)], [HH:mm(endTime)], [IntervalMin]\ne.g : on,00:00,23:59,15";
//                    displaySettingDetail(instructions, SettingItem.HR_TIMING_MEASURE);
//                }
//                else {
//                    showToast("SDK Instance invalid, needs `SDK init`");
//                }
//            }

//        });

        editTextName = getActivity().findViewById(R.id.editText_settingfragment_name);
        editTextSite = getActivity().findViewById(R.id.editText_settingfragment_site);

        //取得資料庫中的userid以及site
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        editTextName.setText(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),""));
        editTextSite.setText(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),""));

//        connectGolife();
        setPairedIDGolife();


    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//        handler.removeCallbacks();
    }

    // seekbar變化時，進入此方法
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()){
            case R.id.seekBar_setting_drop:
                sensitivityDrop = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_drop,progress);
                break;
            case R.id.seekBar_setting_fall:
                sensitivityFall = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_fall,progress);
                break;
            case R.id.seekBar_setting_coma:
                sensitivityComa =getComaSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_coma,progress);
                break;
            case R.id.seekBar_setting_lost_balance:
                sensitivityLostBalance = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_lost_balance,progress);
                break;
            case R.id.seekBar_setting_heavy_step:
                sensitivityHeavyStep = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_heavy_step,progress);
                break;
            case R.id.seekBar_setting_suddenly_wobbing:
                sensitivitySuddenlyWobbing = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_suddenly_wobbing,progress);
                break;

        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    //取得seekbar的初始值
    private int getFirstValue(int id){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        int progress = sharedPreferences.getInt(String.valueOf(id),50);
        return progress;
    }

    //儲存seekbar的值，以供下次進入程式時可以用上次的值
    private void saveSeekbarProgress(int id,int progress){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(String.valueOf(id),progress);
        Log.d(TAG, "saveSeekbarProgress: "+ id);
        editor.apply();
    }

    //敏感度計算
    private float getSensitivity(int progress){
        float sensitivity = 0.0f;
        sensitivity = (progress-50)/20f;
        Log.d(TAG, "getSensitivity: " + sensitivity);
        return sensitivity;
    }

    //昏迷敏感度計算
    private float getComaSensitivity(int progress){
        float sensitivity = 0.0f;
        if(progress>50) sensitivity = ((progress-50)/200f);
        else sensitivity = -((50-progress)/200f);
        return sensitivity;
    }


    // botton 按鈕事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){

            // 儲存按鈕
            case R.id.button_settingfragment_save:
                GlobalVariable globalVariable = (GlobalVariable)getActivity().getApplicationContext();

                if(globalVariable.getDetecting()){
                    Toast.makeText(getActivity(),"請先暫停偵測再編輯名稱",Toast.LENGTH_SHORT).show();
                    return;
                }
                save = true;
                valueEventListener.onDataChange(dataSnapshotAll);
                break;

            // 配對手環按鈕
            case R.id.button_setting_pair_wristband:
                    Toast.makeText(getActivity(),"bluetooth device is open.",Toast.LENGTH_SHORT).show();
                    Intent serverIntent = new Intent(getActivity(), BluetoothDeviceListActivity.class);
                    startActivityForResult(serverIntent, 1);
                break;

            // 尋找手環
            case R.id.button_setting_comfirm_wristband:
                findGolife();
                break;

            // 斷開連線
            case R.id.button_setting_disconnect:
                disconnectGolife();
                break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        String result = data.getExtras().getString("result");//得到新Activity 关闭后返回的数据
//        Log.i(TAG, result);

//        connectGolife();
        setPairedIDGolife();



    }

    // Listening from firebase to get user name and site name.
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
//                        Site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"");
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
//                        UserId = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),"");
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
                            String UserId = sharedPref.getString(getResources().getString(R.string.sharePreferences_user),"");
//                            Site = sharedPref.getString(getResources().getString(R.string.sharePreferences_site),"");

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

    // 設定配對狀態的文字
    private void setPairedIDGolife(){
        TextView textView = getActivity().findViewById(R.id.textview_setting_wristband_connect_state);
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_connect");

            // Demo - get connect information from local storage
            if (mMacAddress == null || mPairingCode == null || mPairingTime == null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor pe = sp.edit();
                mMacAddress = sp.getString("macAddress", "");
                mPairingCode = sp.getString("pairCode", "");
                mPairingTime = sp.getString("pairTime", "");
                mProductID = sp.getString("productID", "");
                mDeviceName = sp.getString("deviceName","");
                pe.apply();
            }
            try{
                textView.setText(getResources().getString(R.string.setting_already_paired)+mDeviceName+"\n"+mMacAddress);
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }else{
            textView.setText(getResources().getString(R.string.setting_no_paired));
        }
    }

    private void findGolife(){
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_connect");

            // Demo - get connect information from local storage
            if (mMacAddress == null || mPairingCode == null || mPairingTime == null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor pe = sp.edit();
                mMacAddress = sp.getString("macAddress", "");
                mPairingCode = sp.getString("pairCode", "");
                mPairingTime = sp.getString("pairTime", "");
                mProductID = sp.getString("productID", "");
                pe.apply();
            }

            showToast("正在尋找手環...");

            // Demo - doConnectDevice API
            _goFITSdk.doConnectDevice(mMacAddress, mPairingCode, mPairingTime, mProductID, new GoFITSdk.GenericCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "doConnectDevice() : onSuccess()");
                    showToast("連線成功 ");

//                    Preference pPref = (Preference) findPreference("demo_connect_status");
                    // Demo - isBLEConnect API
                    boolean isConnect = _goFITSdk.isBLEConnect();
                    String summary = isConnect ? "已連接：" : "未連接：";
//                    pPref.setSummary(summary);
//
//                    pPref = (Preference) findPreference("demo_function_connect");
//                    pPref.setSummary("Connected : " + mMacAddress);
                    try{
                        TextView textView = getActivity().findViewById(R.id.textview_setting_wristband_connect_state);
                        textView.setText(summary+mMacAddress);
                    }catch (Exception e){
                        Log.e(TAG, e.toString());
                    }


                    Log.i(TAG, "demo_function_find_my_care");

                    // Demo - doFindMyCare API
                    _goFITSdk.doFindMyCare(3);

                    // Demo - setRemoteCameraHandler API
//                    demoSetRemoteCameraHandler();
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

    // 連線Golife 手環
    void connectGolife(){
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_connect");

            // Demo - get connect information from local storage
            if (mMacAddress == null || mPairingCode == null || mPairingTime == null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
//                    pPref.setSummary(summary);
//
//                    pPref = (Preference) findPreference("demo_function_connect");
//                    pPref.setSummary("Connected : " + mMacAddress);
                    try{
                        TextView textView = getActivity().findViewById(R.id.textview_setting_wristband_connect_state);
                        textView.setText(summary+mMacAddress);
                    }catch (Exception e){
                        Log.e(TAG, e.toString());
                    }


                    demoSettingHRTimingMeasure("on,00:00,23:59,1");
                    syncGolife();

                    // Demo - setRemoteCameraHandler API
//                    demoSetRemoteCameraHandler();
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

    // 與Golife 手環同步
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

    //與Golife手環段開連線
    private void disconnectGolife(){

        GlobalVariable globalVariable  = (GlobalVariable) getActivity().getApplicationContext();


        if(globalVariable.getDetecting()){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (_goFITSdk != null) {

                        try{
                            _goFITSdk.doDisconnectDevice();
                            Log.i(TAG, "demo_function_disconnect");
                            showToast("斷開連線");
                            TextView textView = getActivity().findViewById(R.id.textview_setting_wristband_connect_state);
                            textView.setText("已斷開連線："+mMacAddress);
                        }catch (Exception e){
                            Log.e(TAG, e.toString());
                        }

                    }
                    else {
                        showToast("SDK Instance invalid, needs `SDK init`");
                    }
                }
            });
            builder.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
            AlertDialog dialog = builder.create();
            dialog.setTitle("裝置偵測中，確定要中斷與手環的連線?");
            dialog.show();
        }
        else {
            if (_goFITSdk != null) {

                try {
                    _goFITSdk.doDisconnectDevice();
                    Log.i(TAG, "demo_function_disconnect");
                    showToast("斷開連線");
                    TextView textView = getActivity().findViewById(R.id.textview_setting_wristband_connect_state);
                    textView.setText("已斷開連線：" + mMacAddress);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }

            } else {
                showToast("SDK Instance invalid, needs `SDK init`");
            }
        }
    }

    // 設定心率頻率("開關，開始時間，結束時間，取樣頻率"，ex:"on,00:00,23:59,1")
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
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }
}
