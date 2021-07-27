package com.water.app.waterconversation;

public class Constants {

    // INTENT ACTION
    public interface ACTION {
        String START_FOREGROUND_ACTION = "com.example.teddy.waterconversation.action.start.foreground";
        String STOP_FOREGROUND_ACTION = "com.example.teddy.waterconversation.action.stop.foreground";
        String ALARM_ACCIDENTS_DROP = "com.example.teddy.waterconversation.action.alarm.accidents.drop";
        String ALARM_ACCIDENTS_FALL = "com.example.teddy.waterconversation.action.alarm.accidents.fall";
        String ALARM_ACCIDENTS_COMA = "com.example.teddy.waterconversation.action.alarm.accidents.coma";
        String ALARM_ASK = "com.example.teddy.waterconversation.action.alarm.accidents.ask";
        String ALARM_PORTENTS_LOST_BALALNCE = "com.example.teddy.waterconversation.action.alarm.portents.lost_balance";
        String ALARM_PORTENTS_HEAVY_STEP = "com.example.teddy.waterconversation.action.alarm.portents.heavy_step";
        String ALARM_PORTENTS_SUDDENLY_WOBBING = "com.example.teddy.waterconversation.action.alarm.portents.suddenly_wobbing";
        String ALARM_BY_BROADCAST = "com.example.teddy.waterconversation.action.alarm.by.broadcast";
        String Receive_ACCIDENT = "com.example.teddy.waterconversation.action.receive.accidents";
        String Receive_ACCIDENTS_DROP = "com.example.teddy.waterconversation.action.receive.accidents.drop";
        String Receive_ACCIDENTS_FALL = "com.example.teddy.waterconversation.action.receive.accidents.fall";
        String Receive_ACCIDENTS_COMA = "com.example.teddy.waterconversation.action.receive.accidents.coma";
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
        int FALLUNKNOWN = 1;
        int DROPUNKNOWN = 2;
        int COMAUNKNOWN = 3;
        int FALLSAFE = 4;
        int DROPSAFE = 5;
        int COMASAFE = 6;
        int FALL = 7;
        int DROP = 8;
        int COMA = 9 ;
        int ASK = 10 ;
        int BREAK = 11;
        //int TIMES_UP = 7;
    }

    public interface TYPE{
        int ACCIDENT = 0;
        int PORTENT = 1;
    }
    public interface FCMtype{
        int Normal = 0;
        int FallUnknown = 1;
        int DropUnknown = 2;
        int ComaUnknown =3;
        int FallSafe =4;
        int DropSafe =5;
        int ComaSafe=6;
        int Fall =7;
        int Drop =8;
        int Coma =9;
    }
}
