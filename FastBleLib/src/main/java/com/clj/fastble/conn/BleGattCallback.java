
package com.clj.fastble.conn;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleGattCallback extends BluetoothGattCallback {

    public abstract void onScanStarted();

    public abstract void onFoundDevice(ScanResult scanResult);

    public abstract void onConnecting(BluetoothGatt gatt, int status);

    public abstract void onConnectError(BleException exception);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onDisConnected(BluetoothGatt gatt, int status, boolean isActive);

}