package com.clj.fastble.callback;


import com.clj.fastble.data.BleDevice;

import java.util.List;

public abstract class BleScanCallback implements BleScanListener {

    public abstract void onScanFinished(List<BleDevice> scanResultList);

    public void onLeScan(BleDevice bleDevice) {
    }
}
