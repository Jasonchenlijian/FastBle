package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;

import java.util.ArrayList;
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

    private BleScanPresenterJellyBean bleScanPresenterJellyBean;
    private BleScanState scanState = BleScanState.STATE_IDLE;

    public void scan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                     long timeOut, final BleScanCallback callback) {

        startLeScan(serviceUuids, new BleScanPresenterJellyBean(names, mac, fuzzy, false, timeOut) {
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

    private synchronized void startLeScan(UUID[] serviceUuids, BleScanPresenterJellyBean callback) {
        if (callback == null)
            return;
        this.bleScanPresenterJellyBean = callback;
        boolean success = BleManager.getInstance().getBluetoothAdapter().startLeScan(serviceUuids, bleScanPresenterJellyBean);
        if (success) {
            scanState = BleScanState.STATE_SCANNING;
            bleScanPresenterJellyBean.notifyScanStarted(true);
        } else {
            bleScanPresenterJellyBean.notifyScanStarted(false);
            callback.removeHandlerMsg();
        }
    }

    public synchronized void stopLeScan() {
        if (bleScanPresenterJellyBean == null)
            return;

        BleManager.getInstance().getBluetoothAdapter().stopLeScan(bleScanPresenterJellyBean);
        bleScanPresenterJellyBean.notifyScanStopped();
        bleScanPresenterJellyBean = null;

        if (scanState == BleScanState.STATE_SCANNING) {
            scanState = BleScanState.STATE_IDLE;
        }
    }

    public void scanAndConnect(UUID[] serviceUuids, String[] names, final String mac, boolean fuzzy,
                               long timeOut, final BleScanAndConnectCallback callback) {

        startLeScan(serviceUuids, new BleScanPresenterJellyBean(names, mac, fuzzy, true, timeOut) {

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
                        callback.onScanFinished(null);
                    }
                } else {
                    if (callback != null) {
                        callback.onScanFinished(bleDeviceList.get(0));
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


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scanOnLollipop() {
        BluetoothLeScanner bluetoothLeScanner = BleManager.getInstance().getBluetoothAdapter().getBluetoothLeScanner();
        ScanFilter scanFilter = new ScanFilter.Builder().build();
        List<ScanFilter> scanFilterList = new ArrayList<>();
        scanFilterList.add(scanFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().build();
        bluetoothLeScanner.startScan(scanFilterList, scanSettings, new ScanCallback() {});
    }




}
