package com.clj.fastble.data;



public class BleMsg {

    // Notify
    public static final int MSG_CHA_NOTIFY_START = 0x11;
    public static final int MSG_CHA_NOTIFY_RESULT = 0x12;
    public static final int MSG_CHA_NOTIFY_DATA_CHANGE = 0x13;
    public static final String KEY_NOTIFY_BUNDLE_STATUS = "notify_status";
    public static final String KEY_NOTIFY_BUNDLE_VALUE = "notify_value";

    // Indicate
    public static final int MSG_CHA_INDICATE_START = 0x21;
    public static final int MSG_CHA_INDICATE_RESULT = 0x22;
    public static final int MSG_CHA_INDICATE_DATA_CHANGE = 0x23;
    public static final String KEY_INDICATE_BUNDLE_STATUS = "indicate_status";
    public static final String KEY_INDICATE_BUNDLE_VALUE = "indicate_value";

    // Write
    public static final int MSG_CHA_WRITE_START = 0x31;
    public static final int MSG_CHA_WRITE_RESULT = 0x32;
    public static final String KEY_WRITE_BUNDLE_STATUS = "write_status";

    // Read
    public static final int MSG_CHA_READ_START = 0x41;
    public static final int MSG_CHA_READ_RESULT = 0x42;
    public static final String KEY_READ_BUNDLE_STATUS = "read_status";
    public static final String KEY_READ_BUNDLE_VALUE = "read_value";

    // Read Rssi
    public static final int MSG_READ_RSSI_START = 0x51;
    public static final int MSG_READ_RSSI_RESULT = 0x52;
    public static final String KEY_READ_RSSI_BUNDLE_STATUS = "rssi_status";
    public static final String KEY_READ_RSSI_BUNDLE_VALUE = "rssi_value";

    // Mtu
    public static final int MSG_SET_MTU_START = 0x61;
    public static final int MSG_SET_MTU_RESULT = 0x62;
    public static final String KEY_SET_MTU_BUNDLE_STATUS = "mtu_status";
    public static final String KEY_SET_MTU_BUNDLE_VALUE = "mtu_value";



}
