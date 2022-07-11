package com.clj.fastble.callback;


import com.clj.fastble.data.BleDevice;

public abstract class BleScanAndConnectCallback extends BleGattCallback implements BleScanListener {

    public abstract void onScanFinished(BleDevice scanResult);

    public void onLeScan(BleDevice bleDevice) {
    }

}
