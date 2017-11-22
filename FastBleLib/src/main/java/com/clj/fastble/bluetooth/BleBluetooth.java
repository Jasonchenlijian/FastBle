package com.clj.fastble.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
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
import com.clj.fastble.utils.BleLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetooth {

    public static final String READ_RSSI_KEY = "rssi_key";

//    private static final int STATE_DISCONNECTED = 0;
//    private static final int STATE_SCANNING = 1;
//    private static final int STATE_CONNECTING = 2;
//    private static final int STATE_CONNECTED = 3;
//    private static final int STATE_SERVICES_DISCOVERED = 4;

    private ConnectState connectState = ConnectState.CONNECT_INIT;//设备状态描述
    //    private int connectionState = STATE_DISCONNECTED;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler(Looper.getMainLooper());


//    private HashMap<String, BluetoothGattCallback> callbackHashMap = new HashMap<>();
//    private HashMap<String, BleScanCallback> bleScanCallbackHashMap = new HashMap<>();
//    private HashMap<String, BleGattCallback> bleGattCallbackHashMap = new HashMap<>();

    private BleGattCallback bleGattCallback;

    private HashMap<String, BleCharacterCallback> bleCharacterCallbackHashMap = new HashMap<>();
    private HashMap<String, BleRssiCallback> bleRssiCallbackHashMap = new HashMap<>();


    private boolean isActiveDisconnect = false;             // 是否主动断开连接


    public BleBluetooth(Context context) {
        this.context = context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            bluetoothAdapter = bluetoothManager.getAdapter();
    }


    public BleConnector newBleConnector() {
        return new BleConnector(this);
    }


    public boolean isInScanning() {
        return connectState == ConnectState.STATE_SCANNING;
    }

    public boolean isConnected() {
        return connectState == ConnectState.CONNECT_SUCCESS;
    }


    private void addConnectGattCallback(BleGattCallback callback) {
        bleGattCallback = callback;
    }

    public synchronized void removeConnectGattCallback() {
        bleGattCallback = null;
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
            return scanResult.getDevice().connectGatt(context, autoConnect, coreGattCallback, TRANSPORT_LE);
        } else {
            return scanResult.getDevice().connectGatt(context, autoConnect, coreGattCallback);
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

    public void enableBluetoothIfDisabled() {
        if (!isBlueEnable()) {
            enableBluetooth();
        }
    }

    public boolean isBlueEnable() {
        return bluetoothAdapter.isEnabled();
    }

    public void enableBluetooth() {
        bluetoothAdapter.enable();
    }

    public void disableBluetooth() {
        bluetoothAdapter.disable();
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    public Context getContext() {
        return context;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
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
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BleLog.i("BluetoothGattCallback：onCharacteristicRead ");

            Iterator iterator = bleCharacterCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onCharacteristicRead(gatt, characteristic, status);
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
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onCharacteristicWrite(gatt, characteristic, status);
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
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onCharacteristicChanged(gatt, characteristic);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            BleLog.i("BluetoothGattCallback：onReadRemoteRssi ");

            Iterator iterator = bleRssiCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onReadRemoteRssi(gatt, rssi, status);
                }
            }
        }
    };
}
