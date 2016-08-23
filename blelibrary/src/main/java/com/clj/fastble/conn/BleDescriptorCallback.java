package com.clj.fastble.conn;

import android.bluetooth.BluetoothGattDescriptor;

public abstract class BleDescriptorCallback extends BleCallback {
    public abstract void onSuccess(BluetoothGattDescriptor descriptor);
}