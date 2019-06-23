package com.water.app.waterconversation;

public class Constants {

    // INTENT ACTION
    public interface ACTION{
        String START_FOREGROUND_ACTION = "com.example.teddy.waterconversation.action.start.foreground";
        String STOP_FOREGROUND_ACTION = "com.example.teddy.waterconversation.action.stop.foreground";
        String ALARM_ACCIDENTS_DROP = "com.example.teddy.waterconversation.action.alarm.accidents.drop";
        String ALARM_ACCIDENTS_FALL = "com.example.teddy.waterconversation.action.alarm.accidents.fall";
        String ALARM_ACCIDENTS_COMA = "com.example.teddy.waterconversation.action.alarm.accidents.coma";
        String ALARM_PORTENTS_LOST_BALALNCE = "com.example.teddy.waterconversation.action.alarm.portents.lost_balance";
        String ALARM_PORTENTS_HEAVY_STEP = "com.example.teddy.waterconversation.action.alarm.portents.heavy_step";
        String ALARM_PORTENTS_SUDDENLY_WOBBING = "com.example.teddy.waterconversation.action.alarm.portents.suddenly_wobbing";
        String ALARM_BY_BROADCAST = "com.example.teddy.waterconversation.action.alarm.by.broadcast";
    }

    // 前兆的常數設定
    public interface PORTENTS{
        int NORMAL = 0;
        int LOST_BALANCE = 1;
        int HEAVY_STEP =2;
        int SUDDENLY_WOBBING = 3;
        int UNKNOWN = 4;
        int TIMES_UP = 5;
    }

    //意外的常數設定
    public interface ACCIDENTS{
        int NORMAL = 0;
        int DROP = 1;
        int FALL = 2;
        int COMA = 3;
        int UNKNOWN = 4;
        int TIMES_UP = 5;
    }

    public interface TYPE{
        int ACCIDENT = 0;
        int PORTENT = 1;
    }
}
