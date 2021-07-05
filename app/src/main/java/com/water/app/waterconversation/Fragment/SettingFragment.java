package com.water.app.waterconversation.Fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.water.app.waterconversation.Activity.MainActivity.OpenPortentAlarm;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityComa;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityDrop;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityFall;
import static com.water.app.waterconversation.Activity.MainActivity.sensitivityPortent;

public class SettingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {


    public static boolean isSettingFragment = true;
    private String TAG = this.getClass().getSimpleName();

    private DatabaseReference userDataBase;
    private ValueEventListener valueEventListener;
    private DataSnapshot dataSnapshotAll;
    private boolean save = false;
    EditText editTextName, editTextSite, editTextNumber;
    private TextView mPercentText;
    private String BindId, verificationCodeBySystem;
    private Button bBind;


    // 設定敏感度的基底
    private float mShiftBase = 10f;        // 可以+ -的值 = (progress-50)/mShiftBase             //progress : 0 ~ 100   //16.5 = 可以 + - 3    //應根據ForeService 的Threshold_Drop...等進行調整
    private float mShiftBaseComa = 100;     // 可以+ -的值 = (progress-50)/mShiftBaseComa         //progress : 0 ~ 100   //165 = 可以 + - 0.3


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
        if (isNetworkConnected() == true) {
            FirebaseApp water_user = FirebaseApp.getInstance("water-user");
            userDataBase = FirebaseDatabase.getInstance(water_user).getReference();
            setValueEventListener();
        }else {
            Toast.makeText(getActivity(), "請打開網路", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int switch_ids[] = {R.id.switch_accident, R.id.switch_precursor, R.id.switch_location, R.id.switch_accident_alarm, R.id.switch_portent_alarm};
        // 建立 switch listener
        Switch mSwitch = null;
        for (int i = 0; i < switch_ids.length; i++) {
            if ((mSwitch = getActivity().findViewById(switch_ids[i])) != null) {
                mSwitch.setOnCheckedChangeListener(this);
                boolean state = getFirstSwitchValue(mSwitch.getId());
                mSwitch.setChecked(state);
            }
        }

        //百分比標示
        mPercentText = getActivity().findViewById(R.id.text_setting_percent_value);

        int seekbar_ids[] = {R.id.seekBar_setting_drop, R.id.seekBar_setting_fall, R.id.seekBar_setting_coma,
                R.id.seekBar_setting_portent};

        // 建立 switch listener
        SeekBar seekBar = null;
        for (int i = 0; i < seekbar_ids.length; i++) {
            if ((seekBar = getActivity().findViewById(seekbar_ids[i])) != null) {
                seekBar.setOnSeekBarChangeListener(this);
                int firstValue = getFirstValue(seekBar.getId());
                Log.d(TAG, "onViewCreated: " + firstValue);
                seekBar.setProgress(firstValue);
            }
        }

        int button_id[] = {R.id.button_settingfragment_save, R.id.button_bind};

        // 建立 button click listener
        Button button = null;
        for (int i = 0; i < button_id.length; i++) {
            if ((button = getActivity().findViewById(button_id[i])) != null) {
                button.setOnClickListener(this);
            }
        }
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        editTextName = getActivity().findViewById(R.id.editText_settingfragment_name);
        editTextSite = getActivity().findViewById(R.id.editText_settingfragment_site);
        editTextNumber = getActivity().findViewById(R.id.editText_settingfragment_number);
        if (gv.getFbind() == false) {
            editTextName.setFocusable(true);
        } else {
            editTextName.setFocusable(false);
        }
        bBind = getActivity().findViewById(R.id.button_bind);
        if (gv.getFbind() == true) {
            bBind.setText("已綁定");
        } else {
            bBind.setText("未綁定");
        }

        //取得資料庫中的userid以及site
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        editTextName.setText(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user), ""));
        editTextSite.setText(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), ""));
        editTextNumber.setText(sharedPreferences.getString("BindNumber",""));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        handler.removeCallbacks();
        isSettingFragment = false;
    }

    // seekbar變化時，進入此方法
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekBar_setting_drop:
                sensitivityDrop = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_drop, progress);
                setPercentText(getResources().getString(R.string.text_setting_sensitivity_drop), progress);
                break;
            case R.id.seekBar_setting_fall:
                sensitivityFall = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_fall, progress);
                setPercentText(getResources().getString(R.string.text_setting_sensitivity_fall), progress);
                break;
            case R.id.seekBar_setting_coma:
                sensitivityComa = getComaSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_coma, progress);
                setPercentText(getResources().getString(R.string.text_setting_sensitivity_coma), progress);
                break;
            case R.id.seekBar_setting_portent:
                sensitivityPortent = getSensitivity(progress);
                saveSeekbarProgress(R.id.seekBar_setting_portent, progress);
                setPercentText(getResources().getString(R.string.text_setting_sensitivity_lost_balance), progress);
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
    private int getFirstValue(int id) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        int progress = sharedPreferences.getInt(String.valueOf(id), 50);
        return progress;
    }

    //儲存seekbar的值，以供下次進入程式時可以用上次的值
    private void saveSeekbarProgress(int id, int progress) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(String.valueOf(id), progress);
        Log.d(TAG, "saveSeekbarProgress: " + id);
        editor.apply();
    }

    //敏感度計算
    private float getSensitivity(int progress) {
        float sensitivity = 0.0f;
        sensitivity = -(progress - 50) / mShiftBase;
        Log.d(TAG, "getSensitivity: " + sensitivity);
        return sensitivity;
    }

    //昏迷敏感度計算
    private float getComaSensitivity(int progress) {
        float sensitivity = 0.0f;
        if (progress > 50) sensitivity = -((progress - 50) / mShiftBaseComa);
        else sensitivity = ((50 - progress) / mShiftBaseComa);
        Log.d(TAG, "getSensitivity: " + sensitivity);
        return sensitivity;
    }


    // botton 按鈕事件
    @Override
    public void onClick(View v) {
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        switch (v.getId()) {

            // 儲存按鈕
            case R.id.button_settingfragment_save:

                if (gv.getDetecting()) {
                    Toast.makeText(getActivity(), "請先暫停偵測再編輯名稱", Toast.LENGTH_SHORT).show();
                    return;
                }
                save = true;
                valueEventListener.onDataChange(dataSnapshotAll);
                break;
            case R.id.button_bind:
                if (save = false) {
                    Toast.makeText(getActivity(), "請先按下儲存再進行綁定", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    valueEventListener.onDataChange(dataSnapshotAll);
                    if (gv.getFbind() == false) {
                        sendVerificationCodetoUser(editTextNumber.getText().toString());
                        final EditText editverify = new EditText(getContext());
                        editverify.setInputType(InputType.TYPE_CLASS_NUMBER);
                        new AlertDialog.Builder(getActivity())
                                .setTitle("請輸入驗證碼進行綁定")
                                .setView(editverify)
                                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        String code = editverify.getText().toString();
                                        verifyCode(code);
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .show();
                    } else {
                        Toast.makeText(getActivity(), "解除綁定?", Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    private void sendVerificationCodetoUser(String editnumber) {
        Log.d(TAG, "綁定:" + "+886" + editTextNumber.getText().toString());
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+886" + editTextNumber.getText().toString(),
                60,
                TimeUnit.SECONDS,
                getActivity(),
                mCallbacks);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onCodeSent(@androidx.annotation.NonNull String s, @androidx.annotation.NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            Log.d(TAG,"綁定:"+"驗證");
            super.onCodeSent(s, forceResendingToken);
            verificationCodeBySystem = s;
        }

        @Override
            public void onVerificationCompleted(@androidx.annotation.NonNull PhoneAuthCredential phoneAuthCredential) {
            }

            @Override
            public void onVerificationFailed(@androidx.annotation.NonNull FirebaseException e) {
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
    };

    private void verifyCode(String codeByUSer){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCodeBySystem,codeByUSer);
        checkUserByCredentials(credential);
    }
    private void checkUserByCredentials(PhoneAuthCredential credential){
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@androidx.annotation.NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            savebindinfo();
                        }else{
                            Toast.makeText(getActivity(),"綁定:錯誤", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }




    private void savebindinfo() {
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        bBind.setText("已綁定");
        gv.setFbind(true);
        gv.setBind(true);
        save = true;
        valueEventListener.onDataChange(dataSnapshotAll);
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("BindNumber", editTextNumber.getText().toString());
        editor.apply();
        Map<String, Object> m = new HashMap<>();
        m.put("phonenumber",editTextNumber.getText().toString());
        m.put("user",editTextName.getText().toString());
        String filename = editTextName.getText().toString()+editTextNumber.getText().toString();
        fs.collection("CTC_Bind").document(filename).set(m, SetOptions.merge());
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    // Listening from firebase to get user name and site name.
    private void setValueEventListener() {

        valueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshotAll = dataSnapshot;
                if (!save) return;
                boolean foundName = false,
                        foundSite = false;

                if (dataSnapshot == null) {
                    Toast.makeText(getActivity(), "請確認網路狀態", Toast.LENGTH_SHORT).show();
                    return;
                }

                Iterable iterableSite = dataSnapshot.child("Site").getChildren();
                for (Object anIterable : iterableSite) {
                    DataSnapshot dataSnapshotSite = (DataSnapshot) anIterable;
                    if (dataSnapshotSite.getKey().equals(editTextSite.getText().toString())) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getResources().getString(R.string.sharePreferences_site), editTextSite.getText().toString());
                        editor.apply();
//                        Site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),"");
                        foundSite = true;
                        Toast.makeText(getActivity(), "已儲存工地", Toast.LENGTH_SHORT).show();
                    }
                }

                if (!foundSite) {
                    Toast.makeText(getActivity(), "請輸入正確的工地", Toast.LENGTH_SHORT).show();
                    save = false;
                    return;
                }

                Iterable iterable = dataSnapshot.child("User").getChildren();
                for (Object anIterable : iterable) {
                    DataSnapshot dataSnapshot1 = (DataSnapshot) anIterable;
                    Log.d(TAG, "user" + dataSnapshot1.getKey());
                    if (dataSnapshot1.getKey().equals(editTextName.getText().toString())) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getResources().getString(R.string.sharePreferences_user), editTextName.getText().toString());
                        editor.apply();
                        foundName = true;
                        Toast.makeText(getActivity(), "已儲存名稱", Toast.LENGTH_SHORT).show();
                    }
                }

                if (!foundName) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            userDataBase.child("User").child(editTextName.getText().toString()).setValue("");
                            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(getResources().getString(R.string.sharePreferences_user), editTextName.getText().toString());
                            editor.putString(getResources().getString(R.string.sharePreferences_site), editTextSite.getText().toString());
                            editor.apply();
                            String UserId = sharedPref.getString(getResources().getString(R.string.sharePreferences_user), "");

                            Log.d(TAG, "已註冊新名稱" + UserId);
                        }
                    });
                    builder.setNegativeButton(R.string.alert_reset_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            Log.d(TAG, "取消");
                            save = false;
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
    private void setPercentText(String title, float percent) {
        String show = title + ":" + percent + "%";
        mPercentText.setText(show);
    }

    //switch變化，改變全域變數
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        GlobalVariable globalVariable = (GlobalVariable) getActivity().getApplicationContext();
        switch (buttonView.getId()) {
            case R.id.switch_accident:
                if (isChecked) {
                    globalVariable.setOpen_accident(true);
                    saveSwitchState(R.id.switch_accident, true);
                    Toast.makeText(getContext(), R.string.open_accident, Toast.LENGTH_SHORT).show();
                } else {
                    globalVariable.setOpen_accident(false);
                    saveSwitchState(R.id.switch_accident, false);
                    Toast.makeText(getContext(), R.string.close_accident, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onCheckedChanged accident: " + globalVariable.getOpen_accident());
                break;
            case R.id.switch_precursor:
                if (isChecked) {
                    globalVariable.setOpen_precursor(true);
                    saveSwitchState(R.id.switch_precursor, true);
                    Toast.makeText(getContext(), R.string.open_precursor, Toast.LENGTH_SHORT).show();
                } else {
                    globalVariable.setOpen_precursor(false);
                    saveSwitchState(R.id.switch_precursor, false);
                    Toast.makeText(getContext(), R.string.close_precursor, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onCheckedChanged precursor:" + globalVariable.getOpen_precursor());
                break;
            case R.id.switch_location:
                if (isChecked) {
                    globalVariable.setOpen_location(true);
                    saveSwitchState(R.id.switch_location, true);
                    Toast.makeText(getContext(), R.string.open_location, Toast.LENGTH_SHORT).show();
                } else {
                    globalVariable.setOpen_location(false);
                    saveSwitchState(R.id.switch_location, false);
                    Toast.makeText(getContext(), R.string.close_location, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onCheckedChanged location:" + globalVariable.getOpen_location());
                break;
            case R.id.switch_accident_alarm:
                if (isChecked) {
                    globalVariable.setOpen_accident_alarm(true);
                    saveSwitchState(R.id.switch_accident_alarm, true);
                    Toast.makeText(getContext(), R.string.open_accident_alarm, Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getContext(), R.string.close_accident_alarm, Toast.LENGTH_SHORT).show();
                    globalVariable.setOpen_accident_alarm(false);
                    saveSwitchState(R.id.switch_accident_alarm, false);
                }
                break;
            case R.id.switch_portent_alarm:
                if (isChecked) {
                    OpenPortentAlarm = true;
                    saveSwitchState(R.id.switch_portent_alarm, true);
                    Toast.makeText(getContext(), R.string.open_portent_alarm, Toast.LENGTH_SHORT).show();
                } else {
                    OpenPortentAlarm = false;
                    saveSwitchState(R.id.switch_portent_alarm, false);
                    Toast.makeText(getContext(), R.string.close_Portent_alarm, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void saveSwitchState(int id, boolean state) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(String.valueOf(id), state);
        Log.d(TAG, "saveSeekbarProgress: " + id);
        editor.apply();
    }

    private boolean getFirstSwitchValue(int id) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean state = sharedPreferences.getBoolean(String.valueOf(id), false);
        return state;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null & cm.getActiveNetworkInfo().isConnected();
    }

}

