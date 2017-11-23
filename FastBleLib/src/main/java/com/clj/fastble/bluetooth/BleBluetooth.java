package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleRssiCallback;
import com.clj.fastble.data.ConnectState;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.utils.BleLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetooth {

    private ConnectState connectState = ConnectState.CONNECT_INIT;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler(Looper.getMainLooper());

    private BleGattCallback bleGattCallback;
    private BleRssiCallback bleRssiCallback;
    private HashMap<String, BleCharacterCallback> bleCharacterCallbackHashMap = new HashMap<>();

    private boolean isActiveDisconnect = false;             // 是否主动断开连接


    public BleConnector newBleConnector() {
        return new BleConnector(this);
    }


    public void addConnectGattCallback(BleGattCallback callback) {
        bleGattCallback = callback;
    }

    public synchronized void removeConnectGattCallback() {
        bleGattCallback = null;
    }

    public void addCharacterCallback(String uuid, BleCharacterCallback bleCallback) {
        bleCharacterCallbackHashMap.put(uuid, bleCallback);
    }

    public void removeCharacterCallback(String uuid) {
        if (bleCharacterCallbackHashMap.containsKey(uuid))
            bleCharacterCallbackHashMap.remove(uuid);
    }

    public void addRssiCallback(BleRssiCallback callback) {
        bleRssiCallback = callback;
    }

    public synchronized void removeRssiCallback() {
        bleRssiCallback = null;
    }


//    public void clearCallback() {
//        callbackHashMap.clear();
//    }

//    public BluetoothGattCallback getGattCallback(String uuid) {
//        if (TextUtils.isEmpty(uuid))
//            return null;
//        return callbackHashMap.get(uuid);
//    }


    private BleBluetooth bleBluetooth;
    private ScanResult scanResult;          // 设备基础信息
    private String uniqueSymbol;            // 唯一符号

    public BleBluetooth(ScanResult scanResult) {
        bleBluetooth = this;
        this.scanResult = scanResult;
        this.uniqueSymbol = scanResult.getDevice().getAddress() + scanResult.getDevice().getName();
    }

    /**
     * 获取设备唯一标识
     *
     * @return
     */
    public String getUniqueSymbol() {
        return uniqueSymbol;
    }

    /**
     * 获取设备连接状态
     *
     * @return 返回设备连接状态
     */
    public ConnectState getConnectState() {
        return connectState;
    }

    /**
     * 获取设备详细信息
     *
     * @return
     */
    public ScanResult getBluetoothLeDevice() {
        return scanResult;
    }

    public synchronized BluetoothGatt connect(ScanResult scanResult,
                                              boolean autoConnect,
                                              BleGattCallback callback) {
        BleLog.i("connect device: " + scanResult.getDevice().getName()
                + "\nmac: " + scanResult.getDevice().getAddress()
                + "\nautoConnect: " + autoConnect);
        addConnectGattCallback(callback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return scanResult.getDevice().connectGatt(BleManager.getInstance().getContext(), autoConnect, coreGattCallback, TRANSPORT_LE);
        } else {
            return scanResult.getDevice().connectGatt(BleManager.getInstance().getContext(), autoConnect, coreGattCallback);
        }
    }

    public synchronized boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                boolean success = (Boolean) refresh.invoke(getBluetoothGatt());
                BleLog.i("refreshDeviceCache, is success:  " + success);
                return success;
            }
        } catch (Exception e) {
            BleLog.i("exception occur while refreshing device: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized void disconnect() {
        connectState = ConnectState.CONNECT_DISCONNECT;
        if (bluetoothGatt != null) {
            isActiveDisconnect = true;
            bluetoothGatt.disconnect();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 关闭GATT
     */
    public synchronized void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    public synchronized void closeBluetoothGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }

        if (bluetoothGatt != null) {
            refreshDeviceCache();
        }

        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public ConnectState getConnectionState() {
        return connectState;
    }


    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "newState: " + newState
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                close();
                if (bleGattCallback != null) {
                    BleManager.getInstance().getDeviceMirrorPool().removeBleBluetooth(bleBluetooth);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        connectState = ConnectState.CONNECT_DISCONNECT;
                        bleGattCallback.onDisConnected(gatt, newState, isActiveDisconnect);
                    } else {
                        connectState = ConnectState.CONNECT_FAILURE;
                        bleGattCallback.onConnectError(new ConnectException(gatt, status));
                    }
                }

            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                connectState = ConnectState.CONNECT_PROCESS;
                bleGattCallback.onConnecting(gatt, newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            if (status == 0) {
                bluetoothGatt = gatt;
                connectState = ConnectState.CONNECT_SUCCESS;
                if (bleGattCallback != null) {
                    isActiveDisconnect = false;
                    BleManager.getInstance().getDeviceMirrorPool().addBleBluetooth(bleBluetooth);
                    bleGattCallback.onConnectSuccess();
                }
            } else {
                close();
                if (bleGattCallback != null) {
                    bleGattCallback.onConnectError(new ConnectException(gatt, status));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BleLog.i("BluetoothGattCallback：onCharacteristicChanged ");

            Iterator iterator = bleCharacterCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BleCharacterCallback) {
                    if (characteristic.getUuid().toString().equals(((BleCharacterCallback) call).getKey())) {
                        ((BleCharacterCallback) call).getBleConnector().notifySuccess();
                        ((BleCharacterCallback) call).onSuccess(characteristic);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BleLog.i("BluetoothGattCallback：onCharacteristicWrite ");

            Iterator iterator = bleCharacterCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BleCharacterCallback) {
                    if (characteristic.getUuid().toString().equals(((BleCharacterCallback) call).getKey())) {
                        ((BleCharacterCallback) call).getBleConnector().writeSuccess();
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            ((BleCharacterCallback) call).onSuccess(characteristic);
                        } else {
                            ((BleCharacterCallback) call).onFailure(new GattException(status));
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BleLog.i("BluetoothGattCallback：onCharacteristicRead ");

            Iterator iterator = bleCharacterCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BleCharacterCallback) {
                    if (characteristic.getUuid().toString().equals(((BleCharacterCallback) call).getKey())) {
                        ((BleCharacterCallback) call).getBleConnector().readSuccess();
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            ((BleCharacterCallback) call).onSuccess(characteristic);
                        } else {
                            ((BleCharacterCallback) call).onFailure(new GattException(status));
                        }
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            BleLog.i("BluetoothGattCallback：onReadRemoteRssi ");

            bleRssiCallback.getBleConnector().rssiSuccess();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bleRssiCallback.onSuccess(rssi);
            } else {
                bleRssiCallback.onFailure(new GattException(status));
            }
        }
    };
}
