package com.clj.fastble.scan;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by 陈利健 on 2016/8/12.
 * 一段限制时间内搜索符合name的设备，取第一个搜索到的设备
 */
public abstract class FirstNameScanCallback extends PeriodScanCallback {

    /**
     * 设备名
     */
    private String name;
    /**
     * 是否发现
     */
    private AtomicBoolean hasFound = new AtomicBoolean(false);


    public FirstNameScanCallback(String name, long timeoutMillis) {
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
        if (TextUtils.isEmpty(device.getName())) {
            return;
        }

        if (!hasFound.get()) {
            if (name.equalsIgnoreCase(device.getName())) {
                hasFound.set(true);
                liteBluetooth.stopScan(FirstNameScanCallback.this);
                onDeviceFound(device, rssi, scanRecord);
            }
        }
    }

    public abstract void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
}
