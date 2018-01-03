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
import com.clj.fastble.callback.BleMtuChangedCallback;
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
    private BleMtuChangedCallback bleMtuChangedCallback;
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

    public synchronized void addMtuChangedCallback(BleMtuChangedCallback callback) {
        bleMtuChangedCallback = callback;
    }

    public synchronized void removeMtuChangedCallback() {
        bleMtuChangedCallback = null;
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
            gatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(),
                    autoConnect, coreGattCallback, TRANSPORT_LE);
        } else {
            gatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(),
                    autoConnect, coreGattCallback);
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
        removeMtuChangedCallback();
        clearCharacterCallback();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "newState: " + newState
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            final BluetoothGatt finalGatt = gatt;
            final int finalStatus = status;
            final int finalState = newState;

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
                                bleGattCallback.onConnectFail(new ConnectException(finalGatt, finalStatus));
                        }
                    });

                } else if (connectState == BleConnectState.CONNECT_CONNECTED) {
                    connectState = BleConnectState.CONNECT_DISCONNECT;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bleGattCallback != null)
                                bleGattCallback.onDisConnected(isActivityDisconnect, bleBluetooth.getDevice(), finalGatt, finalState);
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BleLog.i("BluetoothGattCallback：onServicesDiscovered "
                    + '\n' + "status: " + status
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            final BluetoothGatt finalGatt = gatt;
            final int finalStatus = status;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGatt = finalGatt;
                connectState = BleConnectState.CONNECT_CONNECTED;
                isActivityDisconnect = false;
                BleManager.getInstance().getMultipleBluetoothController().addBleBluetooth(bleBluetooth);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bleGattCallback != null)
                            bleGattCallback.onConnectSuccess(bleDevice, finalGatt, finalStatus);
                    }
                });
            } else {
                closeBluetoothGatt();
                connectState = BleConnectState.CONNECT_FAILURE;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bleGattCallback != null)
                            bleGattCallback.onConnectFail(new ConnectException(finalGatt, finalStatus));
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            BleLog.i("BluetoothGattCallback：onCharacteristicChanged ");

            final byte[] data = characteristic.getValue();

            Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object callback = entry.getValue();
                if (callback instanceof BleNotifyCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((BleNotifyCallback) callback).getKey())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((BleNotifyCallback) callback).onCharacteristicChanged(data);
                            }
                        });
                    }
                }
            }

            iterator = bleIndicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object callback = entry.getValue();
                if (callback instanceof BleIndicateCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((BleIndicateCallback) callback).getKey())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((BleIndicateCallback) callback).onCharacteristicChanged(data);
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            BleLog.i("BleGattCallback：onDescriptorWrite ");

            final int finalStatus = status;

            Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof BleNotifyCallback) {
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(((BleNotifyCallback) call).getKey())) {
                        ((BleNotifyCallback) call).getBleConnector().notifyMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalStatus == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleNotifyCallback) call).onNotifySuccess();
                                } else {
                                    ((BleNotifyCallback) call).onNotifyFailure(new GattException(finalStatus));
                                }
                            }
                        });
                    }
                }
            }

            iterator = bleIndicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object callback = entry.getValue();
                if (callback instanceof BleIndicateCallback) {
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(((BleIndicateCallback) callback).getKey())) {
                        ((BleIndicateCallback) callback).getBleConnector().indicateMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalStatus == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleIndicateCallback) callback).onIndicateSuccess();
                                } else {
                                    ((BleIndicateCallback) callback).onIndicateFailure(new GattException(finalStatus));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            BleLog.i("BluetoothGattCallback：onCharacteristicWrite ");

            final int finalStatus = status;

            Iterator iterator = bleWriteCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object callback = entry.getValue();
                if (callback instanceof BleWriteCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((BleWriteCallback) callback).getKey())) {
                        ((BleWriteCallback) callback).getBleConnector().writeMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalStatus == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleWriteCallback) callback).onWriteSuccess();
                                } else {
                                    ((BleWriteCallback) callback).onWriteFailure(new GattException(finalStatus));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            BleLog.i("BluetoothGattCallback：onCharacteristicRead ");

            final byte[] data = characteristic.getValue();
            final int finalStatus = status;

            Iterator iterator = bleReadCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object callback = entry.getValue();
                if (callback instanceof BleReadCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((BleReadCallback) callback).getKey())) {
                        ((BleReadCallback) callback).getBleConnector().readMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (finalStatus == BluetoothGatt.GATT_SUCCESS) {
                                    ((BleReadCallback) callback).onReadSuccess(data);
                                } else {
                                    ((BleReadCallback) callback).onReadFailure(new GattException(finalStatus));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            BleLog.i("BluetoothGattCallback：onReadRemoteRssi " + status);

            final int finalStatus = status;

            if (bleRssiCallback != null) {
                bleRssiCallback.getBleConnector().rssiMsgInit();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalStatus == BluetoothGatt.GATT_SUCCESS) {
                            bleRssiCallback.onRssiSuccess(rssi);
                        } else {
                            bleRssiCallback.onRssiFailure(new GattException(finalStatus));
                        }
                    }
                });
            }

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            BleLog.i("BluetoothGattCallback：onMtuChanged ");

            final int currentMtu = mtu;
            final int finalStatus = status;

            if (bleMtuChangedCallback != null) {
                bleMtuChangedCallback.getBleConnector().mtuChangedMsgInit();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalStatus == BluetoothGatt.GATT_SUCCESS) {
                            bleMtuChangedCallback.onMtuChanged(currentMtu);
                        } else {
                            bleMtuChangedCallback.onSetMTUFailure(new GattException(finalStatus));
                        }
                    }
                });
            }

        }
    };

}
