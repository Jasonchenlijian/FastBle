package com.clj.fastble.scan;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleScanPresenterImp;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.utils.BleLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleScanner {

    public static BleScanner getInstance() {
        return BleScannerHolder.sBleScanner;
    }

    private static class BleScannerHolder {
        private static final BleScanner sBleScanner = new BleScanner();
    }

    private BleScanState mBleScanState = BleScanState.STATE_IDLE;
    private ScanCallback mScanCallback;

    private final BleScanPresenter mBleScanPresenter = new BleScanPresenter() {

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

    @SuppressLint("MissingPermission")
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

        BluetoothLeScanner scanner = BleManager.getInstance().getBluetoothAdapter().getBluetoothLeScanner();
        if (scanner == null) {
            mBleScanState = BleScanState.STATE_IDLE;
            mBleScanPresenter.notifyScanStarted(false);
            return;
        }

        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                byte[] scanRecord = null;
                if (result.getScanRecord() != null) {
                    scanRecord = result.getScanRecord().getBytes();
                }
                mBleScanPresenter.handleScanResult(device, result.getRssi(), scanRecord);
            }

            @Override
            public void onScanFailed(int errorCode) {
                BleLog.e("onScanFailed, errorCode: " + errorCode);
            }
        };

        List<ScanFilter> filters = new ArrayList<>();
        if (serviceUuids != null && serviceUuids.length > 0) {
            for (UUID uuid : serviceUuids) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(uuid))
                        .build();
                filters.add(filter);
            }
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanner.startScan(filters, settings, mScanCallback);
        mBleScanState = BleScanState.STATE_SCANNING;
        mBleScanPresenter.notifyScanStarted(true);
    }

    @SuppressLint("MissingPermission")
    public synchronized void stopLeScan() {
        if (mScanCallback != null) {
            BluetoothLeScanner scanner = BleManager.getInstance().getBluetoothAdapter().getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(mScanCallback);
            }
            mScanCallback = null;
        }
        mBleScanState = BleScanState.STATE_IDLE;
        mBleScanPresenter.notifyScanStopped();
    }

    public BleScanState getScanState() {
        return mBleScanState;
    }

}
