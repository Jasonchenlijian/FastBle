package com.clj.fastble.data;


import android.bluetooth.BluetoothGatt;

import com.clj.fastble.callback.BleGattCallback;

public class BleConnectStateParameter {

    private BleGattCallback callback;
    private BluetoothGatt gatt;
    private int status;
    private boolean isAcitive;
    private BleDevice bleDevice;


    public BleConnectStateParameter(BleGattCallback callback, BluetoothGatt gatt, int status) {
        this.callback = callback;
        this.gatt = gatt;
        this.status = status;
    }

    public BleGattCallback getCallback() {
        return callback;
    }

    public void setCallback(BleGattCallback callback) {
        this.callback = callback;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isAcitive() {
        return isAcitive;
    }

    public void setAcitive(boolean acitive) {
        isAcitive = acitive;
    }

    public BleDevice getBleDevice() {
        return bleDevice;
    }

    public void setBleDevice(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }
}
