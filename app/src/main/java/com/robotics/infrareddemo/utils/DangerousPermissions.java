package com.robotics.infrareddemo.utils;

import java.util.ArrayList;


public class DangerousPermissions {
    public static final ArrayList<String> needrequest=new ArrayList<>();
    static {
        needrequest.add("android.permission.WRITE_CONTACTS");
        needrequest.add("android.permission.GET_ACCOUNTS");
        needrequest.add("android.permission.READ_CONTACTS");
        needrequest.add("android.permission-group.CONTACTS");

        needrequest.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        needrequest.add("android.permission.PROCESS_OUTGOING_CALLS");
        needrequest.add("android.permission.USE_SIP");
        needrequest.add("android.permission.WRITE_CALL_LOG");
        needrequest.add("android.permission.CALL_PHONE");
        needrequest.add("android.permission.READ_PHONE_STATE");
        needrequest.add("android.permission.READ_CALL_LOG");
        needrequest.add("android.permission-group.PHONE");

        needrequest.add("android.permission.READ_CALENDAR");
        needrequest.add("android.permission.WRITE_CALENDAR");
        needrequest.add("android.permission-group.CALENDAR");

        needrequest.add("android.permission.BODY_SENSORS");
        needrequest.add("android.permission-group.SENSORS");

        needrequest.add("android.permission.CAMERA");
        needrequest.add("android.permission-group.CAMERA");

        needrequest.add("android.permission.ACCESS_COARSE_LOCATION");
        needrequest.add("android.permission.ACCESS_FINE_LOCATION");
        needrequest.add("android.permission-group.LOCATION");

        needrequest.add("android.permission.WRITE_EXTERNAL_STORAGE");
        needrequest.add("android.permission.READ_EXTERNAL_STORAGE");
        needrequest.add("android.permission-group.STORAGE");

        needrequest.add("android.permission.RECORD_AUDIO");
        needrequest.add("android.permission-group.MICROPHONE");

        needrequest.add("android.permission.READ_SMS");
        needrequest.add("android.permission.RECEIVE_WAP_PUSH");
        needrequest.add("android.permission.RECEIVE_MMS");
        needrequest.add("android.permission.RECEIVE_SMS");
        needrequest.add("android.permission.SEND_SMS");
        needrequest.add("android.permission.READ_CELL_BROADCASTS");
        needrequest.add("android.permission-group.SMS");

        needrequest.add("android.permission.ACCESS_FINE_LOCATION");
        needrequest.add("android.permission.ACCESS_COARSE_LOCATION");
    }
}
