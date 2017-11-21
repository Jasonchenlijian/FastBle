package com.clj.fastble.scan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.bluetooth.BleBluetooth;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class PeriodScanCallback implements BluetoothAdapter.LeScanCallback {

    private Handler mHandler = new Handler(Looper.getMainLooper());
    protected BleBluetooth bleBluetooth;
    private long timeoutMillis = 10000;

    PeriodScanCallback(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public abstract void onStarted();

    public abstract void onFinished();

    public final void notifyScanStarted() {
        onStarted();
        if (timeoutMillis > 0) {
            removeHandlerMsg();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bleBluetooth.stopLeScan();
                }
            }, timeoutMillis);
        }
    }

    public final void notifyScanStopped() {
        removeHandlerMsg();
        onFinished();
    }

    public void removeHandlerMsg() {
        mHandler.removeCallbacksAndMessages(null);
    }


    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public PeriodScanCallback setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public BleBluetooth getBleBluetooth() {
        return bleBluetooth;
    }

    public PeriodScanCallback setBleBluetooth(BleBluetooth bluetooth) {
        this.bleBluetooth = bluetooth;
        return this;
    }
}
