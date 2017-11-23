package com.clj.fastble.conn;


import com.clj.fastble.bluetooth.BleConnector;

public abstract class BleRssiCallback extends BleCallback {

    public abstract void onSuccess(int rssi);

    private BleConnector bleConnector;

    public void setBleConnector(BleConnector bleConnector) {
        this.bleConnector = bleConnector;
    }

    public BleConnector getBleConnector() {
        return bleConnector;
    }
}