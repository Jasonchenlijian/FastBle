package com.clj.fastble.conn;

import android.bluetooth.BluetoothGattCharacteristic;

import com.clj.fastble.bluetooth.BleConnector;


public abstract class BleCharacterCallback extends BleCallback {


    public abstract void onSuccess(BluetoothGattCharacteristic characteristic);

    private String key;

    private BleConnector bleConnector;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setBleConnector(BleConnector bleConnector) {
        this.bleConnector = bleConnector;
    }

    public BleConnector getBleConnector() {
        return bleConnector;
    }
}