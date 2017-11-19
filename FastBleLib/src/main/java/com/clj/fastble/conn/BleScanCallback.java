package com.clj.fastble.conn;


import com.clj.fastble.data.ScanResult;

import java.util.List;

public abstract class BleScanCallback {

    public abstract void onScanStarted();

    public abstract void onScanning(ScanResult result);

    public abstract void onScanFinished(List<ScanResult> scanResultList);
}
