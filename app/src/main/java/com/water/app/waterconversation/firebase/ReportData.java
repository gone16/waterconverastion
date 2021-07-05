package com.water.app.waterconversation.firebase;

import java.util.Map;

public class ReportData {
    public String areaName;
    public String site;
    public String date;
    public String time;
    public Integer radius;
    public double longitude;
    public double latitude;
    public double altitude;
    public String photo;

    public ReportData(String areaName, String site, String date, String time, Integer radius,
                       double longitude, double latitude, double altitude,String photo){
        this.areaName = areaName;
        this.site = site;
        this.date = date;
        this.time = time;
        this.radius = radius;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.photo = photo;
    }
}
