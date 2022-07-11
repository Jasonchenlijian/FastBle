package com.clj.fastble.scan;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleScanListener;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.utils.BleLog;

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

    private BleScanState mBleScanState = BleScanState.STATE_IDLE;

    private final BleScanPresenter mBleScanPresenter = new BleScanPresenter() {

        @Override
        public void onScanStarted(boolean success) {
            BleScanListener callback = getBleScanListener();
            if (callback != null) {
                callback.onScanStarted(success);
            }
        }

        @Override
        public void onLeScan(BleDevice bleDevice) {
            BleScanListener callback = mBleScanPresenter.getBleScanListener();
            if (mBleScanPresenter.ismNeedConnect()) {
                if (callback != null) {
                    ((BleScanAndConnectCallback) callback).onLeScan(bleDevice);
                }
            } else {
                if (callback != null) {
                    ((BleScanCallback) callback).onLeScan(bleDevice);
                }
            }
        }

        @Override
        public void onScanning(BleDevice result) {
            BleScanListener callback = mBleScanPresenter.getBleScanListener();
            if (callback != null) {
                callback.onScanning(result);
            }
        }

        @Override
        public void onScanFinished(List<BleDevice> bleDeviceList) {
            final BleScanListener callback =
                    mBleScanPresenter.getBleScanListener();
            if (mBleScanPresenter.ismNeedConnect()) {
                if (bleDeviceList == null || bleDeviceList.size() < 1) {
                    if (callback != null) {
                        ((BleScanAndConnectCallback) callback).onScanFinished(null);
                    }
                } else {
                    if (callback != null) {
                        ((BleScanAndConnectCallback) callback).onScanFinished(bleDeviceList.get(0));
                    }
                    BleManager.getInstance().connect(bleDeviceList.get(0), ((BleScanAndConnectCallback) callback));
                }
            } else {
                if (callback != null) {
                    ((BleScanCallback) callback).onScanFinished(bleDeviceList);
                }
            }
            mBleScanPresenter.setCancelBleScanListener();
        }
    };

    public void scan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                     long timeOut, final BleScanCallback callback) {

        startLeScan(serviceUuids, names, mac, fuzzy, false, timeOut, callback);
    }

    public void scanAndConnect(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                               long timeOut, BleScanAndConnectCallback callback) {

        startLeScan(serviceUuids, names, mac, fuzzy, true, timeOut, callback);
    }

    @SuppressLint("MissingPermission")
    private void startLeScan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                             boolean needConnect, long timeOut, BleScanListener imp) {
        synchronized (this) {
            if (mBleScanState != BleScanState.STATE_IDLE) {
                BleLog.w("scan action already exists, complete the previous scan action first");
                if (imp != null) {
                    imp.onScanStarted(false);
                }
                return;
            }

            mBleScanPresenter.prepare(names, mac, fuzzy, needConnect, timeOut, imp);

            boolean success = BleManager.getInstance().getBluetoothAdapter()
                    .startLeScan(serviceUuids, mBleScanPresenter);
            mBleScanState = success ? BleScanState.STATE_SCANNING : BleScanState.STATE_IDLE;
            mBleScanPresenter.notifyScanStarted(success);
        }
    }

    @SuppressLint("MissingPermission")
    public void stopLeScan(boolean isCallbackScanFinish) {
        synchronized (this) {
            BleManager.getInstance().getBluetoothAdapter().stopLeScan(mBleScanPresenter);
            mBleScanState = BleScanState.STATE_IDLE;
            if (!isCallbackScanFinish) {
                mBleScanPresenter.removeHandlerMsg();
                mBleScanPresenter.setCancelBleScanListener();
                return;
            }
            mBleScanPresenter.notifyScanStopped();
        }
    }

    public BleScanState getScanState() {
        return mBleScanState;
    }


}
