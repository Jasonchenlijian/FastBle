package com.clj.fastble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.clj.fastble.exception.BleException;

/**
 * Created by 陈利健 on 2016/8/17.
 * 蓝牙回调：写
 */
public interface BleManagerWriteCallback {

    /**
     * 数据写成功
     * @param characteristic
     */
    void onDataWriteSuccess(BluetoothGattCharacteristic characteristic);

    /**
     * 数据写失败
     * @param exception
     */
    void onDataWriteFailure(BleException exception);
}
