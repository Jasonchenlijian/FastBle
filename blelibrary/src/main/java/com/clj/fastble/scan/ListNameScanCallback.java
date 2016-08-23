package com.clj.fastble.scan;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 陈利健 on 2016/8/17.
 * 一段限制时间内搜索并列出所有符合name的设备，过滤重复设备
 */
public abstract class ListNameScanCallback extends PeriodScanCallback {
    /**
     * 设备名
     */
    private String name;
    /**
     * 所有符合名称设备的集合
     */
    private List<BluetoothDevice> mScanLeDeviceList = new ArrayList<>();


    public ListNameScanCallback(String name, long timeoutMillis) {
        super(timeoutMillis);
        this.name = name;
        if (name == null) {
            throw new IllegalArgumentException("start scan, name can not be null!");
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;
        if (TextUtils.isEmpty(device.getName()))
            return;
        if (mScanLeDeviceList.contains(device)) {
            return;
        }
        if (name.equalsIgnoreCase(device.getName())) {
            mScanLeDeviceList.add(device);
            onDeviceFound(device, rssi, scanRecord);
        }
    }

    public abstract void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
}
