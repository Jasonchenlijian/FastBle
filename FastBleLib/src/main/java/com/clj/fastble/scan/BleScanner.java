package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleScanCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.data.ScanState;
import com.clj.fastble.exception.NotFoundDeviceException;
import com.clj.fastble.exception.ScanFailedException;

import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleScanner {

    private ScanCallback scanCallback;
    private ScanState scanState = ScanState.STATE_IDLE;

    public boolean scan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                        long timeOut, final BleScanCallback callback) {

        return startLeScan(serviceUuids, new ScanCallback(names, mac, fuzzy, false, timeOut) {
            @Override
            public void onScanStarted() {
                if (callback != null) {
                    callback.onScanStarted();
                }
            }

            @Override
            public void onScanning(ScanResult result) {
                if (callback != null) {
                    callback.onScanning(result);
                }
            }

            @Override
            public void onScanFinished(List<ScanResult> scanResultList) {
                if (callback != null) {
                    callback.onScanFinished(scanResultList);
                }
            }
        });

    }

    private boolean startLeScan(UUID[] serviceUuids, ScanCallback callback) {
        if (callback == null)
            return false;
        this.scanCallback = callback;
        boolean success = BleManager.getInstance().getBluetoothAdapter().startLeScan(serviceUuids, scanCallback);
        if (success) {
            scanState = ScanState.STATE_SCANNING;
            scanCallback.notifyScanStarted();
        } else {
            callback.removeHandlerMsg();
        }
        return success;
    }

    public void stopLeScan() {
        if (scanCallback == null)
            return;

        BleManager.getInstance().getBluetoothAdapter().stopLeScan(scanCallback);
        scanCallback.notifyScanStopped();
        scanCallback = null;

        if (scanState == ScanState.STATE_SCANNING) {
            scanState = ScanState.STATE_IDLE;
        }
    }

    public void scanAndConnect(UUID[] serviceUuids, String[] names, final String mac, boolean fuzzy,
                               final boolean autoConnect, long timeOut, final BleGattCallback callback) {

        boolean success = startLeScan(serviceUuids, new ScanCallback(names, mac, fuzzy, true, timeOut) {

            @Override
            public void onScanStarted() {
                if (callback != null) {
                    callback.onScanStarted();
                }
            }

            @Override
            public void onScanning(ScanResult result) {

            }

            @Override
            public void onScanFinished(final List<ScanResult> scanResultList) {
                if (scanResultList == null || scanResultList.size() < 1) {
                    if (callback != null) {
                        callback.onConnectError(new NotFoundDeviceException());
                    }
                } else {
                    if (callback != null) {
                        callback.onFoundDevice(scanResultList.get(0));
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            BleManager.getInstance().connect(scanResultList.get(0), callback);
                        }
                    });
                }
            }
        });
        if (!success && callback != null) {
            callback.onConnectError(new ScanFailedException());
        }
    }

}
