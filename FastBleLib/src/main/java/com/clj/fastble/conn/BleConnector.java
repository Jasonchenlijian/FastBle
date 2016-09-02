
package com.clj.fastble.conn;

import android.annotation.SuppressLint;
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

    public UUID CLIENT_CHARACTERISTIC_CONFIG;

    private static final int MSG_WRIATE_CHA = 1;
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
    private BleBluetooth liteBluetooth;
    private int timeOutMillis = 20000;
    private Handler handler = new MyHandler();

    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            BleLog.w("handleMessage: "
                    + "\nmsg:" + msg.what
                    + "\nobj:" + msg.obj);

            BleCallback call = (BleCallback) msg.obj;
            if (call != null) {
                liteBluetooth.removeGattCallback(call.getBluetoothGattCallback());
                call.onFailure(BleException.TIMEOUT_EXCEPTION);
            }
            msg.obj = null;
        }
    }

    public BleConnector(BleBluetooth liteBluetooth) {
        this.liteBluetooth = liteBluetooth;
        this.bluetoothGatt = liteBluetooth.getBluetoothGatt();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public BleConnector(BleBluetooth liteBluetooth, BluetoothGattService service,
                        BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
        this(liteBluetooth);
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
    }

    public BleConnector(BleBluetooth liteBluetooth,
                        UUID serviceUUID, UUID charactUUID,
                        UUID descriptorUUID, UUID client_characteristic_conifgUUID) {
        this(liteBluetooth);
        withUUID(serviceUUID, charactUUID, descriptorUUID, client_characteristic_conifgUUID);
    }

    public BleConnector(BleBluetooth liteBluetooth,
                        String serviceUUID, String charactUUID,
                        String descriptorUUID, String client_characteristic_conifgUUID) {
        this(liteBluetooth);
        withUUIDString(serviceUUID, charactUUID, descriptorUUID, client_characteristic_conifgUUID);
    }


    public BleConnector withUUID(UUID serviceUUID, UUID charactUUID,
                                 UUID descriptorUUID, UUID client_characteristic_conifgUUID) {

        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        }

        if (service != null && charactUUID != null) {
            characteristic = service.getCharacteristic(charactUUID);
        }

        if (characteristic != null && descriptorUUID != null) {
            descriptor = characteristic.getDescriptor(descriptorUUID);
        }

        CLIENT_CHARACTERISTIC_CONFIG = client_characteristic_conifgUUID;

        return this;
    }

    public BleConnector withUUIDString(String serviceUUID, String charactUUID,
                                       String descriptorUUID, String client_characteristic_conifgUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(charactUUID),
                formUUID(descriptorUUID), formUUID(client_characteristic_conifgUUID));
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }


    /***************************************main operation************************************************/

    /**
     * Notification
     */
    public boolean enableCharacteristicNotification(BleCharacterCallback bleCallback) {
        return enableCharacteristicNotification(getCharacteristic(), bleCallback);
    }

    /**
     * Notification
     */
    public boolean enableCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                    BleCharacterCallback bleCallback) {

        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "characteristic.getProperties():" + characteristic.getProperties());

            handleCharacteristicNotificationCallback(bleCallback);

            return setCharacteristicNotification(getBluetoothGatt(), characteristic, true);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("该特征值为空或不支持读写!"));
            }
            return false;
        }
    }

    /**
     * Indication
     */
    public boolean enableCharacteristicIndication(BleCharacterCallback bleCallback) {
        return enableCharacteristicIndication(getCharacteristic(), bleCallback);
    }

    /**
     * Indication
     */
    public boolean enableCharacteristicIndication(BluetoothGattCharacteristic characteristic,
                                                  BleCharacterCallback bleCallback) {

        if (characteristic != null && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BleLog.w(TAG, "charact.getProperties():" + characteristic.getProperties());

            handleCharacteristicIndicationCallback(bleCallback);

            return setCharacteristicIndication(getBluetoothGatt(), characteristic, true);

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("该特征值为空或不支持读写!"));
            }
            return false;
        }
    }

    /**
     * 写数据
     */
    public boolean writeCharacteristic(byte[] data, BleCharacterCallback bleCallback) {
        if (data == null)
            return false;
        return writeCharacteristic(getCharacteristic(), data, bleCallback);
    }

    /**
     * 向一个特征值characteristic写数据
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic character, byte[] data,
                                       final BleCharacterCallback bleCallback) {

        if (character == null) {
            Log.e(TAG, "getCharacteristic()为空！");
            return false;
        }

        BleLog.d(TAG, character.getUuid()
                + "\n characteristic.getValue(): " + Arrays.toString(character.getValue())
                + "\n characteristic write bytes: " + Arrays.toString(data)
                + "\n hex: " + HexUtil.encodeHexStr(data));

        handleCharacteristicWriteCallback(bleCallback);

        character.setValue(data);

        if ((character.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            BleLog.w(TAG, "Check characteristic 是否可写-------false"
                    + "\n getProperties(): " + characteristic.getProperties());
            return false;
        } else {
            BleLog.w(TAG, "Check characteristic 是否可写-------true"
                    + "\n getProperties(): " + characteristic.getProperties());
        }

        return handleAfterInitialed(getBluetoothGatt().writeCharacteristic(character), bleCallback);
    }

    /**
     * 读数据
     */
    public boolean readCharacteristic(BleCharacterCallback bleCallback) {
        return readCharacteristic(getCharacteristic(), bleCallback);
    }

    /**
     * 向一个特征值characteristic读数据
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic character, BleCharacterCallback bleCallback) {

        if (character == null) {
            Log.e(TAG, "getCharacteristic()为空！");
            return false;
        }
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            BleLog.w(TAG, "Check characteristic 是否可读-------false"
                    + "\n getProperties(): " + characteristic.getProperties());
            return false;
        } else {
            BleLog.w(TAG, "Check characteristic 是否可读-------true"
                    + "\n getProperties(): " + characteristic.getProperties());
        }

        if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            setCharacteristicNotification(getBluetoothGatt(), character, false);

            handleCharacteristicReadCallback(bleCallback);

            handleAfterInitialed(getBluetoothGatt().readCharacteristic(character), bleCallback);

            setCharacteristicNotification(getBluetoothGatt(), character, true);

            return true;

        } else {
            if (bleCallback != null) {
                bleCallback.onFailure(new OtherException("该特征值不支持读写!"));
            }
            return false;
        }
    }

    /**
     * {@link BleCallback#onInitiatedSuccess} will be called,
     * if the read operation was initiated successfully.
     * Otherwize {@link BleCallback#onFailure} will be called.
     *
     * @return true, if the read operation was initiated successfully
     */
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
     * notification
     */
    public boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean enable) {
        if (gatt == null || characteristic == null) {
            BleLog.w(TAG, "gatt或 characteristic为空");
            return false;
        }

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            Log.w(TAG, "Check characteristic property-----false");
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicNotification----" + enable + "是否成功： " + success
                + '\n' + "characteristic.getUuid() :  " + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    /**
     * indication
     */
    public boolean setCharacteristicIndication(BluetoothGatt gatt,
                                               BluetoothGattCharacteristic characteristic,
                                               boolean enable) {
        if (gatt == null || characteristic == null) {
            BleLog.w(TAG, "gatt或 characteristic为空");
            return false;
        }

        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            Log.w(TAG, "Check characteristic property-----false");
            return false;
        }

        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        BleLog.d(TAG, "setCharacteristicIndication----" + enable + "是否成功： " + success
                + '\n' + "characteristic.getUuid() :  " + characteristic.getUuid());

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
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

    private void handleCharacteristicNotificationCallback(final BleCharacterCallback bleCallback) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_NOTIFY_CHA, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                    if (!msgRemoved.getAndSet(true)) {
                        handler.removeMessages(MSG_NOTIFY_CHA, this);
                    }
                    bleCallback.onSuccess(characteristic);
                }
            });
        }
    }

    private void handleCharacteristicIndicationCallback(final BleCharacterCallback bleCallback) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_INDICATE_DES, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                    if (!msgRemoved.getAndSet(true)) {
                        handler.removeMessages(MSG_INDICATE_DES, this);
                    }
                    bleCallback.onSuccess(characteristic);
                }
            });
        }
    }

    private void handleCharacteristicWriteCallback(final BleCharacterCallback bleCallback) {
        if (bleCallback != null) {

            listenAndTimer(bleCallback, MSG_WRIATE_CHA, new BluetoothGattCallback() {
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic, int status) {
                    handler.removeMessages(MSG_WRIATE_CHA, this);

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        bleCallback.onSuccess(characteristic);
                    } else {
                        bleCallback.onFailure(new GattException(status));
                    }
                }
            });
        }
    }

    private void handleCharacteristicReadCallback(final BleCharacterCallback bleCallback) {
        if (bleCallback != null) {
            listenAndTimer(bleCallback, MSG_READ_CHA, new BluetoothGattCallback() {
                AtomicBoolean msgRemoved = new AtomicBoolean(false);

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic, int status) {
                    if (!msgRemoved.getAndSet(true)) {
                        handler.removeMessages(MSG_READ_CHA, this);
                    }
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        bleCallback.onSuccess(characteristic);
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
    private void listenAndTimer(final BleCallback bleCallback, int what, BluetoothGattCallback callback) {
        bleCallback.setBluetoothGattCallback(callback);
        liteBluetooth.addGattCallback(callback);

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
