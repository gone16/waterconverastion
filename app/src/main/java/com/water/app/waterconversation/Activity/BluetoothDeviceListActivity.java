package com.water.app.waterconversation.Activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.golife.contract.AppContract;
import com.golife.customizeclass.ScanBluetoothDevice;
import com.goyourlife.gofitsdk.GoFITSdk;
import com.water.app.waterconversation.CameraClass;
import com.water.app.waterconversation.R;

import java.util.ArrayList;

import static com.water.app.waterconversation.Activity.MainActivity._goFITSdk;

public class BluetoothDeviceListActivity extends Activity {

    private final String TAG = getClass().getSimpleName();
    private ScanBluetoothDevice mSelectDevice = null;
    private String mMacAddress = null;
    private String mPairingCode = null;
    private String mPairingTime = null;
    private String mProductID = null;
    private boolean mPaired = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bluetooth_devices_list);

        CameraClass.checkPermission(AppContract.PermissionType.storage, this);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor pe = sp.edit();
        mMacAddress = sp.getString("macAddress", "");
        mPairingCode = sp.getString("pairCode", "");
        mPairingTime = sp.getString("pairTime", "");
        mProductID = sp.getString("productID", "");
        pe.apply();

        if(mMacAddress != null){
            mPaired = true;
            ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(BluetoothDeviceListActivity.this,R.layout.device_name);
            pairedListView.setAdapter(adapter);
            pairedListView.setOnItemClickListener(mDeviceClickListener);
            adapter.add("已配對裝置："+mProductID+", "+mMacAddress);
        }

        Button buttonScan=(Button)findViewById(R.id.button_setting_scan);
        buttonScan.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {

                if (mPaired) {
                    //建立確認是否以此工地代號與姓名偵測的dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothDeviceListActivity.this);
                    builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                          scanDevices();
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
                    dialog.setMessage("確定要重新掃描新裝置?");
                    dialog.show();
                } else {
                    scanDevices();
                }
            }
        });
    }

    void showToast(String text) {
        Toast.makeText(BluetoothDeviceListActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    private void scanDevices(){
        if (_goFITSdk != null) {
            Log.i(TAG, "demo_function_scan");
            showToast("配對中....");
            // Demo - doScanDevice API
            _goFITSdk.doScanDevice(new GoFITSdk.DeviceScanCallback() {
                @Override
                public void onSuccess(ScanBluetoothDevice device) {
                    // TODO : TBD
                    Log.i(TAG, "doScanDevice() : onSuccess() : device = " + device.getDevice().getName() + ", " + device.getDevice().getAddress() + ", " + device.getRSSI() + ", " + device.getProductID());
                }

                @Override
                public void onCompletion(ArrayList<ScanBluetoothDevice> devices) {
                    showToast("Scan complete");
                    for (ScanBluetoothDevice device : devices) {
                        Log.i(TAG, "doScanDevice() : onCompletion() : device = " + device.getDevice().getName() + ", " + device.getDevice().getAddress() + ", " + device.getRSSI() + ", " + device.getProductID());
                    }

                    if (devices.size() > 0) {
                        mSelectDevice = devices.get(0);
                        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(BluetoothDeviceListActivity.this, R.layout.device_name);
                        pairedListView.setAdapter(adapter);
                        pairedListView.setOnItemClickListener(mDeviceClickListener);
                        for (int i = 0; i < devices.size(); i++) {
                            adapter.add(devices.get(i).getDevice().getAddress() + ", " + devices.get(i).getDevice().getName());
                        }


                        String summary = "Recommended Device : \n" + mSelectDevice.getDevice().getAddress() + ", " + mSelectDevice.getRSSI();
//                                pPref.setSummary(summary);
                        Log.i(TAG, "doScanDevice() : onCompletion() : mSelectDevice = " + mSelectDevice.getDevice().getName() + ", " + mSelectDevice.getDevice().getAddress() + ", " + mSelectDevice.getRSSI() + ", " + mSelectDevice.getProductID());
                    } else {
//                                pPref.setSummary("Device Not Found");
                        Log.d(TAG, "onCompletion: Devices Not found");
                    }
                }

                @Override
                public void onFailure(int errorCode, String errorMsg) {
                    Log.e(TAG, "doScanDevice() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                    showToast("未開啟藍牙 或 發生其他錯誤");
                }
            });
        } else {
            showToast("SDK Instance invalid, needs `SDK init`");
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            // Cancel discovery because it's costly and we're about to connect
            if (_goFITSdk != null) {
                Log.i(TAG, "demo_function_new_pairing");

                if (mSelectDevice != null) {
                    mMacAddress = mSelectDevice.getDevice().getAddress();
                }
                else {
                    Toast.makeText(BluetoothDeviceListActivity.this, "No Device Selected, `Scan Device` First!", Toast.LENGTH_SHORT).show();
                }

                // Demo - doNewPairing API
                _goFITSdk.doNewPairing(mSelectDevice, new GoFITSdk.NewPairingCallback() {
                    @Override
                    public void onSuccess(String pairingCode, String pairingTime) {
                        Log.i(TAG, "doNewPairing() : onSuccess() : Got pairingCode = " + pairingCode);
                        Log.i(TAG, "doNewPairing() : onSuccess() : Confirming...");
                        mPairingCode = pairingCode;
                        mPairingTime = pairingTime;
                        mConfirmPairingCodeHandler.postDelayed(mConfirmPairingCodeRunnable, 5000);
                    }

                    @Override
                    public void onFailure(int errorCode, String errorMsg) {
                        Log.e(TAG, "doNewPairing() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                        showToast("doNewPairing() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                    }
                });
            }
            else {
                showToast("SDK Instance invalid, needs `SDK init`");
            }
        }
    };

    private Handler mConfirmPairingCodeHandler = new Handler();
    private final Runnable mConfirmPairingCodeRunnable = new Runnable() {
        public void run() {
            mConfirmPairingCodeHandler.removeCallbacks(mConfirmPairingCodeRunnable);

            // Demo - confirmPairingCode API
            if (_goFITSdk != null) {
                mProductID = mSelectDevice.getProductID();
                _goFITSdk.doConfirmPairingCode(mPairingCode, mPairingTime, mProductID, new GoFITSdk.GenericCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "doConfirmPairingCode() : onSuccess() : Pairing Complete!");
                        showToast("Pairing complete");

                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(BluetoothDeviceListActivity.this);
                        SharedPreferences.Editor pe = sp.edit();
                        pe.putString("productID", mProductID);
                        pe.putString("deviceName",mSelectDevice.getDevice().getName());
                        pe.commit();

                        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(BluetoothDeviceListActivity.this,R.layout.device_name);
                        pairedListView.setAdapter(adapter);
                        pairedListView.setOnItemClickListener(mDeviceClickListener);
                        adapter.add("已配對裝置："+mProductID+", "+mMacAddress);
                        mPaired = true;
//                        Preference pPref = (Preference) findPreference("demo_function_new_pairing");
                        String summary = "Confirm Paring Code : " + mPairingCode + "(" + mPairingTime + ")";
//                        pPref.setSummary(summary);

//                        pPref = (Preference) findPreference("demo_connect_status");
                        // Demo - isBLEConnect API
                        boolean isConnect = _goFITSdk.isBLEConnect();
                        Log.d(TAG, "Connected: "+isConnect);
//                        summary = isConnect ? "Connected" : "Disconnected";
//                        pPref.setSummary(summary);

                        // Demo - setRemoteCameraHandler API
//                        demoSetRemoteCameraHandler();
                    }

                    @Override
                    public void onFailure(int errorCode, String errorMsg) {
                        Log.e(TAG, "doConfirmPairingCode() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                        showToast("doConfirmPairingCode() : onFailure() : errorCode = " + errorCode + ", " + "errorMsg = " + errorMsg);
                    }
                });
            }
            else {
                showToast("SDK Instance invalid, needs `SDK init`");
            }
        }
    };

//    void demoSetRemoteCameraHandler() {
//        _goFITSdk.setRemoteCameraHandler(new AppContract.RemoteCameraHandler() {
//            @Override
//            public void triggerCamera() {
//
//                Log.e("[RemoteCamera]", "Trigger Remote Camera!");
//
//                if (CameraClass.cameraGetCurrent() != null) {
//                    CameraClass.cameraTakePicture(mCameraShutterCallback, CameraClass.mCameraPictureCallback);
//                } else {
//                    startActivity(new Intent(getApplicationContext(), RemoteCamera.class));
//                }
//
//            }
//        });
//    }
}
