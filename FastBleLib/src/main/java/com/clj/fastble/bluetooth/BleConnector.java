
package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.clj.fastble.conn.BleCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleRssiCallback;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;
import com.clj.fastble.utils.BleLog;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;
import java.util.UUID;

/**
 * Ble Device Connector.
 * be sure main thread
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleConnector {

    private static final String TAG = BleConnector.class.getSimpleName();
    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int MSG_NOTIFY_CHA = 0x014;
    private static final int MSG_INDICATE_DES = 0x015;
    private static final int MSG_WRITE_CHA = 0x01;
    private static final int MSG_READ_CHA = 0x012;
    private static final int MSG_READ_RSSI = 0x013;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BleBluetooth bleBluetooth;
    private static int timeOutMillis = 10000;
    private Handler handler = new MyHandler();

    private static final class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_NOTIFY_CHA:
                    BleCharacterCallback notifyCallback = (BleCharacterCallback) msg.obj;
                    if (notifyCallback != null)
                        notifyCallback.onFailure(new TimeoutException());
                    break;

                case MSG_INDICATE_DES:
                    BleCharacterCallback indicateCallback = (BleCharacterCallback) msg.obj;
                    if (indicateCallback != null)
                        indicateCallback.onFailure(new TimeoutException());
                    break;

                case MSG_WRITE_CHA:
                    BleCharacterCallback writeCallback = (BleCharacterCallback) msg.obj;
                    if (writeCallback != null)
                        writeCallback.onFailure(new TimeoutException());
                    break;

                case MSG_READ_CHA:
                    BleCharacterCallback readCallback = (BleCharacterCallback) msg.obj;
                    if (readCallback != null)
                        readCallback.onFailure(new TimeoutException());
                    break;
            }

            BleCallback call = (BleCallback) msg.obj;
            if (call != null) {
                call.onFailure(new TimeoutException());
            }
            msg.obj = null;
        }
    }

    public BleConnector(BleBluetooth bleBluetooth) {
        this.bleBluetooth = bleBluetooth;
        this.bluetoothGatt = bleBluetooth.getBluetoothGatt();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public BleConnector withUUID(UUID serviceUUID, UUID charactUUID) {

        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        }

        if (service != null && charactUUID != null) {
            characteristic = service.getCharacteristic(charactUUID);
        }

        return this;
    }

    public BleConnector withUUIDString(String serviceUUID, String charactUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(charactUUID));
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }




     /*------------------------------- main operation ----------------------------------- */


    /**
     * notify
     */
    public boolean enableCharacteristicNotify(BleCharacterCallback bleCallback, String uuid_notify) {

        if (getCharacteristic() != null
                && (getCharacteristic().getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + getCharacteristic().getProperties());

            handleCharacteristicNotificationCallback(bleCallback, uuid_notify);

            return setCharacteristicNotification(getBluetoothGatt(), getCharacteristic(), true, bleCallback);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support notify!"));
                bleCallback.onInitiatedResult(false);
            }
            return false;
        }
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify() {
        if (getCharacteristic() != null
                && (getCharacteristic().getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + getCharacteristic().getProperties());

            return setCharacteristicNotification(getBluetoothGatt(), getCharacteristic(), false, null);
        } else {
            return false;
        }
    }

    /**
     * notify setting
     */
    private boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  boolean enable,
                                                  BleCharacterCallback bleCallback) {
        if (gatt == null || characteristic == null) {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("gatt or characteristic equal null"));
                bleCallback.onInitiatedResult(false);
            }
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicNotification: " + enable
                + "\nsuccess: " + success
                + "\ncharacteristic.getUuid(): " + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (bleCallback != null) {
                bleCallback.onInitiatedResult(success2);
            }
            return success2;
        }
        if (bleCallback != null) {
            bleCallback.onFailure(new OtherException("notify operation failed"));
            bleCallback.onInitiatedResult(false);
        }
        return false;
    }

    /**
     * indicate
     */
    public boolean enableCharacteristicIndicate(BleCharacterCallback bleCallback, String uuid_indicate) {
        if (getCharacteristic() != null
                && (getCharacteristic().getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + getCharacteristic().getProperties());

            handleCharacteristicIndicationCallback(bleCallback, uuid_indicate);

            return setCharacteristicIndication(getBluetoothGatt(), getCharacteristic(), true, bleCallback);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support indicate!"));
            }
            return false;
        }
    }


    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate() {
        if (getCharacteristic() != null
                && (getCharacteristic().getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + getCharacteristic().getProperties());

            return setCharacteristicIndication(getBluetoothGatt(), getCharacteristic(), false, null);

        } else {
            return false;
        }
    }

    /**
     * indicate setting
     */
    private boolean setCharacteristicIndication(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                boolean enable,
                                                BleCharacterCallback bleCallback) {
        if (gatt == null || characteristic == null) {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("gatt or characteristic equal null"));
                bleCallback.onInitiatedResult(false);
            }
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicIndication:" + enable
                + "\nsuccess:" + success
                + "\ncharacteristic.getUuid():" + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (bleCallback != null) {
                bleCallback.onInitiatedResult(success2);
            }
            return success2;
        }
        if (bleCallback != null) {
            bleCallback.onFailure(new OtherException("indicate operation failed"));
            bleCallback.onInitiatedResult(false);
        }
        return false;
    }


    /**
     * write
     */
    public boolean writeCharacteristic(byte[] data, BleCharacterCallback bleCallback, String uuid_write) {
        if (data == null) {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("the data to be written is empty"));
                bleCallback.onInitiatedResult(false);
            }
            return false;
        }

        if (getCharacteristic() == null
                || (getCharacteristic().getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support write!"));
                bleCallback.onInitiatedResult(false);
            }
            return false;
        }

        BleLog.d(TAG, getCharacteristic().getUuid()
                + "\ncharacteristic.getProperties():" + getCharacteristic().getProperties()
                + "\ncharacteristic.getValue(): " + Arrays.toString(getCharacteristic().getValue())
                + "\ncharacteristic write bytes: " + Arrays.toString(data)
                + "\nhex: " + HexUtil.encodeHexStr(data));

        handleCharacteristicWriteCallback(bleCallback, uuid_write);
        getCharacteristic().setValue(data);
        return handleAfterInitialed(getBluetoothGatt().writeCharacteristic(getCharacteristic()), bleCallback);
    }

    /**
     * read
     */
    public boolean readCharacteristic(BleCharacterCallback bleCallback, String uuid_read) {
        if (getCharacteristic() != null
                && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            BleLog.d(TAG, getCharacteristic().getUuid()
                    + "\ncharacteristic.getProperties(): " + getCharacteristic().getProperties()
                    + "\ncharacteristic.getValue(): " + Arrays.toString(getCharacteristic().getValue()));

            setCharacteristicNotification(getBluetoothGatt(), getCharacteristic(), false, bleCallback);
            handleCharacteristicReadCallback(bleCallback, uuid_read);
            return handleAfterInitialed(getBluetoothGatt().readCharacteristic(getCharacteristic()), bleCallback);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support read!"));
                bleCallback.onInitiatedResult(false);
            }
            return false;
        }
    }

    /**
     * rssi
     */
    public boolean readRemoteRssi(BleRssiCallback bleCallback) {
        handleRSSIReadCallback(bleCallback);
        return handleAfterInitialed(getBluetoothGatt().readRemoteRssi(), bleCallback);
    }


    /**************************************** handle call back ******************************************/

    /**
     * notify
     */
    private void handleCharacteristicNotificationCallback(final BleCharacterCallback bleCallback,
                                                          final String uuid_notify) {
        if (bleCallback != null) {
            handler.removeMessages(MSG_NOTIFY_CHA, this);
            bleCallback.setBleConnector(this);
            bleBluetooth.addCharacterCallback(uuid_notify, bleCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_NOTIFY_CHA, bleCallback), timeOutMillis);
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicationCallback(final BleCharacterCallback bleCallback,
                                                        final String uuid_indicate) {
        if (bleCallback != null) {

            handler.removeMessages(MSG_INDICATE_DES, this);
            bleCallback.setBleConnector(this);
            bleBluetooth.addCharacterCallback(uuid_indicate, bleCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_INDICATE_DES, bleCallback), timeOutMillis);
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(BleCharacterCallback bleCallback,
                                                   String uuid_write) {
        if (bleCallback != null) {
            handler.removeMessages(MSG_WRITE_CHA, this);
            bleCallback.setBleConnector(this);
            bleBluetooth.addCharacterCallback(uuid_write, bleCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_WRITE_CHA, bleCallback), timeOutMillis);
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(final BleCharacterCallback bleCallback,
                                                  final String uuid_read) {
        if (bleCallback != null) {
            handler.removeMessages(MSG_READ_CHA, this);
            bleCallback.setBleConnector(this);
            bleBluetooth.addCharacterCallback(uuid_read, bleCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_READ_CHA, bleCallback), timeOutMillis);
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(final BleRssiCallback bleCallback) {
        if (bleCallback != null) {
            handler.removeMessages(MSG_READ_RSSI, this);
            bleCallback.setBleConnector(this);
            bleBluetooth.addRssiCallback(bleCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_READ_RSSI, bleCallback), timeOutMillis);
        }
    }

    private boolean handleAfterInitialed(boolean initiated, BleCallback bleCallback) {
        if (bleCallback != null) {
            if (!initiated) {
                bleCallback.onFailure(new OtherException("write or read operation failed"));
            }
            bleCallback.onInitiatedResult(initiated);
        }
        return initiated;
    }

    public void notifySuccess() {
        handler.removeMessages(MSG_NOTIFY_CHA, this);
    }

    public void indicateSuccess() {
        handler.removeMessages(MSG_INDICATE_DES, this);
    }

    public void writeSuccess() {
        handler.removeMessages(MSG_WRITE_CHA, this);
    }

    public void readSuccess() {
        handler.removeMessages(MSG_READ_CHA, this);
    }

    public void rssiSuccess() {
        handler.removeMessages(MSG_READ_RSSI, this);
    }






    /*------------------------------- getter and setter ----------------------------------- */


    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public BleConnector setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
        return this;
    }

    public BluetoothGattService getService() {
        return service;
    }

    public BleConnector setService(BluetoothGattService service) {
        this.service = service;
        return this;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public BleConnector setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
        return this;
    }

    public int getTimeOutMillis() {
        return timeOutMillis;
    }

    public BleConnector setTimeOutMillis(int timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
        return this;
    }
}
