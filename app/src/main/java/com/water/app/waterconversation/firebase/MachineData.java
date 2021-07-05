package com.water.app.waterconversation.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MachineData {
    public String machineName;
    public String site;
    public String date;
    public String time;
    public Integer radius;
    public double longitude;
    public double latitude;
    public double altitude;

    public MachineData(String machineName, String site, String date, String time, Integer radius,
                       double longitude, double latitude, double altitude){
        this.machineName = machineName;
        this.site = site;
        this.date = date;
        this.time = time;
        this.radius = radius;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }
}
