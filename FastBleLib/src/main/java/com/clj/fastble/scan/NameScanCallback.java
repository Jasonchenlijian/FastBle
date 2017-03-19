package com.clj.fastble.scan;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * scan a known name device, then connect
 */
public abstract class NameScanCallback extends PeriodScanCallback {


    private String mName;
    private boolean mFuzzy;
    private AtomicBoolean hasFound = new AtomicBoolean(false);

    public NameScanCallback(String name, long timeoutMillis, boolean fuzzy) {
        super(timeoutMillis);
        this.mName = name;
        this.mFuzzy = fuzzy;
        if (name == null) {
            onDeviceNotFound();
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
            if (mFuzzy ? mName.contains(device.getName()) : mName.equalsIgnoreCase(device.getName())) {
                hasFound.set(true);
                bleBluetooth.stopScan(NameScanCallback.this);
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
