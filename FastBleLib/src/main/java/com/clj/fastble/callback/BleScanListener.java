package com.clj.fastble.callback;

import com.clj.fastble.data.BleDevice;

public interface BleScanListener {

    void onScanStarted(boolean success);

    void onScanning(BleDevice bleDevice);

}
