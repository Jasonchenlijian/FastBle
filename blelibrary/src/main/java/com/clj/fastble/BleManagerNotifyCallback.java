package com.clj.fastble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.clj.fastble.exception.BleException;

/**
 * Created by 陈利健 on 2016/8/17.
 * 蓝牙回调： 监听Notify
 */
public interface BleManagerNotifyCallback {

    /**
     * Notify 数据成功回调
     * @param characteristic
     */
    void onNotifyDataChangeSuccess(BluetoothGattCharacteristic characteristic);

    /**
     * Notify 失败回调
     * @param exception
     */
    void onNotifyDataChangeFailure(BleException exception);
}
