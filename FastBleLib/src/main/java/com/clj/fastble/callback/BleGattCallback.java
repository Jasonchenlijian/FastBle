
package com.clj.fastble.callback;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onFoundDevice(BleDevice scanResult);

    public abstract void onConnecting(BluetoothGatt gatt, int status);

    public abstract void onConnectError(BleException exception);

    public abstract void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status);

    public abstract void onDisConnected(boolean isActive, BluetoothGatt gatt, int status);

}