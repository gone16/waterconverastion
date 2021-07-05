package com.water.app.waterconversation.Service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import static com.water.app.waterconversation.GlobalVariable.CHANNEL_ID;


import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.water.app.waterconversation.Activity.MainActivity;
import com.water.app.waterconversation.Constants;
import com.water.app.waterconversation.DangerReceiver;
import com.water.app.waterconversation.GlobalVariable;
import com.water.app.waterconversation.R;



public class MyFirebaseService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseService";


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        GlobalVariable gv = (GlobalVariable) getApplicationContext();
        String d = remoteMessage.getData().get("accident");
        String t = remoteMessage.getData().get("title");
        gv.setReceiveID(remoteMessage.getData().get("id"));
        boolean receive = false;
        Log.d(TAG, "title" + t);

        if (remoteMessage.getData().size() > 0) {
            if (t.equals("accident")) {
                if (d.equals("1")) {
                    receiveAccident(Constants.ACTION.Receive_ACCIDENTS_FALL);
                    Log.d(TAG, "" + d);
                    receive = true;
                } else if (d.equals("2")) {
                    receiveAccident(Constants.ACTION.Receive_ACCIDENTS_DROP);
                    receive = true;
                } else if (d.equals("3")) {
                    receiveAccident(Constants.ACTION.Receive_ACCIDENTS_COMA);
                    receive = true;
                }
            }
            if (t.equals("dangerAreaDanger")) {
                Intent mIntent = new Intent("dangerReceive");
                mIntent.setAction("danger");
                String status = remoteMessage.getData().get("status");
                mIntent.putExtra("status", status);
                sendBroadcast(mIntent);
                sendNotification("注意",  status);
            }
            if (t.equals("machineDanger")) {
                if (gv.getIsMachineOperating() == true) {
                    Intent mIntent = new Intent("dangerReceive");
                    mIntent.setAction("moperater");
                    String status = remoteMessage.getData().get("status");
                    mIntent.putExtra("machine", status);
                    sendBroadcast(mIntent);
                    sendNotification("注意",  status + "，請小心操作");
                }
                if (gv.getIsMachineOperating() == false) {
                    Intent mIntent = new Intent("dangerReceive");
                    mIntent.setAction("danger");
                    String status = remoteMessage.getData().get("status");
                    mIntent.putExtra("status", status);
                    sendBroadcast(mIntent);
                    sendNotification("注意", status + "，請盡速離開");
                }
            }
            if (t.equals("machine")) {
                gv.setOperateEnable(true);
                String mName = remoteMessage.getData().get("machine");
                Intent mIntent = new Intent("machineReceive");
                mIntent.setAction("machine");
                mIntent.putExtra("machine", mName);
                sendBroadcast(mIntent);
                gv.setMachineName(mName);
                gv.setMachineRadius(remoteMessage.getData().get("radius"));
                sendNotification("靠近機具", "已靠近" + mName + "，點選已進入操作頁面");
            }
        }
        if (remoteMessage.getNotification() != null) {
            sendNotification(remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
            Log.i(TAG, "title" + remoteMessage.getNotification().getTitle());
        }
        if (!receive) {
            gv.setReceiveID("none");
        }
    }

    private void receiveAccident(String type) {
        GlobalVariable globalVariable = (GlobalVariable) getApplicationContext();
        if (globalVariable.getAlarming()) {
            Intent intent1 = new Intent(Constants.ACTION.Receive_ACCIDENT);
            Bundle bundle = new Bundle();
            bundle.putString("state", type);
            intent1.putExtras(bundle);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendBroadcast(intent1);
            return;
        }
        PendingIntent pi;
        AlarmManager am;
//        Log.d(TAG, "alarmAccidents: "+type);
        switch (type) {
            case Constants.ACTION.Receive_ACCIDENTS_DROP:
                Intent intent1 = new Intent(this, DangerReceiver.class);
                intent1.setAction(Constants.ACTION.Receive_ACCIDENTS_DROP);
                pi = PendingIntent.getActivity(this, 0, intent1, 0);
                am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pi);
                globalVariable.setReceiveAlarming(true);
                break;

            case Constants.ACTION.Receive_ACCIDENTS_FALL:
                Intent intent2 = new Intent(this, DangerReceiver.class);
                intent2.setAction(Constants.ACTION.Receive_ACCIDENTS_FALL);
                pi = PendingIntent.getActivity(this, 0, intent2, 0);
                am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pi);
                globalVariable.setReceiveAlarming(true);
                break;
            case Constants.ACTION.Receive_ACCIDENTS_COMA:
                Intent intent3 = new Intent(this, DangerReceiver.class);
                intent3.setAction(Constants.ACTION.Receive_ACCIDENTS_COMA);
                pi = PendingIntent.getActivity(this, 0, intent3, 0);
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pi);
                globalVariable.setReceiveAlarming(true);
                break;
        }
    }

    //取得token
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token" + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
    }

    //送通知
    private void sendNotification(String messageTitle, String messageBody) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(messageTitle)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }
}

