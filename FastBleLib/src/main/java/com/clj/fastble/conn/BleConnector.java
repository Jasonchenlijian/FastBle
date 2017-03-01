
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
import com.clj.fastble.log.BleLog;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ble Device Connector.
 * 确保在主线程中调用
 */
public class BleConnector {
    private static final String TAG = "BleConnector";
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
        return enableCharacteristicNotify(getCharacteristic(), bleCallback, uuid_notify);
    }

    /**
     * notify
     */
    public boolean enableCharacteristicNotify(BluetoothGattCharacteristic characteristic,
                                              BleCharacterCallback bleCallback, String uuid_notify) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + characteristic.getProperties());

            handleCharacteristicNotificationCallback(bleCallback, uuid_notify);

            return setCharacteristicNotification(getBluetoothGatt(), characteristic, true);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("Characteristic not support notify!"));
            }
            return false;
        }
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify() {
        return disableCharacteristicNotify(getCharacteristic());
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify(BluetoothGattCharacteristic characteristic) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + characteristic.getProperties());

            return setCharacteristicNotification(getBluetoothGatt(), characteristic, false);
        } else {
            return false;
        }
    }

    /**
     * indicate
     */
    public boolean enableCharacteristicIndicate(BleCharacterCallback bleCallback, String uuid_indicate) {
        return enableCharacteristicIndicate(getCharacteristic(), bleCallback, uuid_indicate);
    }

    /**
     * indicate
     */
    public boolean enableCharacteristicIndicate(BluetoothGattCharacteristic characteristic,
                                                BleCharacterCallback bleCallback, String uuid_indicate) {

        if (characteristic != null && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + characteristic.getProperties());

            handleCharacteristicIndicationCallback(bleCallback, uuid_indicate);

            return setCharacteristicIndication(getBluetoothGatt(), characteristic, true);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("Characteristic not support indicate!"));
            }
            return false;
        }
    }

    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate() {
        return disableCharacteristicIndicate(getCharacteristic());
    }

    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate(BluetoothGattCharacteristic characteristic) {
        if (characteristic != null && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + characteristic.getProperties());

            return setCharacteristicIndication(getBluetoothGatt(), characteristic, false);

        } else {
            return false;
        }
    }

    /**
     * write
     */
    public boolean writeCharacteristic(byte[] data, BleCharacterCallback bleCallback, String uuid_write) {
        if (data == null)
            return false;
        return writeCharacteristic(getCharacteristic(), data, bleCallback, uuid_write);
    }

    /**
     * write
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic character, byte[] data,
                                       final BleCharacterCallback bleCallback,
                                       final String uuid_write) {
        if (character == null
                || (character.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("Characteristic not support write!"));
            }
            return false;
        }

        BleLog.d(TAG, character.getUuid()
                + "\ncharacteristic.getProperties():" + character.getProperties()
                + "\n characteristic.getValue(): " + Arrays.toString(character.getValue())
                + "\n characteristic write bytes: " + Arrays.toString(data)
                + "\n hex: " + HexUtil.encodeHexStr(data));

        handleCharacteristicWriteCallback(bleCallback, uuid_write);

        character.setValue(data);

        return handleAfterInitialed(getBluetoothGatt().writeCharacteristic(character), bleCallback);
    }

    /**
     * read
     */
    public boolean readCharacteristic(BleCharacterCallback bleCallback, String uuid_read) {
        return readCharacteristic(getCharacteristic(), bleCallback, uuid_read);
    }

    /**
     * read
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic character,
                                      BleCharacterCallback bleCallback,
                                      final String uuid_read) {
        if (character != null
                && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            BleLog.d(TAG, character.getUuid()
                    + "\ncharacteristic.getProperties():" + character.getProperties()
                    + "\n characteristic.getValue(): " + Arrays.toString(character.getValue()));

            setCharacteristicNotification(getBluetoothGatt(), character, false);
            handleCharacteristicReadCallback(bleCallback, uuid_read);

            return handleAfterInitialed(getBluetoothGatt().readCharacteristic(character), bleCallback);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("Characteristic not support read!"));
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


    private boolean handleAfterInitialed(boolean initiated, BleCallback bleCallback) {
        if (bleCallback != null) {

            BleLog.d(TAG, "initiated： " + initiated);

            if (initiated) {
                bleCallback.onInitiatedSuccess();
            } else {
                bleCallback.onFailure(new InitiatedException());
            }
        }
        return initiated;
    }

    /**
     * notify
     */
    public boolean setCharacteristicNotification(BluetoothGatt gatt,
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
        BleLog.d(TAG, "setCharacteristicNotification----" + enable + "----success： " + success
                + '\n' + "characteristic.getUuid() :  " + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
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
    public boolean setCharacteristicIndication(BluetoothGatt gatt,
                                               BluetoothGattCharacteristic characteristic,
                                               boolean enable) {
        if (gatt == null || characteristic == null) {
            BleLog.w(TAG, "gatt or characteristic equal null");
            return false;
        }

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            Log.w(TAG, "Check characteristic property: false");
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicIndication----" + enable + "----success： " + success
                + '\n' + "characteristic.getUuid() :  " + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }


    /****************************************
     * handle call back
     ******************************************/

    private void handleCharacteristicNotificationCallback(final BleCharacterCallback bleCallback,
                                                          final String uuid_notify) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_NOTIFY_CHA, uuid_notify, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

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

    private void handleCharacteristicIndicationCallback(final BleCharacterCallback bleCallback,
                                                        final String uuid_indicate) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_INDICATE_DES, uuid_indicate, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

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
