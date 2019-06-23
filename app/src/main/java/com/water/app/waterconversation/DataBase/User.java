package com.water.app.waterconversation.DataBase;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey (autoGenerate = true)
    private int _id;

    private String userId;
    private String deviceId;
    private String site;
    private String date;
    private String time;
    private float longitude;
    private float latitude;
    private float accX;
    private float accY;
    private float accZ;
    private Integer accident;
    private Integer accidentAns;
    private Integer portent;
    private Integer portentAns;


    public User(String userId,String deviceId,String site,String date,String time,float longitude,float latitude,float accX,float accY,float accZ,Integer accident,Integer accidentAns,Integer portent,Integer portentAns){
        this.userId=userId;
        this.deviceId = deviceId;
        this.site = site;
        this.date = date;
        this.time = time;
        this.longitude = longitude;
        this.latitude = latitude;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.accident = accident;
        this.accidentAns = accidentAns;
        this.portent = portent;
        this.portentAns = portentAns;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public String getUserId(){
        return userId;
    }

    public void setDeviceId(String deviceId){
        this.deviceId = deviceId;
    }

    public String getDeviceId(){
        return deviceId;
    }

    public void setDate(String date){
        this.date = date;
    }

    public String getDate(){
        return date;
    }

    public void setTime(String time){
        this.time = time;
    }

    public String getTime(){
        return time;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setAccX(float accX) {
        this.accX = accX;
    }

    public float getAccX() {
        return accX;
    }

    public void setAccY(float accY) {
        this.accY = accY;
    }

    public float getAccY() {
        return accY;
    }

    public void setAccZ(float accZ) {
        this.accZ = accZ;
    }

    public float getAccZ() {
        return accZ;
    }

    public void setAccident(Integer accident) {
        this.accident = accident;
    }

    public Integer getAccident() {
        return accident;
    }

    public void setAccidentAns(Integer accidentAns) {
        this.accidentAns = accidentAns;
    }

    public Integer getAccidentAns() {
        return accidentAns;
    }

    public void setPortent(Integer portent) {
        this.portent = portent;
    }

    public Integer getPortent() {
        return portent;
    }

    public void setPortentAns(Integer portentAns) {
        this.portentAns = portentAns;
    }

    public Integer getPortentAns() {
        return portentAns;
    }
}