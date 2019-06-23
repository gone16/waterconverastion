package com.water.app.waterconversation.Fragment;

import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

import static com.water.app.waterconversation.Activity.MainActivity.Site;
import static com.water.app.waterconversation.Activity.MainActivity.UserId;
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

        int button_ids[] = {R.id.button_settingfragment_save};

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

        editTextName = getActivity().findViewById(R.id.editText_settingfragment_name);
        editTextSite = getActivity().findViewById(R.id.editText_settingfragment_site);
        Button buttonSave = getActivity().findViewById(R.id.button_settingfragment_save);

        //取得資料庫中的userid以及site
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        editTextName.setText(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_user),""));
        editTextSite.setText(sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site),""));



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
            case R.id.button_settingfragment_save:
                GlobalVariable globalVariable = (GlobalVariable)getActivity().getApplicationContext();

                if(globalVariable.getIsDetecting()){
                    Toast.makeText(getActivity(),"請先暫停偵測再編輯名稱",Toast.LENGTH_SHORT).show();
                    return;
                }
                save = true;
                valueEventListener.onDataChange(dataSnapshotAll);
        }
    }

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
}
