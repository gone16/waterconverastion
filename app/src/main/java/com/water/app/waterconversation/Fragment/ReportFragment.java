package com.water.app.waterconversation.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static com.android.volley.VolleyLog.TAG;

public class ReportFragment extends Fragment implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static boolean isReportFragment = true;
    private GoogleApiClient googleApiClient;
    private Double latitude, longitude, altitude;
    private String longi, lati, alti;
    private String site, path, photo;
    private FirebaseFirestore fs;
    private PopupWindow pw;
    private File outputImage;
    EditText editTextRname;
    Button button_save;
    ImageButton button_add;
    ExifInterface exifInterface;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report, container, false);
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

        FirebaseApp.initializeApp(getActivity());
        FirebaseFirestore fs = FirebaseFirestore.getInstance();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        button_save = getActivity().findViewById(R.id.button_Rsave);
        button_add = getActivity().findViewById(R.id.button_add);

        button_save.setOnClickListener(this);
        button_add.setOnClickListener(this);

        editTextRname = getActivity().findViewById(R.id.editText_Rname);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        site = sharedPreferences.getString(getResources().getString(R.string.sharePreferences_site), "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isReportFragment = false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            //新增
            case R.id.button_add:
                final LinearLayout linearLayoutForm = (LinearLayout)getActivity().findViewById(R.id.linearLayoutForm);
                final ConstraintLayout newView = (ConstraintLayout) getActivity().getLayoutInflater().inflate(R.layout.report_object,null);
                    newView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    final ImageButton btnRemove = (ImageButton) newView.findViewById(R.id.btn_del);
                    btnRemove.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                linearLayoutForm.removeView(newView);
                            }
                        });
                    ImageView btnPic = (ImageView) newView.findViewById(R.id.button_Rpic);
                    btnPic.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            GlobalVariable gv = (GlobalVariable)getActivity().getApplicationContext();
                            gv.setChildID(linearLayoutForm.indexOfChild(newView));
                            showpopwindow();
                        }
                    });
                    linearLayoutForm.addView(newView);
                break;
            // 儲存
            case R.id.button_Rsave:
                if ("".equals(editTextRname.getText().toString())) {
                    Toast.makeText(getActivity(), "未填入危險區域之描述", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setPositiveButton(R.string.alert_reset_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        saveReportData();
                        Toast.makeText(getContext(), "上傳成功", Toast.LENGTH_SHORT).show();
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
                dialog.setMessage("是否上傳資料?");
                dialog.show();
                break;
            case R.id.tv_cancel:
                pw.dismiss();
                break;
            case R.id.tv_pick_photo:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
                pw.dismiss();
                break;
            case R.id.tv_take_photo:
                outputImage = new File(Environment.getExternalStorageDirectory(),"image.jpg");
                try {
                    //判断文件是否存在，存在删除，不存在创建
                    if (outputImage.exists()){
                        Log.e(TAG,"exists");
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                    Log.e(TAG,"create");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    Uri imageUri = FileProvider.getUriForFile(getContext(),"com.water.app.waterconversation.fileprovider",outputImage);
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent1.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                }else {
                    Uri imageUri = Uri.fromFile(outputImage);
                    intent1.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                }
                startActivityForResult(intent1, 2);
                pw.dismiss();
                break;
        }
    }

    private void showpopwindow() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_pop, null);
        pw = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setAnimationStyle(R.style.pop_anim);

        pw.showAtLocation(contentView, Gravity.BOTTOM, 0, 0);
        pw.setOutsideTouchable(true);

        TextView tpho = (TextView) contentView.findViewById(R.id.tv_take_photo);
        TextView ppho = (TextView) contentView.findViewById(R.id.tv_pick_photo);
        TextView can = (TextView) contentView.findViewById(R.id.tv_cancel);
        ppho.setOnClickListener(this);
        tpho.setOnClickListener(this);
        can.setOnClickListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        GlobalVariable gv = (GlobalVariable)getActivity().getApplicationContext();
        int i = gv.getChildID();
        if (resultCode == RESULT_OK && requestCode == 1) {
            try {
                Uri selectedImage = data.getData(); //獲取系統返回的照片的Uri
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                //從系統表中查詢指定Uri對應的照片
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                path = cursor.getString(columnIndex); //獲取照片路徑
                cursor.close();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                ExifInterface exifInterface = new ExifInterface(path);
                alti = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                longi= exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                lati = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                Bitmap bitmap = BitmapFactory.decodeFile(path,options);
                LinearLayout linearLayout = (LinearLayout)getActivity().findViewById(R.id.linearLayoutForm);
                ConstraintLayout innerLayout = (ConstraintLayout) linearLayout.getChildAt(i);
                ImageView iv = (ImageView)innerLayout.findViewById(R.id.button_Rpic);
                iv.setImageBitmap(bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] bytes = baos.toByteArray();
                photo = Base64.encodeToString(bytes, Base64.DEFAULT);
                iv.setTag(photo);
                longitude = GetDegree(longi);
                latitude = GetDegree(lati);
                altitude = GetAlti(alti);
                TextView lo = (TextView)innerLayout.findViewById(R.id.r_longi);
                TextView la = (TextView)innerLayout.findViewById(R.id.r_lati);
                TextView al = (TextView)innerLayout.findViewById(R.id.r_alti);
                lo.setText(Double.toString(longitude));
                la.setText(Double.toString(latitude));
                al.setText(Double.toString(altitude));
            } catch (Exception e) {
                Log.e("Exception", e.getMessage(), e);
            }
        }
        if (resultCode == RESULT_OK && requestCode == 2) {
            try {
                ExifInterface exifInterface = new ExifInterface(outputImage.getAbsolutePath());
                alti = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                longi= exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                lati = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 3;
                Bitmap bitmap = BitmapFactory.decodeFile(outputImage.getAbsolutePath(),options);
                LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.linearLayoutForm);
                ConstraintLayout innerLayout = (ConstraintLayout) linearLayout.getChildAt(i);
                ImageView iv = (ImageView) innerLayout.findViewById(R.id.button_Rpic);
                iv.setImageBitmap(bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] bytes = baos.toByteArray();
                //base64 encode
                photo = Base64.encodeToString(bytes, Base64.DEFAULT);
                iv.setTag(photo);
                longitude = GetDegree(longi);
                latitude = GetDegree(lati);
                altitude = GetAlti(alti);
                TextView lo = (TextView)innerLayout.findViewById(R.id.r_longi);
                TextView la = (TextView)innerLayout.findViewById(R.id.r_lati);
                TextView al = (TextView)innerLayout.findViewById(R.id.r_alti);
                lo.setText(Double.toString(longitude));
                la.setText(Double.toString(latitude));
                al.setText(Double.toString(altitude));
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

    private void saveReportData() {
        GlobalVariable gv = (GlobalVariable) getActivity().getApplicationContext();
        fs = FirebaseFirestore.getInstance();
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        LinearLayout linearLayout = (LinearLayout)getActivity().findViewById(R.id.linearLayoutForm);
        int a = linearLayout.getChildCount();
        Log.e(TAG,"aaaaaa"+a);
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            Map<String, Object> n = new HashMap<>();
            Map<String, Object> m = new HashMap<>();
            ConstraintLayout innerLayout = (ConstraintLayout) linearLayout.getChildAt(i);
            EditText editText = (EditText)innerLayout.findViewById(R.id.editText);
            ImageView iv = (ImageView)innerLayout.findViewById(R.id.button_Rpic);
            TextView lo = (TextView)innerLayout.findViewById(R.id.r_longi);
            TextView la = (TextView)innerLayout.findViewById(R.id.r_lati);
            TextView al = (TextView)innerLayout.findViewById(R.id.r_alti);
            m.put("areaName", editText.getText().toString());
            m.put("site", site);
            m.put("date", getCurrentDate());
            m.put("time", getCurrentTime());
            m.put("longitude", Double.valueOf(lo.getText().toString()));//121.568852+Math.random()*0.00032);
            m.put("latitude", Double.valueOf(la.getText().toString()));//25.049914+Math.random()*0.000265);
            m.put("altitude", Double.valueOf(al.getText().toString()));
            m.put("photo", iv.getTag());
            m.put("timestamp",ts);
            n.put(editText.getText().toString(), m);
            fs.collection("CTC_dangerArea").document(editTextRname.getText().toString()).set(n, SetOptions.merge());
        }
    }

    public String getCurrentTime() {
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "HH:mm:ss:SSSS";
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




    /**
     * 當連上google api時觸發
     *
     * @param bundle bundle
     */
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
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

}