package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class ScanCallback implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private boolean mNeedConnect = false;
    private List<ScanResult> mScanResultList = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private long timeoutMillis = 10000;

    public ScanCallback(String[] names, String mac, boolean fuzzy, boolean needConnect, long timeoutMillis) {
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

        if (TextUtils.isEmpty(device.getName())) {
            return;
        }

        ScanResult scanResult = new ScanResult(device, rssi, scanRecord, System.currentTimeMillis());

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
                    if (mFuzzy ? device.getName().contains(name) : name.equalsIgnoreCase(device.getName())) {
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

    private void next(ScanResult scanResult) {
        if (mNeedConnect) {
            mScanResultList.add(scanResult);
            BleManager.getInstance().getBleScanner().stopLeScan();
        } else {
            AtomicBoolean hasFound = new AtomicBoolean(false);
            for (ScanResult result : mScanResultList) {
                if (result.getDevice().equals(scanResult.getDevice())) {
                    hasFound.set(true);
                }
            }
            if (!hasFound.get()) {
                mScanResultList.add(scanResult);
                onScanning(scanResult);
            }
        }
    }

    public final void notifyScanStarted() {
        mScanResultList.clear();
        onScanStarted();
        if (timeoutMillis > 0) {
            removeHandlerMsg();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BleManager.getInstance().getBleScanner().stopLeScan();
                }
            }, timeoutMillis);
        }
    }

    public final void notifyScanStopped() {
        removeHandlerMsg();
        onScanFinished(mScanResultList);
    }

    public final void removeHandlerMsg() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted();

    public abstract void onScanning(ScanResult result);

    public abstract void onScanFinished(List<ScanResult> scanResultList);


}
