package com.water.app.waterconversation;

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

    public String getCurrentDate(){
        Calendar mCal = Calendar.getInstance();
        String dataFormat = "yyyy-MM-dd";
        SimpleDateFormat df = new SimpleDateFormat(dataFormat);
        String date = df.format(mCal.getTime());

        return date;
    }


}
