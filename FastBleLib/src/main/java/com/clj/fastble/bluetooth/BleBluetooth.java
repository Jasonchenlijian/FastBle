package com.clj.fastble.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.clj.fastble.conn.BleConnector;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.utils.BleLog;
import com.clj.fastble.scan.MacScanCallback;
import com.clj.fastble.scan.NameScanCallback;
import com.clj.fastble.scan.PeriodScanCallback;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class BleBluetooth {

    private static final String CONNECT_CALLBACK_KEY = "connect_key";
    public static final String READ_RSSI_KEY = "rssi_key";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_SCANNING = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_SERVICES_DISCOVERED = 4;

    private int connectionState = STATE_DISCONNECTED;
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler(Looper.getMainLooper());
    private HashMap<String, BluetoothGattCallback> callbackHashMap = new HashMap<>();


    public BleBluetooth(Context context) {
        this.context = context = context.getApplicationContext();
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }



    public BleConnector newBleConnector() {
        return new BleConnector(this);
    }




    public boolean isInScanning() {
        return connectionState == STATE_SCANNING;
    }

    public boolean isConnectingOrConnected() {
        return connectionState >= STATE_CONNECTING;
    }

    public boolean isConnected() {
        return connectionState >= STATE_CONNECTED;
    }

    public boolean isServiceDiscovered() {
        return connectionState == STATE_SERVICES_DISCOVERED;
    }




    private void addConnectGattCallback(BleGattCallback callback) {
        callbackHashMap.put(CONNECT_CALLBACK_KEY, callback);
    }

    public void addGattCallback(String uuid, BluetoothGattCallback callback) {
        callbackHashMap.put(uuid, callback);
    }

    public void removeConnectGattCallback() {
        callbackHashMap.remove(CONNECT_CALLBACK_KEY);
    }

    public void removeGattCallback(String key) {
        callbackHashMap.remove(key);
    }

    public void clearCallback() {
        callbackHashMap.clear();
    }

    public BluetoothGattCallback getGattCallback(String uuid) {
        if (TextUtils.isEmpty(uuid))
            return null;
        return callbackHashMap.get(uuid);
    }




    public boolean startLeScan(BluetoothAdapter.LeScanCallback callback) {
        boolean success = bluetoothAdapter.startLeScan(callback);
        if (success) {
            connectionState = STATE_SCANNING;
        }
        return success;
    }

    public boolean startLeScan(PeriodScanCallback callback) {
        callback.setBleBluetooth(this).notifyScanStarted();
        boolean success = bluetoothAdapter.startLeScan(callback);
        if (success) {
            connectionState = STATE_SCANNING;
        } else {
            callback.removeHandlerMsg();
        }
        return success;
    }

    public void stopScan(BluetoothAdapter.LeScanCallback callback) {
        if (callback instanceof PeriodScanCallback) {
            ((PeriodScanCallback) callback).removeHandlerMsg();
        }
        bluetoothAdapter.stopLeScan(callback);
        if (connectionState == STATE_SCANNING) {
            connectionState = STATE_DISCONNECTED;
        }
    }



    public synchronized BluetoothGatt connect(final BluetoothDevice device,
                                              final boolean autoConnect,
                                              final BleGattCallback callback) {
        BleLog.i("connect name: " + device.getName()
                + "\nmac: " + device.getAddress()
                + "\nautoConnect: " + autoConnect);
        addConnectGattCallback(callback);
        return device.connectGatt(context, autoConnect, coreGattCallback);
    }



    /**
     * 搜索指定设备名
     *
     * @param name        设备名
     * @param time_out    超时时间
     * @param autoConnect
     * @param callback
     * @return
     */
    public boolean scanNameAndConnect(String name, long time_out, final boolean autoConnect, final BleGattCallback callback) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("illegal name ! ");
        }
        startLeScan(new NameScanCallback(name, time_out) {

            @Override
            public void onScanTimeout() {
                if (callback != null) {
                    callback.onNotFoundDevice();
                }
            }

            @Override
            public void onDeviceFound(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        connect(device, autoConnect, callback);
                    }
                });
            }
        });
        return true;
    }

    /**
     * 搜索指定设备地址
     *
     * @param mac         设备地址
     * @param time_out    超时时间
     * @param autoConnect
     * @param callback
     * @return
     */
    public boolean scanMacAndConnect(String mac, long time_out, final boolean autoConnect, final BleGattCallback callback) {
        if (TextUtils.isEmpty(mac)) {
            throw new IllegalArgumentException("illegal mac ! ");
        }
        startLeScan(new MacScanCallback(mac, time_out) {

            @Override
            public void onScanTimeout() {
                if (callback != null) {
                    callback.onConnectFailure(BleException.TIMEOUT_EXCEPTION);
                }
            }

            @Override
            public void onDeviceFound(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        connect(device, autoConnect, callback);
                    }
                });
            }
        });
        return true;
    }

    public boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                final boolean success = (Boolean) refresh.invoke(getBluetoothGatt());
                BleLog.i("Refreshing result: " + success);
                return success;
            }
        } catch (Exception e) {
            BleLog.i("An exception occured while refreshing device", e);
        }
        return false;
    }

    /**
     * 断开、刷新、关闭 bluetooth gatt.
     */
    public void closeBluetoothGatt() {
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

    /**
     * 检查蓝牙是否关闭，如果关闭则开启
     */
    public void enableBluetoothIfDisabled() {
        if (!isBlueEnable()) {
            enableBluetooth();
        }
    }

    /**
     * 蓝牙是否打开
     */
    public boolean isBlueEnable() {
        return bluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public void enableBluetooth() {
        bluetoothAdapter.enable();
    }

    /**
     * 关闭蓝牙
     */
    public void disableBluetooth() {
        bluetoothAdapter.disable();
    }

    /**
     * 切换到主线程
     *
     * @param runnable
     */
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

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }


    /**
     * return
     * {@link #STATE_DISCONNECTED}
     * {@link #STATE_SCANNING}
     * {@link #STATE_CONNECTING}
     * {@link #STATE_CONNECTED}
     * {@link #STATE_SERVICES_DISCOVERED}
     */
    public int getConnectionState() {
        return connectionState;
    }

    private BleGattCallback coreGattCallback = new BleGattCallback() {

        @Override
        public void onNotFoundDevice() {
            BleLog.i("coreGattCallback：onNotFoundDevice ");

            bluetoothGatt = null;
            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BleGattCallback) {
                    ((BleGattCallback) call).onNotFoundDevice();
                }
            }
        }

        @Override
        public void onConnectSuccess(BluetoothGatt gatt, int status) {
            BleLog.i("coreGattCallback：onConnectSuccess ");

            bluetoothGatt = gatt;
            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BleGattCallback) {
                    ((BleGattCallback) call).onConnectSuccess(gatt, status);
                }
            }
        }

        @Override
        public void onConnectFailure(BleException exception) {
            BleLog.i("coreGattCallback：onConnectFailure ");

            bluetoothGatt = null;
            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BleGattCallback) {
                    ((BleGattCallback) call).onConnectFailure(exception);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BleLog.i("coreGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "newState: " + newState
                    + '\n' + "thread: " + Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                onConnectSuccess(gatt, status);

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                onConnectFailure(new ConnectException(gatt, status));

            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                connectionState = STATE_CONNECTING;
            }

            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onConnectionStateChange(gatt, status, newState);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BleLog.i("coreGattCallback：onServicesDiscovered ");

            connectionState = STATE_SERVICES_DISCOVERED;
            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onServicesDiscovered(gatt, status);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BleLog.i("coreGattCallback：onCharacteristicRead ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
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
            BleLog.i("coreGattCallback：onCharacteristicWrite ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
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
            BleLog.i("coreGattCallback：onCharacteristicChanged ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onCharacteristicChanged(gatt, characteristic);
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BleLog.i("coreGattCallback：onDescriptorRead ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onDescriptorRead(gatt, descriptor, status);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BleLog.i("coreGattCallback：onDescriptorWrite ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onDescriptorWrite(gatt, descriptor, status);
                }
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            BleLog.i("coreGattCallback：onReliableWriteCompleted ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object call = entry.getValue();
                if (call instanceof BluetoothGattCallback) {
                    ((BluetoothGattCallback) call).onReliableWriteCompleted(gatt, status);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            BleLog.i("coreGattCallback：onReadRemoteRssi ");

            Iterator iterator = callbackHashMap.entrySet().iterator();
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
