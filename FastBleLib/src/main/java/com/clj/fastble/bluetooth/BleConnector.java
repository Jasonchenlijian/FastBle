
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

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;

import java.util.UUID;

/**
 * Ble Device Connector.
 * be sure main thread
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleConnector {

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int MSG_NOTIFY_CHA = 0x11;
    private static final int MSG_INDICATE_DES = 0x12;
    private static final int MSG_WRITE_CHA = 0x13;
    private static final int MSG_READ_CHA = 0x14;
    private static final int MSG_READ_RSSI = 0x15;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BleBluetooth bleBluetooth;
    private Handler handler = new MyHandler();

    private static final class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NOTIFY_CHA:
                    BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                    if (notifyCallback != null)
                        notifyCallback.onNotifyFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_INDICATE_DES:
                    BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                    if (indicateCallback != null)
                        indicateCallback.onIndicateFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_WRITE_CHA:
                    BleWriteCallback writeCallback = (BleWriteCallback) msg.obj;
                    if (writeCallback != null)
                        writeCallback.onWriteFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_READ_CHA:
                    BleReadCallback readCallback = (BleReadCallback) msg.obj;
                    if (readCallback != null)
                        readCallback.onReadFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
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
    public void enableCharacteristicNotify(BleNotifyCallback bleNotifyCallback, String uuid_notify) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (setCharacteristicNotification(bluetoothGatt, characteristic, true, bleNotifyCallback)) {
                handleCharacteristicNotificationCallback(bleNotifyCallback, uuid_notify);
            }
        } else {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("this characteristic not support notify!"));
        }
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify() {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicNotification(bluetoothGatt, characteristic, false, null);
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
                                                  BleNotifyCallback bleNotifyCallback) {
        if (gatt == null || characteristic == null) {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                if (bleNotifyCallback != null)
                    bleNotifyCallback.onNotifyFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * indicate
     */
    public void enableCharacteristicIndicate(BleIndicateCallback bleIndicateCallback, String uuid_indicate) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (setCharacteristicIndication(bluetoothGatt, characteristic, true, bleIndicateCallback)) {
                handleCharacteristicIndicationCallback(bleIndicateCallback, uuid_indicate);
            }
        } else {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("this characteristic not support indicate!"));
        }
    }


    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate() {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicIndication(bluetoothGatt, characteristic, false, null);
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
                                                BleIndicateCallback bleIndicateCallback) {
        if (gatt == null || characteristic == null) {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                if (bleIndicateCallback != null)
                    bleIndicateCallback.onIndicateFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * write
     */
    public void writeCharacteristic(byte[] data, BleWriteCallback bleWriteCallback, String uuid_write) {
        if (data == null || data.length <= 0) {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(new OtherException("the data to be written is empty"));
            return;
        }

        if (characteristic == null
                || (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(new OtherException("this characteristic not support write!"));
            return;
        }

        if (characteristic.setValue(data)) {
            if (bluetoothGatt.writeCharacteristic(characteristic)) {
                handleCharacteristicWriteCallback(bleWriteCallback, uuid_write);
            } else {
                if (bleWriteCallback != null)
                    bleWriteCallback.onWriteFailure(new OtherException("gatt writeCharacteristic fail"));
            }
        } else {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(new OtherException("Updates the locally stored value of this characteristic fail"));
        }
    }

    /**
     * read
     */
    public void readCharacteristic(BleReadCallback bleReadCallback, String uuid_read) {
        if (characteristic != null
                && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            if (bluetoothGatt.readCharacteristic(characteristic)) {
                handleCharacteristicReadCallback(bleReadCallback, uuid_read);
            } else {
                if (bleReadCallback != null)
                    bleReadCallback.onReadFailure(new OtherException("gatt readCharacteristic fail"));
            }
        } else {
            if (bleReadCallback != null)
                bleReadCallback.onReadFailure(new OtherException("this characteristic not support read!"));
        }
    }

    /**
     * rssi
     */
    public void readRemoteRssi(BleRssiCallback bleRssiCallback) {
        if (bluetoothGatt.readRemoteRssi()) {
            handleRSSIReadCallback(bleRssiCallback);
        } else {
            if (bleRssiCallback != null)
                bleRssiCallback.onRssiFailure(new OtherException("gatt readRemoteRssi fail"));
        }
    }


    /**************************************** Handle call back ******************************************/

    /**
     * notify
     */
    private void handleCharacteristicNotificationCallback(BleNotifyCallback bleNotifyCallback,
                                                          String uuid_notify) {
        if (bleNotifyCallback != null) {
            handler.removeMessages(MSG_NOTIFY_CHA, this);
            bleNotifyCallback.setBleConnector(this);
            bleNotifyCallback.setKey(uuid_notify);
            bleBluetooth.addNotifyCallback(uuid_notify, bleNotifyCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_NOTIFY_CHA, bleNotifyCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicationCallback(BleIndicateCallback bleIndicateCallback,
                                                        String uuid_indicate) {
        if (bleIndicateCallback != null) {
            handler.removeMessages(MSG_INDICATE_DES, this);
            bleIndicateCallback.setBleConnector(this);
            bleIndicateCallback.setKey(uuid_indicate);
            bleBluetooth.addIndicateCallback(uuid_indicate, bleIndicateCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_INDICATE_DES, bleIndicateCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(BleWriteCallback bleWriteCallback,
                                                   String uuid_write) {
        if (bleWriteCallback != null) {
            handler.removeMessages(MSG_WRITE_CHA, this);
            bleWriteCallback.setBleConnector(this);
            bleWriteCallback.setKey(uuid_write);
            bleBluetooth.addWriteCallback(uuid_write, bleWriteCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_WRITE_CHA, bleWriteCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(BleReadCallback bleReadCallback,
                                                  String uuid_read) {
        if (bleReadCallback != null) {
            handler.removeMessages(MSG_READ_CHA, this);
            bleReadCallback.setBleConnector(this);
            bleReadCallback.setKey(uuid_read);
            bleBluetooth.addReadCallback(uuid_read, bleReadCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_READ_CHA, bleReadCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(final BleRssiCallback bleRssiCallback) {
        if (bleRssiCallback != null) {
            handler.removeMessages(MSG_READ_RSSI, this);
            bleRssiCallback.setBleConnector(this);
            bleBluetooth.addRssiCallback(bleRssiCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_READ_RSSI, bleRssiCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
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


}
