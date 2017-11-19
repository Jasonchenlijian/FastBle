package com.clj.fastble.scan;


import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.clj.fastble.data.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ScanCallback extends PeriodScanCallback {

    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private boolean mNeedConnect = false;
    private AtomicBoolean hasFound = new AtomicBoolean(false);
    private List<ScanResult> mScanResultList = new ArrayList<>();

    public ScanCallback(String[] names, String mac, boolean fuzzy, boolean needConnect, long timeoutMillis) {
        super(timeoutMillis);
        this.mDeviceNames = names;
        this.mDeviceMac = mac;
        this.mFuzzy = fuzzy;
        this.mNeedConnect = needConnect;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        if (TextUtils.isEmpty(device.getName())) {
            return;
        }

        ScanResult scanResult = new ScanResult(device, rssi, scanRecord, System.currentTimeMillis());

        if (mNeedConnect) {

            if (!hasFound.get()) {

                if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length > 0)) {
                    hasFound.set(true);
                    mScanResultList.add(scanResult);
                    bleBluetooth.stopLeScan();
                    return;
                }

                if (!TextUtils.isEmpty(mDeviceMac)) {
                    if (!mDeviceMac.equalsIgnoreCase(device.getAddress()))
                        return;
                }

                if (mDeviceNames != null && mDeviceNames.length > 0) {

                    boolean equal = false;
                    for (String name : mDeviceNames) {
                        if (mFuzzy ? device.getName().contains(name) : name.equalsIgnoreCase(device.getName())) {
                            equal = true;
                        }
                    }
                    if (!equal) {
                        return;
                    }
                }

                hasFound.set(true);
                mScanResultList.add(scanResult);
                bleBluetooth.stopLeScan();
            }

        } else {
            synchronized (this) {
                hasFound.set(false);
                for (ScanResult result : mScanResultList) {
                    if (result.getDevice().equals(device)) {
                        hasFound.set(true);
                    }
                }
                if (!hasFound.get()) {
                    mScanResultList.add(scanResult);
                    onScanning(scanResult);
                }
            }
        }

    }

    @Override
    public void onStarted() {
        mScanResultList.clear();
        hasFound.set(false);
        onScanStarted();
    }

    @Override
    public void onFinished() {
        onScanFinished(mScanResultList);
    }

    public abstract void onScanStarted();

    public abstract void onScanning(ScanResult result);

    public abstract void onScanFinished(List<ScanResult> scanResultList);


}
