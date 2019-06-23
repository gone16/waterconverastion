package com.water.app.waterconversation.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UploadData {

    public String id;
    public String idDevice;
    public String date;
    public String time;
    public float latitude;
    public float longitude;
    public float accX,accY,accZ;
    public int accident,accidentAns,portent,portentAns;
    public String site;
    public float altitude;
    public float heartRate;

    public UploadData(String id, String idDevice, String date, String time, float latitude, float longitude,
                float accX, float accY, float accZ, int accident, int accidentAns, int portent,
                int portentAns, String site, float altitude,float heartRate) {
        this.id = id;
        this.idDevice = idDevice;
        this.date = date;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.accident = accident;
        this.accidentAns = accidentAns;
        this.portent = portent;
        this.portentAns = portentAns;
        this.site = site;
        this.altitude = altitude;
        this.heartRate = heartRate;
    }
}