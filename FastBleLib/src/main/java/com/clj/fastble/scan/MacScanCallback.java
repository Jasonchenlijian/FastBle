package com.clj.fastble.scan;


import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by 陈利健 on 2016/11/25.
 * 一段限制时间内搜索符合mac的设备，取第一个搜索到的设备
 */
public abstract class MacScanCallback extends PeriodScanCallback{

    /**
     * 设备名
     */
    private String mac;
    /**
     * 是否发现
     */
    private AtomicBoolean hasFound = new AtomicBoolean(false);


    public MacScanCallback(String mac, long timeoutMillis) {
        super(timeoutMillis);
        this.mac = mac;
        if (mac == null) {
            throw new IllegalArgumentException("start scan, mac can not be null!");
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;
        if (TextUtils.isEmpty(device.getAddress())) {
            return;
        }

        if (!hasFound.get()) {
            if (mac.equalsIgnoreCase(device.getAddress())) {
                hasFound.set(true);
                bleBluetooth.stopScan(MacScanCallback.this);
                onDeviceFound(device, rssi, scanRecord);
            }
        }
    }

    @Override
    public void onScanTimeout() {
        onDeviceNotFound();
    }

    public abstract void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);

    public abstract void onDeviceNotFound();
}
