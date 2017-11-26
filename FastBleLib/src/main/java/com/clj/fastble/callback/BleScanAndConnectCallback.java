package com.clj.fastble.callback;


import com.clj.fastble.data.BleDevice;

public abstract class BleScanAndConnectCallback extends BleGattCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanFinished(BleDevice scanResult);

}
