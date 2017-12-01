package com.clj.fastble.scan;


import com.clj.fastble.data.BleDevice;

import java.util.List;

public interface IBleScanPresenter {

    void onScanStarted(boolean success);

    void onScanning(BleDevice bleDevice);

    void onScanFinished(List<BleDevice> bleDeviceList);

    void notifyScanStarted(boolean success);

    void notifyScanStopped();

}
