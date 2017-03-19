package com.clj.fastble.scan;


import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * scan a known mac device, then connect
 */
public abstract class MacScanCallback extends PeriodScanCallback {


    private String mMac;
    private AtomicBoolean hasFound = new AtomicBoolean(false);

    public MacScanCallback(String mac, long timeoutMillis) {
        super(timeoutMillis);
        this.mMac = mac;
        if (mac == null) {
            onDeviceNotFound();
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
            if (mMac.equalsIgnoreCase(device.getAddress())) {
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
