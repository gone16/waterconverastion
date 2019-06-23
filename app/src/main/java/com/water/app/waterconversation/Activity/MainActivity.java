package com.water.app.waterconversation.Activity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.water.app.waterconversation.DataBase.UserViewModel;
import com.water.app.waterconversation.Fragment.BillboardFragment;
import com.water.app.waterconversation.Fragment.SettingFragment;
import com.water.app.waterconversation.R;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity  {

//    private static final String TAG = "MainActivity";
    private String TAG = this.getClass().getSimpleName();
    private UserViewModel userViewModel;
    private ArrayList<String> permissions = new ArrayList<>();  //permissions array
    private ArrayList<String> permissionsToRequest;
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    public static String UserId = "";
    public static String DeviceId;
    public static String Site = "";

    public static boolean OpenAccidentAlarm= false;
    public static boolean OpenPortentAlarm= false;

    public static float sensitivityDrop = 0.0f;
    public static float sensitivityFall = 0.0f;
    public static float sensitivityComa = 0.0f;
    public static float sensitivityLostBalance = 0.0f;
    public static float sensitivityHeavyStep = 0.0f;
    public static float sensitivitySuddenlyWobbing = 0.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId("1:1082505655377:android:e909de7443911a15") // Required for Analytics.
                .setApiKey("AIzaSyBi0fZ6BHlSuI-rE9DYhGx4VYCiN_slbvs") // Required for Auth.
                .setDatabaseUrl("https://water-user-ead4c.firebaseio.com") // Required for RTDB.
                .build();

        // Initialize with secondary app.
        FirebaseApp.initializeApp(this);
        FirebaseApp.initializeApp(Objects.requireNonNull(this), options, "water-user");

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.navigation_billboard:
                        replaceFragment(new BillboardFragment());
                        return true;
//                    case R.id.navigation_location:
//                        replaceFragment(new LocationFragment());
//                        return true;
//                    case R.id.navigation_parameter:
//                        replaceFragment(new ParameterFragment());
//                        return true;
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

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }


        int seekbar_ids[] = {R.id.seekBar_setting_drop,R.id.seekBar_setting_fall,R.id.seekBar_setting_coma,
                R.id.seekBar_setting_lost_balance,R.id.seekBar_setting_heavy_step,R.id.seekBar_setting_suddenly_wobbing};

        Log.d(TAG, "setUI: seekbar");
        // 建立 switch listener
        SeekBar seekBar = null;
        for (int i = 0; i < seekbar_ids.length; i++) {
//            if ((seekBar = findViewById(seekbar_ids[i])) != null) {
                int firstValue = getFirstValue(seekbar_ids[i]);
                Log.d(TAG, "seekbar value: "+ firstValue);
                setupSharedPreferences(seekbar_ids[i], firstValue);
//                seekBar.setProgress(firstValue);
//            }
        }

//        setupSharedPreferences();

        //設立第一個頁面為BillFragment
        replaceFragment(new BillboardFragment());
    }


    private int getFirstValue(int id){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int progress = sharedPreferences.getInt(String.valueOf(id),50);
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
    private void replaceFragment(Fragment fragment){
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
                Log.d(TAG, "setupSP coma: "+sensitivityComa);
                break;
            case R.id.seekBar_setting_lost_balance:
                sensitivityLostBalance = getSensitivity(progress);
                break;
            case R.id.seekBar_setting_heavy_step:
                sensitivityHeavyStep = getSensitivity(progress);
                break;
            case R.id.seekBar_setting_suddenly_wobbing:
                sensitivitySuddenlyWobbing = getSensitivity(progress);
                break;

//        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//        sensitivityDrop = sharedPreferences.getFloat("drop",0.0f);
//        sensitivityFall = sharedPreferences.getFloat("fall",0.0f);
//        sensitivityComa = sharedPreferences.getFloat("coma",0.0f);
//        sensitivityLostBalance = sharedPreferences.getFloat("lostbalance",0.0f);
//        sensitivityHeavyStep = sharedPreferences.getFloat("heavystep",0.0f);
//        sensitivitySuddenlyWobbing = sharedPreferences.getFloat("suddenlywobbing",0.0f);
        }
    }

    //敏感度計算
    private float getSensitivity(int progress){
        float sensitivity = 0.0f;
        sensitivity = (progress-50)/20f;
        Log.d(TAG, "getSensitivity: " + sensitivity);
        return sensitivity;
    }

    private float getComaSensitivity(int progress){
        float sensitivity = 0.0f;
        if(progress>50) sensitivity = ((progress-50)/200f);
        else sensitivity = -((50-progress)/200f);
        return sensitivity;
    }



    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}