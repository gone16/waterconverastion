package com.water.app.waterconversation.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.water.app.waterconversation.DataBase.UserViewModel;
import com.water.app.waterconversation.Fragment.BillboardFragment;
import com.water.app.waterconversation.Fragment.LocationFragment;
import com.water.app.waterconversation.Fragment.MachineFragment;
import com.water.app.waterconversation.Fragment.ReportFragment;
import com.water.app.waterconversation.Fragment.SettingFragment;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    //    private static final String TAG = "MainActivity";
    private String TAG = this.getClass().getSimpleName();
    private UserViewModel userViewModel;
    private ArrayList<String> permissions = new ArrayList<>();  //permissions array
    private ArrayList<String> permissionsToRequest;
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    public static String DeviceId;

    //public static boolean OpenAccidentAlarm = false;
    public static boolean OpenPortentAlarm = false;

    // 初始設定
    public static float sensitivityDrop = 0.0f;
    public static float sensitivityFall = 0.0f;
    public static float sensitivityComa = 0.0f;
    public static float sensitivityPortent = 0.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        GlobalVariable gv = (GlobalVariable)getApplicationContext();
                        String token = task.getResult().getToken();
                        gv.setToken(token);
                        // Log and show
                        Log.d(TAG,"token"+token);
                    }
                });
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:1082505655377:android:e909de7443911a15") // Required for Analytics.
                .setApiKey("AIzaSyBi0fZ6BHlSuI-rE9DYhGx4VYCiN_slbvs") // Required for Auth.
                .setDatabaseUrl("https://water-user-ead4c.firebaseio.com") // Required for RTDB.
                .build();

        GlobalVariable globalVariable = (GlobalVariable) this.getApplicationContext();

        if (isNetworkConnected()) {
            // Initialize with secondary app.
            if (!globalVariable.getFirebaseSet()) {
                FirebaseApp.initializeApp(this);
                FirebaseApp.initializeApp(Objects.requireNonNull(this), options, "water-user");
                globalVariable.setFirebaseSet(true);
            }
        } else {
            Toast.makeText(MainActivity.this, "請打開網路", Toast.LENGTH_LONG).show();
        }
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.navigation_billboard:
                        replaceFragment(new BillboardFragment());
                        return true;
                    case R.id.navigation_location:
                        replaceFragment(new LocationFragment());
                        return true;
                    case R.id.navigation_machine:
                        replaceFragment(new MachineFragment());
                        return true;
                    case R.id.navigation_report:
                        replaceFragment(new ReportFragment());
                        return true;
                    case R.id.navigation_setting:
                        replaceFragment(new SettingFragment());
                        return true;
                }
                return false;
            }
        });
        // 在android 8 以上，需要在使用權限前再度確認權限
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.BLUETOOTH);
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        permissions.add(Manifest.permission.CAMERA);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = "default_notification_channel_id";
            String channelName = "default_notification_channel_name";
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }


        int seekbar_ids[] = {R.id.seekBar_setting_drop, R.id.seekBar_setting_fall, R.id.seekBar_setting_coma,
                R.id.seekBar_setting_portent};

        Log.d(TAG, "setUI: seekbar");
        // 建立 switch listener
        SeekBar seekBar = null;
        for (int i = 0; i < seekbar_ids.length; i++) {
//            if ((seekBar = findViewById(seekbar_ids[i])) != null) {
            int firstValue = getFirstValue(seekbar_ids[i]);
            Log.d(TAG, "seekbar value: " + firstValue);
            setupSharedPreferences(seekbar_ids[i], firstValue);
//                seekBar.setProgress(firstValue);
//            }
        }

//        setupSharedPreferences();

        //設立第一個頁面為BillFragment
        replaceFragment(new BillboardFragment());

        IntentFilter filter = new IntentFilter("danger");
        this.registerReceiver(mDangerReceiver, filter);

        IntentFilter filter1 = new IntentFilter("machine");
        this.registerReceiver(mMachineReceiver, filter1);

        IntentFilter filter2 = new IntentFilter("moperater");
        this.registerReceiver(mDangerOperate, filter2);

        if (globalVariable.getOperateEnable()==true){
            BottomNavigationView bnv = findViewById(R.id.navigation);
            bnv.setSelectedItemId(R.id.navigation_machine);
        }
    }


    private int getFirstValue(int id) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int progress = sharedPreferences.getInt(String.valueOf(id), 50);
        return progress;
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }


    //更換Fragment用
    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.ContentLayout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void setupSharedPreferences(int id, int progress) {

        switch (id) {
            case R.id.seekBar_setting_drop:
                sensitivityDrop = getSensitivity(progress);
                break;
            case R.id.seekBar_setting_fall:
                sensitivityFall = getSensitivity(progress);
                break;
            case R.id.seekBar_setting_coma:
                sensitivityComa = getComaSensitivity(progress);
                Log.d(TAG, "setupSP coma: " + sensitivityComa);
                break;
            case R.id.seekBar_setting_portent:
                sensitivityPortent = getSensitivity(progress);
                break;
        }
    }

    //敏感度計算
    private float getSensitivity(int progress) {
        float sensitivity = 0.0f;
        sensitivity = (progress - 50) / 20f;
        Log.d(TAG, "getSensitivity: " + sensitivity);
        return sensitivity;
    }

    private float getComaSensitivity(int progress) {
        float sensitivity = 0.0f;
        if (progress > 50) sensitivity = ((progress - 50) / 200f);
        else sensitivity = -((50 - progress) / 200f);
        return sensitivity;
    }

    void showToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver mDangerOperate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent Intent) {
            final String status = Intent.getStringExtra("status");
            AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
            mDialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            mDialog.setTitle("注意");
            mDialog.setMessage( status + "，請注意周圍");
            mDialog.show();
        }
    };

    private BroadcastReceiver mDangerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent Intent) {
            final String status = Intent.getStringExtra("status");
            AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
            mDialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            mDialog.setTitle("危險");
            mDialog.setMessage( status + "，請小心");
            mDialog.show();
        }
    };

    private BroadcastReceiver mMachineReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent Intent) {
            final String machine = Intent.getStringExtra("machine");
            AlertDialog.Builder mDialog = new AlertDialog.Builder(MainActivity.this);
            mDialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    BottomNavigationView btn = findViewById(R.id.navigation);
                    btn.setSelectedItemId(R.id.navigation_machine);
                }
            });
            mDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    GlobalVariable gv = (GlobalVariable) getApplicationContext();
                    gv.setMachineName("");
                }
            });
            mDialog.setTitle("機具操作");
            mDialog.setMessage("已進入" + machine + "範圍，點選操作機具");
            mDialog.show();
        }
    };


    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}