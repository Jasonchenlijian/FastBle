package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleScanPresenter implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private boolean mNeedConnect = false;
    private List<BleDevice> mBleDeviceList = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private long timeoutMillis = 10000;

    public BleScanPresenter(String[] names, String mac, boolean fuzzy, boolean needConnect, long timeoutMillis) {
        this.mDeviceNames = names;
        this.mDeviceMac = mac;
        this.mFuzzy = fuzzy;
        this.mNeedConnect = needConnect;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        BleDevice scanResult = new BleDevice(device, rssi, scanRecord, System.currentTimeMillis());

        synchronized (this) {
            if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
                next(scanResult);
                return;
            }

            if (!TextUtils.isEmpty(mDeviceMac)) {
                if (!mDeviceMac.equalsIgnoreCase(device.getAddress()))
                    return;
            }

            if (mDeviceNames != null && mDeviceNames.length > 0) {
                AtomicBoolean equal = new AtomicBoolean(false);
                for (String name : mDeviceNames) {
                    String remoteName = device.getName();
                    if (remoteName == null)
                        remoteName = "";
                    if (mFuzzy ? remoteName.contains(name) : remoteName.equalsIgnoreCase(name)) {
                        equal.set(true);
                    }
                }
                if (!equal.get()) {
                    return;
                }
            }

            next(scanResult);
        }
    }

    private void next(BleDevice bleDevice) {
        if (mNeedConnect) {
            mBleDeviceList.add(bleDevice);
            BleManager.getInstance().getBleScanner().stopLeScan();
        } else {
            AtomicBoolean hasFound = new AtomicBoolean(false);
            for (BleDevice result : mBleDeviceList) {
                if (result.getDevice().equals(bleDevice.getDevice())) {
                    hasFound.set(true);
                }
            }
            if (!hasFound.get()) {
                mBleDeviceList.add(bleDevice);
                onScanning(bleDevice);
            }
        }
    }

    public final void notifyScanStarted(boolean success) {
        if(success){
            mBleDeviceList.clear();
            onScanStarted(true);
            if (timeoutMillis > 0) {
                removeHandlerMsg();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BleManager.getInstance().getBleScanner().stopLeScan();
                    }
                }, timeoutMillis);
            }
        }else {
            onScanStarted(false);
        }
    }

    public final void notifyScanStopped() {
        removeHandlerMsg();
        onScanFinished(mBleDeviceList);
    }

    public final void removeHandlerMsg() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(BleDevice bleDevice);

    public abstract void onScanFinished(List<BleDevice> bleDeviceList);


}
