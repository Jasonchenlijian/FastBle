package com.clj.fastble.scan;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.clj.fastble.data.ScanResult;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * scan a known name device, then connect
 */
public abstract class NameScanCallback extends PeriodScanCallback {

    private String mName = null;
    private String[] mNames = null;
    private boolean mFuzzy = false;
    private AtomicBoolean hasFound = new AtomicBoolean(false);

    public NameScanCallback(String name, long timeoutMillis, boolean fuzzy) {
        super(timeoutMillis);
        this.mName = name;
        this.mFuzzy = fuzzy;
        if (TextUtils.isEmpty(name)) {
            onDeviceNotFound();
        }
    }

    public NameScanCallback(String[] names, long timeoutMillis, boolean fuzzy) {
        super(timeoutMillis);
        this.mNames = names;
        this.mFuzzy = fuzzy;
        if (names == null || names.length < 1) {
            onDeviceNotFound();
        }
    }

    @Override
    public void notifyScanCancel() {
        super.notifyScanCancel();
        hasFound.set(false);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        if (TextUtils.isEmpty(device.getName())) {
            return;
        }

        if (!hasFound.get()) {

            ScanResult scanResult = new ScanResult(device, rssi, scanRecord,
                    System.currentTimeMillis());

            if (mName != null) {
                if (mFuzzy ? device.getName().contains(mName) : mName.equalsIgnoreCase(device.getName())) {
                    hasFound.set(true);
                    bleBluetooth.stopScan(NameScanCallback.this);
                    onDeviceFound(scanResult);
                }
            } else if (mNames != null) {
                for (String name : mNames) {
                    if (mFuzzy ? device.getName().contains(name) : name.equalsIgnoreCase(device.getName())) {
                        hasFound.set(true);
                        bleBluetooth.stopScan(NameScanCallback.this);
                        onDeviceFound(scanResult);
                        return;
                    }
                }
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

    public abstract void onDeviceFound(ScanResult sanResult);

    public abstract void onDeviceNotFound();
}
