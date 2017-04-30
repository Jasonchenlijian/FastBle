package com.clj.fastble.scan;


import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.clj.fastble.data.ScanResult;

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
        if (TextUtils.isEmpty(mac)) {
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

            ScanResult scanResult = new ScanResult(device, rssi, scanRecord,
                    System.currentTimeMillis());

            if (mMac.equalsIgnoreCase(device.getAddress())) {
                hasFound.set(true);
                bleBluetooth.stopScan(MacScanCallback.this);
                onDeviceFound(scanResult);
            }
        }
    }

    @Override
    public void onScanTimeout() {
        onDeviceNotFound();
    }

    @Override
    public void onScanCancel() {

    }

    public abstract void onDeviceFound(ScanResult scanResult);

    public abstract void onDeviceNotFound();
}
