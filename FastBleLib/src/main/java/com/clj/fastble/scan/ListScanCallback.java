package com.clj.fastble.scan;

import android.bluetooth.BluetoothDevice;

import com.clj.fastble.data.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * scan for a period of time
 */
public abstract class ListScanCallback extends PeriodScanCallback {

    private List<ScanResult> resultList = new ArrayList<>();
    private AtomicBoolean hasFound = new AtomicBoolean(false);

    public ListScanCallback(long timeoutMillis) {
        super(timeoutMillis);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        ScanResult scanResult = new ScanResult(device, rssi, scanRecord,
                System.currentTimeMillis());

        synchronized (this) {
            hasFound.set(false);
            for (ScanResult result : resultList) {
                if (result.getDevice().equals(device)) {
                    hasFound.set(true);
                }
            }
            if (!hasFound.get()) {
                resultList.add(scanResult);
                onScanning(scanResult);
            }
        }
    }

    @Override
    public void onScanTimeout() {
        ScanResult[] results = new ScanResult[resultList.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = resultList.get(i);
        }
        onScanComplete(results);
    }

    @Override
    public void onScanCancel() {
        ScanResult[] resultArr = resultList.toArray(new ScanResult[resultList.size()]);
        onScanComplete(resultArr);
    }

    public abstract void onScanning(ScanResult result);

    public abstract void onScanComplete(ScanResult[] results);

}
