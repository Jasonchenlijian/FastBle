
package com.clj.fastble.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;


public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onStartConnect();

    public abstract void onConnectFail(BleDevice bleDevice, BleException exception);

    public abstract void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status);

    public abstract void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status);

}