package com.clj.fastble.callback;


import com.clj.fastble.data.BleDevice;

import java.util.List;

public abstract class BleScanCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(BleDevice result);

    public abstract void onScanFinished(List<BleDevice> scanResultList);
}
