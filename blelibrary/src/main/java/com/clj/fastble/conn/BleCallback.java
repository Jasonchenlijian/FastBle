
package com.clj.fastble.conn;

import android.bluetooth.BluetoothGattCallback;
import com.clj.fastble.exception.BleException;

public abstract class BleCallback {
    private BluetoothGattCallback bluetoothGattCallback;

    protected BleCallback setBluetoothGattCallback(BluetoothGattCallback bluetoothGattCallback) {
        this.bluetoothGattCallback = bluetoothGattCallback;
        return this;
    }

    protected BluetoothGattCallback getBluetoothGattCallback() {
        return bluetoothGattCallback;
    }

    public void onInitiatedSuccess() {
    }

    public abstract void onFailure(BleException exception);
}