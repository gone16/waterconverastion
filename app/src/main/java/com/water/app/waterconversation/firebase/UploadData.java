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
    public int accident,portent;
    public String site;
    public float altitude;
    public String token;
    public Boolean machine;
    public String machineName;
    public String receiveAns;

    public UploadData(String id, String idDevice, String date, String time, float latitude, float longitude,
                      float accX, float accY, float accZ, int accident, int portent, String site, float altitude,
                      String token,Boolean machine,String machineName,String receiveAns) {
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
        this.portent = portent;
        this.site = site;
        this.altitude = altitude;
        this.token = token;
        this.machine = machine;
        this.machineName = machineName;
        this.receiveAns = receiveAns;
    }
}