package com.clj.fastble.bluetooth;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleConnectStateParameter;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;
import com.clj.fastble.utils.BleLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetooth {

    private BleGattCallback mBleGattCallback;
    private BleRssiCallback mBleRssiCallback;
    private BleMtuChangedCallback bleMtuChangedCallback;

    private final HashMap<String, BleNotifyCallback> bleNotifyCallbackHashMap = new HashMap<>();
    private final HashMap<String, BleIndicateCallback> bleIndicateCallbackHashMap = new HashMap<>();
    private final HashMap<String, BleWriteCallback> bleWriteCallbackHashMap = new HashMap<>();
    private final HashMap<String, BleReadCallback> bleReadCallbackHashMap = new HashMap<>();

    private LastState mLastState;
    private boolean isActiveDisconnect = false;
    private final BleDevice mBleDevice;
    private BluetoothGatt mBluetoothGatt;
    private final MainHandler mMainHandler = new MainHandler(Looper.getMainLooper());
    private final BleManager mBleManager = BleManager.getInstance();
    private BleConnector mBleConnector;

    public BleBluetooth(BleDevice bleDevice) {
        this.mBleDevice = bleDevice;
    }

    public BleConnector newBleConnector() {
        if (mBleConnector != null) {
            return mBleConnector;
        }
        return mBleConnector = new BleConnector(this);
    }

    public void addConnectGattCallback(BleGattCallback callback) {
        mBleGattCallback = callback;
    }

    public void removeConnectGattCallback() {
        mBleGattCallback = null;
    }

    public void removeBleConnector() {
        if (mBleConnector != null) {
            mBleConnector.removeCallbacksAndMessages();
            mBleConnector = null;
        }
    }

    public void addNotifyCallback(String uuid, BleNotifyCallback bleNotifyCallback) {
        bleNotifyCallbackHashMap.put(uuid, bleNotifyCallback);
    }

    public void addIndicateCallback(String uuid, BleIndicateCallback bleIndicateCallback) {
        bleIndicateCallbackHashMap.put(uuid, bleIndicateCallback);
    }

    public void addWriteCallback(String uuid, BleWriteCallback bleWriteCallback) {
        bleWriteCallbackHashMap.put(uuid, bleWriteCallback);
    }

    public void addReadCallback(String uuid, BleReadCallback bleReadCallback) {
        bleReadCallbackHashMap.put(uuid, bleReadCallback);
    }

    public void removeNotifyCallback(String uuid) {
        bleNotifyCallbackHashMap.remove(uuid);
    }

    public boolean isHasNotifyCallback(String uuid) {
        return bleNotifyCallbackHashMap.containsKey(uuid);
    }

    public void removeIndicateCallback(String uuid) {
        bleIndicateCallbackHashMap.remove(uuid);
    }

    public void removeWriteCallback(String uuid) {
        bleWriteCallbackHashMap.remove(uuid);
    }

    public void removeReadCallback(String uuid) {
        bleReadCallbackHashMap.remove(uuid);
    }

    public void clearCharacterCallback() {
        bleNotifyCallbackHashMap.clear();
        bleIndicateCallbackHashMap.clear();
        bleWriteCallbackHashMap.clear();
        bleReadCallbackHashMap.clear();
    }

    public void addRssiCallback(BleRssiCallback callback) {
        mBleRssiCallback = callback;
    }

    public void removeRssiCallback() {
        mBleRssiCallback = null;
    }

    public void addMtuChangedCallback(BleMtuChangedCallback callback) {
        bleMtuChangedCallback = callback;
    }

    public void removeMtuChangedCallback() {
        bleMtuChangedCallback = null;
    }


    public String getDeviceKey() {
        return mBleDevice.getKey();
    }

    public BleDevice getDevice() {
        return mBleDevice;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    @SuppressLint("MissingPermission")
    public BluetoothGatt connect(BleDevice bleDevice,
                                 boolean autoConnect,
                                 BleGattCallback callback) {
        synchronized (this) {
            BleLog.i("connect device: " + bleDevice.getName()
                    + "\nmac: " + bleDevice.getMac()
                    + "\nautoConnect: " + autoConnect
                    + "\ncurrentThread: " + Thread.currentThread().getId());
            addConnectGattCallback(callback);

            mLastState = LastState.CONNECT_CONNECTING;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = bleDevice.getDevice().connectGatt(mBleManager.getContext(),
                        autoConnect, coreGattCallback, TRANSPORT_LE);
            } else {
                mBluetoothGatt = bleDevice.getDevice().connectGatt(mBleManager.getContext(),
                        autoConnect, coreGattCallback);
            }
            if (mBluetoothGatt != null) {
                if (mBleGattCallback != null) {
                    mBleGattCallback.onStartConnect();
                }
                Message message = mMainHandler.obtainMessage();
                message.what = BleMsg.MSG_CONNECT_OVER_TIME;
                mMainHandler.sendMessageDelayed(message, mBleManager.getConnectOverTime());
            } else {
                disconnect();
                mLastState = LastState.CONNECT_FAILURE;
                mBleManager.getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);
                if (mBleGattCallback != null)
                    mBleGattCallback.onConnectFail(bleDevice, new OtherException("GATT connect exception occurred!"));
            }
            return mBluetoothGatt;
        }
    }

    public void destroy() {
        synchronized (this) {
            BleLog.i("--------Ble: destroy()--------");
            clearCharacterCallback();
            removeConnectGattCallback();
            removeRssiCallback();
            removeMtuChangedCallback();
            disconnect();
            mLastState = LastState.CONNECT_IDLE;
            mMainHandler.removeCallbacksAndMessages(null);
        }
    }

    public void disconnect() {
        synchronized (this) {
            BleLog.d("--------disconnect()-----");
            removeBleConnector();
            disconnectGatt();
            refreshDeviceCache();
        }
    }

    @SuppressLint("MissingPermission")
    private void disconnectGatt() {
        isActiveDisconnect = true;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public void refreshDeviceCache() {
        synchronized (this) {
            try {
                final Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null && mBluetoothGatt != null) {
                    boolean success = (Boolean) refresh.invoke(mBluetoothGatt);
                    BleLog.i("refreshDeviceCache, is success:  " + success);
                }
            } catch (Exception e) {
                BleLog.i("exception occur while refreshing device: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private final class MainHandler extends Handler {

        MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleMsg.MSG_CONNECT_FAIL: {
                    disconnect();
                    mLastState = LastState.CONNECT_FAILURE;
                    mBleManager.getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);

                    BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                    int status = para.getStatus();
                    BleGattCallback bleGattCallback = mBleGattCallback;
                    mBleGattCallback = null;
                    if (bleGattCallback != null)
                        bleGattCallback.onConnectFail(mBleDevice, new ConnectException(mBluetoothGatt, status));
                }
                break;

                case BleMsg.MSG_DISCONNECTED: {
                    disconnect();
                    mLastState = LastState.CONNECT_DISCONNECT;
                    mBleManager.getMultipleBluetoothController().removeBleBluetooth(BleBluetooth.this);

                    BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                    boolean isActive = para.isActive();
                    int status = para.getStatus();

                    clearCharacterCallback();
                    removeRssiCallback();
                    removeMtuChangedCallback();
                    BleGattCallback bleGattCallback = mBleGattCallback;
                    mBleGattCallback = null;
                    if (bleGattCallback != null)
                        bleGattCallback.onDisConnected(isActive, mBleDevice, mBluetoothGatt, status);
                }
                break;
                case BleMsg.MSG_CONNECT_OVER_TIME: {
                    disconnect();
                    mLastState = LastState.CONNECT_FAILURE;
                    mBleManager.getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);
                    BleGattCallback bleGattCallback = mBleGattCallback;
                    mBleGattCallback = null;
                    if (bleGattCallback != null)
                        bleGattCallback.onConnectFail(mBleDevice, new TimeoutException());
                }
                break;

                case BleMsg.MSG_DISCOVER_SERVICES: {
                    if (mBluetoothGatt != null) {
                        boolean discoverServiceResult = mBluetoothGatt.discoverServices();
                        if (!discoverServiceResult) {
                            Message message = mMainHandler.obtainMessage();
                            message.what = BleMsg.MSG_DISCOVER_FAIL;
                            mMainHandler.sendMessage(message);
                        }
                    } else {
                        Message message = mMainHandler.obtainMessage();
                        message.what = BleMsg.MSG_DISCOVER_FAIL;
                        mMainHandler.sendMessage(message);
                    }
                }
                break;

                case BleMsg.MSG_DISCOVER_FAIL: {
                    discoverFail();
                }
                break;
                case BleMsg.MSG_DISCOVER_SUCCESS: {
                    discoverSuccess(msg);
                }
                break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private void discoverSuccess(Message msg) {
        mLastState = LastState.CONNECT_CONNECTED;
        isActiveDisconnect = false;
        mBleManager.getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);
        mBleManager.getMultipleBluetoothController().addBleBluetooth(BleBluetooth.this);

        BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
        int status = para.getStatus();
        if (mBleGattCallback != null)
            mBleGattCallback.onConnectSuccess(mBleDevice, mBluetoothGatt, status);
    }

    private void discoverFail() {
        disconnect();
        mLastState = LastState.CONNECT_FAILURE;
        mBleManager.getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);

        if (mBleGattCallback != null)
            mBleGattCallback.onConnectFail(mBleDevice, new OtherException("GATT discover services exception occurred!"));
    }

    private final BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "newState: " + newState
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            mBluetoothGatt = gatt;

            mMainHandler.removeMessages(BleMsg.MSG_CONNECT_OVER_TIME);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Message message = mMainHandler.obtainMessage();
                message.what = BleMsg.MSG_DISCOVER_SERVICES;
                mMainHandler.sendMessageDelayed(message, 0);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mLastState == LastState.CONNECT_CONNECTING) {
                    Message message = mMainHandler.obtainMessage();
                    message.what = BleMsg.MSG_CONNECT_FAIL;
                    message.obj = new BleConnectStateParameter(status);
                    mMainHandler.sendMessage(message);
                } else if (mLastState == LastState.CONNECT_CONNECTED) {
                    Message message = mMainHandler.obtainMessage();
                    message.what = BleMsg.MSG_DISCONNECTED;
                    BleConnectStateParameter para = new BleConnectStateParameter(status);
                    para.setActive(isActiveDisconnect);
                    message.obj = para;
                    mMainHandler.sendMessage(message);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BleLog.i("BluetoothGattCallback：onServicesDiscovered "
                    + '\n' + "status: " + status
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            mBluetoothGatt = gatt;
            mMainHandler.removeMessages(BleMsg.MSG_CONNECT_OVER_TIME);

            Message message = mMainHandler.obtainMessage();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                message.what = BleMsg.MSG_DISCOVER_SUCCESS;
                message.obj = new BleConnectStateParameter(status);
            } else {
                message.what = BleMsg.MSG_DISCOVER_FAIL;
            }
            mMainHandler.sendMessage(message);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            for (Map.Entry<String, BleNotifyCallback> entry : bleNotifyCallbackHashMap.entrySet()) {
                BleNotifyCallback bleNotifyCallback = entry.getValue();
                if (bleNotifyCallback != null) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleNotifyCallback.getKey())) {
                        Handler handler = bleNotifyCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE;
                            message.obj = bleNotifyCallback;
                            Bundle bundle = new Bundle();
                            bundle.putByteArray(BleMsg.KEY_NOTIFY_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }

            for (Map.Entry<String, BleIndicateCallback> entry : bleIndicateCallbackHashMap.entrySet()) {
                BleIndicateCallback bleIndicateCallback = entry.getValue();
                if (bleIndicateCallback != null) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleIndicateCallback.getKey())) {
                        Handler handler = bleIndicateCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_INDICATE_DATA_CHANGE;
                            message.obj = bleIndicateCallback;
                            Bundle bundle = new Bundle();
                            bundle.putByteArray(BleMsg.KEY_INDICATE_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            for (Map.Entry<String, BleNotifyCallback> entry : bleNotifyCallbackHashMap.entrySet()) {
                BleNotifyCallback bleNotifyCallback = entry.getValue();
                if (bleNotifyCallback != null) {
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(bleNotifyCallback.getKey())) {
                        Handler handler = bleNotifyCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_NOTIFY_RESULT;
                            message.obj = bleNotifyCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_NOTIFY_BUNDLE_STATUS, status);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }

            for (Map.Entry<String, BleIndicateCallback> entry : bleIndicateCallbackHashMap.entrySet()) {
                BleIndicateCallback bleIndicateCallback = entry.getValue();
                if (bleIndicateCallback != null) {
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(bleIndicateCallback.getKey())) {
                        Handler handler = bleIndicateCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_INDICATE_RESULT;
                            message.obj = bleIndicateCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_INDICATE_BUNDLE_STATUS, status);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            for (Map.Entry<String, BleWriteCallback> entry : bleWriteCallbackHashMap.entrySet()) {
                BleWriteCallback bleWriteCallback = entry.getValue();
                if (bleWriteCallback != null) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleWriteCallback.getKey())) {
                        Handler handler = bleWriteCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_WRITE_RESULT;
                            message.obj = bleWriteCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_WRITE_BUNDLE_STATUS, status);
                            bundle.putByteArray(BleMsg.KEY_WRITE_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            for (Map.Entry<String, BleReadCallback> entry : bleReadCallbackHashMap.entrySet()) {
                BleReadCallback bleReadCallback = entry.getValue();
                if (bleReadCallback != null) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleReadCallback.getKey())) {
                        Handler handler = bleReadCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_READ_RESULT;
                            message.obj = bleReadCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_READ_BUNDLE_STATUS, status);
                            bundle.putByteArray(BleMsg.KEY_READ_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);

            if (mBleRssiCallback != null) {
                Handler handler = mBleRssiCallback.getHandler();
                if (handler != null) {
                    Message message = handler.obtainMessage();
                    message.what = BleMsg.MSG_READ_RSSI_RESULT;
                    message.obj = mBleRssiCallback;
                    Bundle bundle = new Bundle();
                    bundle.putInt(BleMsg.KEY_READ_RSSI_BUNDLE_STATUS, status);
                    bundle.putInt(BleMsg.KEY_READ_RSSI_BUNDLE_VALUE, rssi);
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if (bleMtuChangedCallback != null) {
                Handler handler = bleMtuChangedCallback.getHandler();
                if (handler != null) {
                    Message message = handler.obtainMessage();
                    message.what = BleMsg.MSG_SET_MTU_RESULT;
                    message.obj = bleMtuChangedCallback;
                    Bundle bundle = new Bundle();
                    bundle.putInt(BleMsg.KEY_SET_MTU_BUNDLE_STATUS, status);
                    bundle.putInt(BleMsg.KEY_SET_MTU_BUNDLE_VALUE, mtu);
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }
        }
    };

    enum LastState {
        CONNECT_IDLE,
        CONNECT_CONNECTING,
        CONNECT_CONNECTED,
        CONNECT_FAILURE,
        CONNECT_DISCONNECT
    }

}
