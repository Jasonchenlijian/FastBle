package com.clj.fastble.exception.hanlder;

import com.clj.fastble.exception.BlueToothNotEnableException;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.exception.InitiatedException;
import com.clj.fastble.exception.NotFoundDeviceException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.ScanFailedException;
import com.clj.fastble.exception.TimeoutException;
import com.clj.fastble.utils.BleLog;

public class DefaultBleExceptionHandler extends BleExceptionHandler {

    private static final String TAG = "BleExceptionHandler";

    public DefaultBleExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onInitiatedException(InitiatedException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onNotFoundDeviceException(NotFoundDeviceException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onBlueToothNotEnableException(BlueToothNotEnableException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onScanFailedException(ScanFailedException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        BleLog.e(TAG, e.getDescription());
    }
}
