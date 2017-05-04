
package com.clj.fastble.conn;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;


public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onNotFoundDevice();

    public abstract void onFoundDevice(ScanResult scanResult);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onConnectFailure(BleException exception);

}