package com.robotics.infrareddemo;

public class ImageDataBean {

    byte[] data;
    int width;
    int height;
    long time =0l;
    byte[] infrareddata;
    int infraredwidth;
    int infraredheight;
    long infraredTime =0l;
    public ImageDataBean(byte[] data, int width, int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }
    public void setinfrareddata(byte[] data, int width, int height){
        this.infrareddata = data;
        this.infraredwidth = width;
        this.infraredheight = height;
    }
    public void setdata(byte[] data, int width, int height){
        this.data = data;
        this.width = width;
        this.height = height;
    }
    public void setinfrareddata(byte[] data, int width, int height,long infraredTime){
        this.infrareddata = data;
        this.infraredwidth = width;
        this.infraredheight = height;
        this.infraredTime=infraredTime;
    }
    public void setdata(byte[] data, int width, int height,long time){
        this.data = data;
        this.width = width;
        this.height = height;
        this.time=time;
    }

    public ImageDataBean() {
    }

    public byte[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }


    public int getHeight() {
        return height;
    }


    public byte[] getInfrareddata() {
        return infrareddata;
    }

    public int getInfraredwidth() {
        return infraredwidth;
    }

    public int getInfraredheight() {
        return infraredheight;
    }

    public long getTime() {
        return time;
    }

    public long getInfraredTime() {
        return infraredTime;
    }

    public String string() {
        return "ImageDataBean{" +
                "data="  +data.hashCode()+
                ",\n width=" + width +
                ",\n height=" + height +
                ",\n infrareddata=" +infrareddata.hashCode()+
                ",\n infraredwidth=" + infraredwidth +
                ",\n infraredheight=" + infraredheight +
                '}';
    }

}
