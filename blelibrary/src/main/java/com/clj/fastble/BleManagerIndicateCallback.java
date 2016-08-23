package com.clj.fastble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.clj.fastble.exception.BleException;

/**
 * Created by 陈利健 on 2016/8/17.
 * 蓝牙回调： 监听Indicate
 */
public interface BleManagerIndicateCallback {

    /**
     * Indicate 数据成功回调
     * @param characteristic
     */
    void onIndicateDataChangeSuccess(BluetoothGattCharacteristic characteristic);

    /**
     * Indicate 失败回调
     * @param exception
     */
    void onIndicateDataChangeFailure(BleException exception);
}
