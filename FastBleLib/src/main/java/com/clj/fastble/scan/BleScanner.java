package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleScanPresenterImp;
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

    private BleScanPresenter mBleScanPresenter = new BleScanPresenter() {

        @Override
        public void onScanStarted(boolean success) {
            BleScanPresenterImp callback = mBleScanPresenter.getBleScanPresenterImp();
            if (callback != null) {
                callback.onScanStarted(success);
            }
        }

        @Override
        public void onLeScan(BleDevice bleDevice) {
            if (mBleScanPresenter.ismNeedConnect()) {
                BleScanAndConnectCallback callback = (BleScanAndConnectCallback)
                        mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onLeScan(bleDevice);
                }
            } else {
                BleScanCallback callback = (BleScanCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onLeScan(bleDevice);
                }
            }
        }

        @Override
        public void onScanning(BleDevice result) {
            BleScanPresenterImp callback = mBleScanPresenter.getBleScanPresenterImp();
            if (callback != null) {
                callback.onScanning(result);
            }
        }

        @Override
        public void onScanFinished(List<BleDevice> bleDeviceList) {
            if (mBleScanPresenter.ismNeedConnect()) {
                final BleScanAndConnectCallback callback = (BleScanAndConnectCallback)
                        mBleScanPresenter.getBleScanPresenterImp();
                if (bleDeviceList == null || bleDeviceList.size() < 1) {
                    if (callback != null) {
                        callback.onScanFinished(null);
                    }
                } else {
                    if (callback != null) {
                        callback.onScanFinished(bleDeviceList.get(0));
                    }
                    final List<BleDevice> list = bleDeviceList;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BleManager.getInstance().connect(list.get(0), callback);
                        }
                    }, 100);
                }
            } else {
                BleScanCallback callback = (BleScanCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onScanFinished(bleDeviceList);
                }
            }
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

    private synchronized void startLeScan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                                          boolean needConnect, long timeOut, BleScanPresenterImp imp) {

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

    public synchronized void stopLeScan() {
        BleManager.getInstance().getBluetoothAdapter().stopLeScan(mBleScanPresenter);
        mBleScanState = BleScanState.STATE_IDLE;
        mBleScanPresenter.notifyScanStopped();
    }

    public BleScanState getScanState() {
        return mBleScanState;
    }


}
