package com.clj.fastble.callback;


import com.clj.fastble.bluetooth.BleConnector;
import com.clj.fastble.exception.BleException;

public abstract class BleRssiCallback {

    public abstract void onRssiFailure(BleException exception);

    public abstract void onRssiSuccess(int rssi);

    private BleConnector bleConnector;

    public void setBleConnector(BleConnector bleConnector) {
        this.bleConnector = bleConnector;
    }

    public BleConnector getBleConnector() {
        return bleConnector;
    }
}