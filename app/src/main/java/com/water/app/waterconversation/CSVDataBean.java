package com.water.app.waterconversation;

public class CSVDataBean {
    public String id;
    public String idDevice;
    public String date;
    public String time;
    public float latitude;
    public float longitude;
    public float accX,accY,accZ,pitch,roll;
    public int accident,portent;
    public String site;
    public float altitude;

    public CSVDataBean(String id, String idDevice, String date, String time, float latitude, float longitude, float accX, float accY, float accZ,float pitch,float roll, int accident, int portent, String site, float altitude) {
        this.id = id;
        this.idDevice = idDevice;
        this.date = date;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.pitch = pitch;
        this.roll = roll;
        this.accident = accident;
        this.portent = portent;
        this.site = site;
        this.altitude = altitude;
    }

    public CSVDataBean(){

    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIdDevice(String idDevice) {
        this.idDevice = idDevice;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setAccX(float accX) {
        this.accX = accX;
    }

    public void setAccY(float accY) {
        this.accY = accY;
    }

    public void setAccZ(float accZ) {
        this.accZ = accZ;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public void setAccident(int accident) {
        this.accident = accident;
    }

    public void setPortent(int portent) {
        this.portent = portent;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public String getId() {
        return id;
    }

    public String getIdDevice() {
        return idDevice;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getAccX() {
        return accX;
    }

    public float getAccY() {
        return accY;
    }

    public float getAccZ() {
        return accZ;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

    public int getAccident() {
        return accident;
    }

    public int getPortent() {
        return portent;
    }

    public String getSite() {
        return site;
    }

    public float getAltitude() {
        return altitude;
    }

}
