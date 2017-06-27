package com.clj.fastble.exception.hanlder;

import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.BlueToothNotEnableException;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.exception.InitiatedException;
import com.clj.fastble.exception.NotFoundDeviceException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.ScanFailedException;
import com.clj.fastble.exception.TimeoutException;


public abstract class BleExceptionHandler {

    public BleExceptionHandler handleException(BleException exception) {

        if (exception != null) {

            if (exception instanceof ConnectException) {
                onConnectException((ConnectException) exception);

            } else if (exception instanceof GattException) {
                onGattException((GattException) exception);

            } else if (exception instanceof TimeoutException) {
                onTimeoutException((TimeoutException) exception);

            } else if (exception instanceof InitiatedException) {
                onInitiatedException((InitiatedException) exception);

            } else if (exception instanceof NotFoundDeviceException) {
                onNotFoundDeviceException((NotFoundDeviceException) exception);

            } else if (exception instanceof BlueToothNotEnableException) {
                onBlueToothNotEnableException((BlueToothNotEnableException) exception);

            } else if (exception instanceof ScanFailedException) {
                onScanFailedException((ScanFailedException) exception);

            } else {
                onOtherException((OtherException) exception);
            }
        }
        return this;
    }

    /**
     * connect failed
     */
    protected abstract void onConnectException(ConnectException e);

    /**
     * gatt error status
     */
    protected abstract void onGattException(GattException e);

    /**
     * operation timeout
     */
    protected abstract void onTimeoutException(TimeoutException e);

    /**
     * operation inititiated error
     */
    protected abstract void onInitiatedException(InitiatedException e);

    /**
     * not found device error
     */
    protected abstract void onNotFoundDeviceException(NotFoundDeviceException e);

    /**
     * bluetooth not enable error
     */
    protected abstract void onBlueToothNotEnableException(BlueToothNotEnableException e);

    /**
     * scan failed error
     */
    protected abstract void onScanFailedException(ScanFailedException e);

    /**
     * other exceptions
     */
    protected abstract void onOtherException(OtherException e);
}
