package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.NotFoundDeviceException;

import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleScanner {

    public static BleScanner getInstance() {
        return BleScannerHolder.sBleScanner;
    }

    private static class BleScannerHolder {
        private static final BleScanner sBleScanner = new BleScanner();
    }

    private BleScanPresenter scanCallback;
    private BleScanState scanState = BleScanState.STATE_IDLE;

    public void scan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                     long timeOut, final BleScanCallback callback) {

        startLeScan(serviceUuids, new BleScanPresenter(names, mac, fuzzy, false, timeOut) {
            @Override
            public void onScanStarted(boolean success) {
                if (callback != null) {
                    callback.onScanStarted(success);
                }
            }

            @Override
            public void onScanning(BleDevice result) {
                if (callback != null) {
                    callback.onScanning(result);
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                if (callback != null) {
                    callback.onScanFinished(scanResultList);
                }
            }
        });

    }

    private synchronized void startLeScan(UUID[] serviceUuids, BleScanPresenter callback) {
        if (callback == null)
            return;
        this.scanCallback = callback;
        boolean success = BleManager.getInstance().getBluetoothAdapter().startLeScan(serviceUuids, scanCallback);
        if (success) {
            scanState = BleScanState.STATE_SCANNING;
            scanCallback.notifyScanStarted(true);
        } else {
            scanCallback.notifyScanStarted(false);
            callback.removeHandlerMsg();
        }
    }

    public synchronized void stopLeScan() {
        if (scanCallback == null)
            return;

        BleManager.getInstance().getBluetoothAdapter().stopLeScan(scanCallback);
        scanCallback.notifyScanStopped();
        scanCallback = null;

        if (scanState == BleScanState.STATE_SCANNING) {
            scanState = BleScanState.STATE_IDLE;
        }
    }

    public void scanAndConnect(UUID[] serviceUuids, String[] names, final String mac, boolean fuzzy,
                               long timeOut, final BleGattCallback callback) {

        startLeScan(serviceUuids, new BleScanPresenter(names, mac, fuzzy, true, timeOut) {

            @Override
            public void onScanStarted(boolean success) {
                if (callback != null) {
                    callback.onScanStarted(success);
                }
            }

            @Override
            public void onScanning(BleDevice bleDevice) {

            }

            @Override
            public void onScanFinished(final List<BleDevice> bleDeviceList) {
                if (bleDeviceList == null || bleDeviceList.size() < 1) {
                    if (callback != null) {
                        callback.onConnectError(new NotFoundDeviceException());
                    }
                } else {
                    if (callback != null) {
                        callback.onFoundDevice(bleDeviceList.get(0));
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            BleManager.getInstance().connect(bleDeviceList.get(0), callback);
                        }
                    });
                }
            }
        });
    }

}
