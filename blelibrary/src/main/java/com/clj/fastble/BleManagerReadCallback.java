package com.clj.fastble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.clj.fastble.exception.BleException;

/**
 * Created by 陈利健 on 2016/8/17.
 * 蓝牙回调：读
 */
public interface BleManagerReadCallback {

    /**
     * 数据读成功
     * @param characteristic
     */
    void onDataReadSuccess(BluetoothGattCharacteristic characteristic);

    /**
     * 数据读失败
     * @param exception
     */
    void onDataReadFailure(BleException exception);
}
