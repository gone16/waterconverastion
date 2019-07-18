package com.water.app.waterconversation;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MyTime {

    public String getCurrentTime(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "kk:mm:ss:SSS";
        String[] hour =  dataFormat.split(":");
        if(hour[0] == "24"){
            dataFormat = "00"+":"+hour[1]+":"+hour[2]+":"+hour[3];
        }
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());

        return time;
    }

    public String getCurrentHour(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "kk:mm:ss:SSS";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String time = df.format(mCal.getTime());
        String[] hour = time.split(":");
        return hour[0];
    }

    public String getCurrentDate(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "yyyy-MM-dd";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String date = df.format(mCal.getTime());

        return date;
    }

    public long getLongCurrentTime() {
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "yyyy-MM-dd kk:mm:ss";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        long time= 0;
        try {
            time = df.parse(df.format(mCal.getTime())).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    public long getLongTime(String date,String time){
        try{
            Calendar mCal = Calendar.getInstance();
            String dataFormat = "yyyy-MM-dd kk:mm:ss";
            SimpleDateFormat df = new SimpleDateFormat(dataFormat);
            String timeFull = date+" "+time;
            Log.e("timeFull",timeFull);
            //long dbtime = 0;

            long dbTime = df.parse(timeFull).getTime();
            Log.d("dbTime",dbTime+"");

            return dbTime;
        }catch (ParseException e){
            e.printStackTrace();
        }
        return 0;
    }


}
