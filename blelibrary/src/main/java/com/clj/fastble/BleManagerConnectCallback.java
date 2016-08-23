package com.clj.fastble;

import android.bluetooth.BluetoothGatt;

import com.clj.fastble.exception.BleException;

/**
 * Created by 陈利健 on 2016/8/17.
 * 蓝牙回调： 扫描、连接
 */
public interface BleManagerConnectCallback {

    /**
     * 连接成功
     * @param gatt
     * @param status
     */
    void onConnectSuccess(BluetoothGatt gatt, int status);

    /**
     * 服务发现
     * @param gatt
     * @param status
     */
    void onServicesDiscovered(BluetoothGatt gatt, int status);

    /**
     * 连接断开
     * @param exception
     */
    void onConnectFailure(BleException exception);

}
