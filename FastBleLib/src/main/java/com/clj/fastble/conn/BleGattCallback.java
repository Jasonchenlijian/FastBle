
package com.clj.fastble.conn;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;


public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onFoundDevice(ScanResult scanResult);

    public abstract void onConnecting(BluetoothGatt gatt, int status);

    public abstract void onConnectError(BleException exception);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onDisConnected(BleException exception);

}