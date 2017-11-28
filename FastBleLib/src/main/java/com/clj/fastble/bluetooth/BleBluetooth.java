package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleConnectState;
import com.clj.fastble.data.BleDevice;
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

    private BleConnectState connectState = BleConnectState.CONNECT_IDLE;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isActivityDisconnect = false;

    private BleGattCallback bleGattCallback;
    private BleRssiCallback bleRssiCallback;
    private HashMap<String, BleNotifyCallback> bleNotifyCallbackHashMap = new HashMap<>();
    private HashMap<String, BleIndicateCallback> bleIndicateCallbackHashMap = new HashMap<>();
    private HashMap<String, BleWriteCallback> bleWriteCallbackHashMap = new HashMap<>();
    private HashMap<String, BleReadCallback> bleReadCallbackHashMap = new HashMap<>();

    private BleBluetooth bleBluetooth;
    private BleDevice bleDevice;
    private BluetoothGatt bluetoothGatt;


    public BleBluetooth(BleDevice bleDevice) {
        bleBluetooth = this;
        this.bleDevice = bleDevice;
    }


    public BleConnector newBleConnector() {
        return new BleConnector(this);
    }


    public synchronized void addConnectGattCallback(BleGattCallback callback) {
        bleGattCallback = callback;
    }

    public synchronized void removeConnectGattCallback() {
        bleGattCallback = null;
    }

    public synchronized void addNotifyCallback(String uuid, BleNotifyCallback bleNotifyCallback) {
        bleNotifyCallbackHashMap.put(uuid, bleNotifyCallback);
    }

    public synchronized void addIndicateCallback(String uuid, BleIndicateCallback bleIndicateCallback) {
        bleIndicateCallbackHashMap.put(uuid, bleIndicateCallback);
    }

    public synchronized void addWriteCallback(String uuid, BleWriteCallback bleWriteCallback) {
        bleWriteCallbackHashMap.put(uuid, bleWriteCallback);
    }

    public synchronized void addReadCallback(String uuid, BleReadCallback bleReadCallback) {
        bleReadCallbackHashMap.put(uuid, bleReadCallback);
    }

    public synchronized void removeNotifyCallback(String uuid) {
        if (bleNotifyCallbackHashMap.containsKey(uuid))
            bleNotifyCallbackHashMap.remove(uuid);
    }

    public synchronized void removeIndicateCallback(String uuid) {
        if (bleIndicateCallbackHashMap.containsKey(uuid))
            bleIndicateCallbackHashMap.remove(uuid);
    }

    public synchronized void removeWriteCallback(String uuid) {
        if (bleWriteCallbackHashMap.containsKey(uuid))
            bleWriteCallbackHashMap.remove(uuid);
    }

    public synchronized void removeReadCallback(String uuid) {
        if (bleReadCallbackHashMap.containsKey(uuid))
            bleReadCallbackHashMap.remove(uuid);
    }

    public synchronized void clearCharacterCallback() {
        if (bleNotifyCallbackHashMap != null)
            bleNotifyCallbackHashMap.clear();
        if (bleIndicateCallbackHashMap != null)
            bleIndicateCallbackHashMap.clear();
        if (bleWriteCallbackHashMap != null)
            bleWriteCallbackHashMap.clear();
        if (bleReadCallbackHashMap != null)
            bleReadCallbackHashMap.clear();
    }

    public synchronized void addRssiCallback(BleRssiCallback callback) {
        bleRssiCallback = callback;
    }

    public synchronized void removeRssiCallback() {
        bleRssiCallback = null;
    }


    public String getDeviceKey() {
        return bleDevice.getKey();
    }

    public BleConnectState getConnectState() {
        return connectState;
    }

    public BleDevice getDevice() {
        return bleDevice;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }


    public synchronized BluetoothGatt connect(BleDevice bleDevice,
                                              boolean autoConnect,
                                              BleGattCallback callback) {
        BleLog.i("connect device: " + bleDevice.getName()
                + "\nmac: " + bleDevice.getMac()
                + "\nautoConnect: " + autoConnect);
        addConnectGattCallback(callback);

        BluetoothGatt gatt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(), autoConnect, coreGattCallback, TRANSPORT_LE);
        } else {
            gatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(), autoConnect, coreGattCallback);
        }
        if (gatt != null) {
            if (bleGattCallback != null)
                bleGattCallback.onStartConnect();
            connectState = BleConnectState.CONNECT_CONNECTING;
        }
        return gatt;
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
        if (bluetoothGatt != null) {
            isActivityDisconnect = true;
            bluetoothGatt.disconnect();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private synchronized void closeBluetoothGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    public synchronized void destroy() {
        connectState = BleConnectState.CONNECT_IDLE;
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
        if (bluetoothGatt != null) {
            refreshDeviceCache();
        }
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        removeConnectGattCallback();
        removeRssiCallback();
        clearCharacterCallback();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "newState: " + newState
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                closeBluetoothGatt();

                BleManager.getInstance().getMultipleBluetoothController().removeBleBluetooth(bleBluetooth);

                if (connectState == BleConnectState.CONNECT_CONNECTING) {
                    connectState = BleConnectState.CONNECT_FAILURE;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bleGattCallback != null)
                                bleGattCallback.onConnectFail(new ConnectException(gatt, status));
                        }
                    });

                } else if (connectState == BleConnectState.CONNECT_CONNECTED) {
                    connectState = BleConnectState.CONNECT_DISCONNECT;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bleGattCallback != null)
                                bleGattCallback.onDisConnected(isActivityDisconnect, bleBluetooth.getDevice(), gatt, newState);
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            BleLog.i("BluetoothGattCallback：onServicesDiscovered "
                    + '\n' + "status: " + status
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGatt = gatt;
                connectState = BleConnectState.CONNECT_CONNECTED;

                isActivityDisconnect = false;
                BleManager.getInstance().getMultipleBluetoothController().addBleBluetooth(bleBluetooth);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bleGattCallback != null)
                            bleGattCallback.onConnectSuccess(bleDevice, gatt, status);
                    }
                });
            } else {
                closeBluetoothGatt();
                connectState = BleConnectState.CONNECT_FAILURE;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bleGattCallback != null)
                            bleGattCallback.onConnectFail(new ConnectException(gatt, status));
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            BleLog.i("BluetoothGattCallback：onCharacteristicChanged ");

            Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleNotifyCallback) {
                    if (characteristic.getUuid().toString().equals(((BleNotifyCallback) call).getKey())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bleGattCallback != null)
                                    ((BleNotifyCallback) call).onCharacteristicChanged(characteristic.getValue());
                            }
                        });
                    }
                }
            }

            iterator = bleIndicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleIndicateCallback) {
                    if (characteristic.getUuid().toString().equals(((BleIndicateCallback) call).getKey())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (bleGattCallback != null)
                                    ((BleIndicateCallback) call).onCharacteristicChanged(characteristic.getValue());
                            }
                        });
                    }
                }
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            BleLog.i("BleGattCallback：onDescriptorWrite ");

            Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleNotifyCallback) {
                    if (descriptor.getCharacteristic().getUuid().toString().equals(((BleNotifyCallback) call).getKey())) {
                        ((BleNotifyCallback) call).getBleConnector().notifySuccess();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleNotifyCallback) call).onNotifySuccess();
                                } else {
                                    ((BleNotifyCallback) call).onNotifyFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }

            iterator = bleIndicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleIndicateCallback) {
                    if (descriptor.getCharacteristic().getUuid().toString().equals(((BleIndicateCallback) call).getKey())) {
                        ((BleIndicateCallback) call).getBleConnector().indicateSuccess();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleIndicateCallback) call).onIndicateSuccess();
                                } else {
                                    ((BleIndicateCallback) call).onIndicateFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, final int status) {
            BleLog.i("BluetoothGattCallback：onCharacteristicWrite ");

            Iterator iterator = bleWriteCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleWriteCallback) {
                    if (characteristic.getUuid().toString().equals(((BleWriteCallback) call).getKey())) {
                        ((BleWriteCallback) call).getBleConnector().writeSuccess();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleWriteCallback) call).onWriteSuccess();
                                } else {
                                    ((BleWriteCallback) call).onWriteFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            BleLog.i("BluetoothGattCallback：onCharacteristicRead ");

            Iterator iterator = bleReadCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleReadCallback) {
                    if (characteristic.getUuid().toString().equals(((BleReadCallback) call).getKey())) {
                        ((BleReadCallback) call).getBleConnector().readSuccess();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleReadCallback) call).onReadSuccess(characteristic.getValue());
                                } else {
                                    ((BleReadCallback) call).onReadFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            BleLog.i("BluetoothGattCallback：onReadRemoteRssi ");

            bleRssiCallback.getBleConnector().rssiSuccess();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        bleRssiCallback.onRssiSuccess(rssi);
                    } else {
                        bleRssiCallback.onRssiFailure(new GattException(status));
                    }
                }
            });
        }

    };

}
