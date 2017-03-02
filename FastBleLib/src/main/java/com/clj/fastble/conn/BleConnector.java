
package com.clj.fastble.conn;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.exception.InitiatedException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.utils.BleLog;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ble Device Connector.
 * be sure main thread
 */
public class BleConnector {
    private static final String TAG = BleConnector.class.getSimpleName();
    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int MSG_WRITE_CHA = 1;
    private static final int MSG_WRIATE_DES = 2;
    private static final int MSG_READ_CHA = 3;
    private static final int MSG_READ_DES = 4;
    private static final int MSG_READ_RSSI = 5;
    private static final int MSG_NOTIFY_CHA = 6;
    private static final int MSG_NOTIY_DES = 7;
    private static final int MSG_INDICATE_DES = 8;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor descriptor;
    private BleBluetooth bleBluetooth;
    private int timeOutMillis = 20000;
    private Handler handler = new MyHandler();

    private static final class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            BleCallback call = (BleCallback) msg.obj;
            if (call != null) {
                call.onFailure(BleException.TIMEOUT_EXCEPTION);
            }
            msg.obj = null;
        }
    }

    public BleConnector(BleBluetooth bleBluetooth) {
        this.bleBluetooth = bleBluetooth;
        this.bluetoothGatt = bleBluetooth.getBluetoothGatt();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public BleConnector(BleBluetooth bleBluetooth, BluetoothGattService service,
                        BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
        this(bleBluetooth);
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
    }

    public BleConnector(BleBluetooth bleBluetooth,
                        UUID serviceUUID, UUID charactUUID,
                        UUID descriptorUUID, UUID client_characteristic_conifgUUID) {
        this(bleBluetooth);
        withUUID(serviceUUID, charactUUID, descriptorUUID);
    }

    public BleConnector(BleBluetooth bleBluetooth,
                        String serviceUUID, String charactUUID,
                        String descriptorUUID, String client_characteristic_conifgUUID) {
        this(bleBluetooth);
        withUUIDString(serviceUUID, charactUUID, descriptorUUID);
    }


    public BleConnector withUUID(UUID serviceUUID, UUID charactUUID, UUID descriptorUUID) {

        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        }

        if (service != null && charactUUID != null) {
            characteristic = service.getCharacteristic(charactUUID);
        }

        if (characteristic != null && descriptorUUID != null) {
            descriptor = characteristic.getDescriptor(descriptorUUID);
        }

        return this;
    }

    public BleConnector withUUIDString(String serviceUUID, String charactUUID,
                                       String descriptorUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(charactUUID), formUUID(descriptorUUID));
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }


    /***************************************main operation************************************************/

    /**
     * notify
     */
    public boolean enableCharacteristicNotify(BleCharacterCallback bleCallback, String uuid_notify) {

        if (getCharacteristic() != null
                && (getCharacteristic().getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + getCharacteristic().getProperties());

            handleCharacteristicNotificationCallback(bleCallback, uuid_notify);

            return setCharacteristicNotification(getBluetoothGatt(), getCharacteristic(), true);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support notify!"));
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

            return setCharacteristicNotification(getBluetoothGatt(), getCharacteristic(), false);
        } else {
            return false;
        }
    }

    /**
     * notify setting
     */
    private boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  boolean enable) {
        if (gatt == null || characteristic == null) {
            BleLog.w(TAG, "gatt or characteristic equal null");
            return false;
        }

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            BleLog.w(TAG, "Check characteristic property: false");
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicNotification:" + enable
                + "\nsuccess： " + success
                + "\ncharacteristic.getUuid():" + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
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

            return setCharacteristicIndication(getBluetoothGatt(), getCharacteristic(), true);

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

            return setCharacteristicIndication(getBluetoothGatt(), getCharacteristic(), false);

        } else {
            return false;
        }
    }

    /**
     * indicate setting
     */
    private boolean setCharacteristicIndication(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                boolean enable) {
        if (gatt == null || characteristic == null) {
            BleLog.w(TAG, "gatt or characteristic equal null");
            return false;
        }

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            Log.w(TAG, "check characteristic property: false");
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicIndication:" + enable
                + "\nsuccess:" + success
                + "\ncharacteristic.getUuid():" + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }


    /**
     * write
     */
    public boolean writeCharacteristic(byte[] data, BleCharacterCallback bleCallback, String uuid_write) {
        if (data == null)
            return false;

        if (getCharacteristic() == null
                || (getCharacteristic().getProperties()
                & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support write!"));
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

            setCharacteristicNotification(getBluetoothGatt(), getCharacteristic(), false);
            handleCharacteristicReadCallback(bleCallback, uuid_read);

            return handleAfterInitialed(getBluetoothGatt().readCharacteristic(getCharacteristic()), bleCallback);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("this characteristic not support read!"));
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

            listenAndTimer(bleCallback, MSG_NOTIFY_CHA, uuid_notify, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {

                    if (!msgRemoved.getAndSet(true)) {
                        handler.removeMessages(MSG_NOTIFY_CHA, this);
                    }
                    if (characteristic.getUuid().equals(UUID.fromString(uuid_notify))) {
                        bleCallback.onSuccess(characteristic);
                    }
                }
            });
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicationCallback(final BleCharacterCallback bleCallback,
                                                        final String uuid_indicate) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_INDICATE_DES, uuid_indicate, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {

                    if (!msgRemoved.getAndSet(true)) {
                        handler.removeMessages(MSG_INDICATE_DES, this);
                    }
                    if (characteristic.getUuid().equals(UUID.fromString(uuid_indicate))) {
                        bleCallback.onSuccess(characteristic);
                    }
                }
            });
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(final BleCharacterCallback bleCallback,
                                                   final String uuid_write) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_WRITE_CHA, uuid_write, new BluetoothGattCallback() {
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic, int status) {
                    handler.removeMessages(MSG_WRITE_CHA, this);

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (characteristic.getUuid().equals(UUID.fromString(uuid_write))) {
                            bleCallback.onSuccess(characteristic);
                        }
                    } else {
                        bleCallback.onFailure(new GattException(status));
                    }
                }
            });
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(final BleCharacterCallback bleCallback,
                                                  final String uuid_read) {
        if (bleCallback != null) {
            listenAndTimer(bleCallback, MSG_READ_CHA, uuid_read, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic, int status) {
                    if (!msgRemoved.getAndSet(true)) {
                        handler.removeMessages(MSG_READ_CHA, this);
                    }
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (characteristic.getUuid().equals(UUID.fromString(uuid_read))) {
                            bleCallback.onSuccess(characteristic);
                        }
                    } else {
                        bleCallback.onFailure(new GattException(status));
                    }
                }
            });
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(final BleRssiCallback bleCallback) {

        if (bleCallback != null) {
            listenAndTimer(bleCallback, MSG_READ_RSSI, BleBluetooth.READ_RSSI_KEY, new BluetoothGattCallback() {
                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    handler.removeMessages(MSG_READ_RSSI, this);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        bleCallback.onSuccess(rssi);
                    } else {
                        bleCallback.onFailure(new GattException(status));
                    }
                }
            });
        }
    }

    private boolean handleAfterInitialed(boolean initiated, BleCallback bleCallback) {
        if (bleCallback != null) {

            BleLog.d(TAG, "initiated: " + initiated);

            if (initiated) {
                bleCallback.onInitiatedSuccess();
            } else {
                bleCallback.onFailure(new InitiatedException());
            }
        }
        return initiated;
    }


    /**
     * listen bleBluetooth gatt callback, and send a delayed message.
     */
    private void listenAndTimer(final BleCallback bleCallback, int what, String uuid, BluetoothGattCallback callback) {
        bleCallback.setBluetoothGattCallback(callback);
        bleBluetooth.addGattCallback(uuid, callback);

        Message msg = handler.obtainMessage(what, bleCallback);
        handler.sendMessageDelayed(msg, timeOutMillis);
    }


    /*****************************
     * getter and setter
     ***********************************/

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

    public BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }

    public BleConnector setDescriptor(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
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
