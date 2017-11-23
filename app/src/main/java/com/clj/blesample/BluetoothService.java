package com.clj.blesample;


import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleRssiCallback;
import com.clj.fastble.conn.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.util.List;
import java.util.UUID;

public class BluetoothService extends Service {

    public BluetoothBinder mBinder = new BluetoothBinder();
    private BleManager bleManager;
    private Handler threadHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;
    private Callback2 mCallback2 = null;

    private String name;
    private String mac;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    @Override
    public void onCreate() {
        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bleManager = null;
        mCallback = null;
        mCallback2 = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        bleManager.closeBluetoothGatt();
        return super.onUnbind(intent);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void setScanCallback(Callback callback) {
        mCallback = callback;
    }

    public void setConnectCallback(Callback2 callback) {
        mCallback2 = callback;
    }

    public interface Callback {

        void onStartScan();

        void onScanning(BleDevice scanResult);

        void onScanComplete();

        void onConnecting();

        void onConnectFail();

        void onDisConnected();

        void onServicesDiscovered();
    }

    public interface Callback2 {

        void onDisConnected();
    }

    public void setting(String[] names, String mac, String[] uuids, boolean isAuto) {
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                serviceUuids[i] = UUID.fromString(uuids[i]);
            }
        }

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)
                .setDeviceName(true, names)
                .setDeviceMac(mac)
                .setAutoConnect(isAuto)
                .setTimeOut(8000)
                .build();
        bleManager.initScanRule(scanRuleConfig);
    }

    public void scanDevice(boolean isConnect) {
        if (isConnect) {
            scanAndConnect();
        } else {
            scan();
        }
    }

    public void cancelScan() {
        bleManager.cancelScan();
    }

    public void scan() {
        resetInfo();

        boolean b = bleManager.scan(new BleScanCallback() {
            @Override
            public void onScanStarted() {
                if (mCallback != null) {
                    mCallback.onStartScan();
                }
            }

            @Override
            public void onScanning(final BleDevice result) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onScanning(result);
                        }
                    }
                });
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onScanComplete();
                        }
                    }
                });
            }
        });
        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete();
            }
        }
    }

    public void connect(final BleDevice scanResult) {
        bleManager.connect(scanResult, new BleGattCallback() {

            @Override
            public void onScanStarted() {

            }

            @Override
            public void onFoundDevice(BleDevice scanResult) {
                if (mCallback != null) {
                    mCallback.onConnecting();
                }
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail();
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered();
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected();
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected();
                        }
                    }
                });
            }
        });
    }

    public void scanAndConnect() {
        resetInfo();

        bleManager.scanAndConnect(new BleGattCallback() {

            @Override
            public void onScanStarted() {
                if (mCallback != null) {
                    mCallback.onStartScan();
                }
            }

            @Override
            public void onFoundDevice(BleDevice scanResult) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onScanComplete();
                        }
                    }
                });
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnecting();
                        }
                    }
                });
            }

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onConnectFail();
                        }
                    }
                });
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothService.this.gatt = gatt;
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onServicesDiscovered();
                        }
                    }
                });
            }

            @Override
            public void onDisConnected(BluetoothGatt gatt, int status, BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected();
                        }
                        if (mCallback2 != null) {
                            mCallback2.onDisConnected();
                        }
                    }
                });
            }
        });
    }

    public boolean read(String uuid_service, String uuid_read, BleCharacterCallback callback) {
        return bleManager.read(uuid_service, uuid_read, callback);
    }

    public boolean write(String uuid_service, String uuid_write, String hex, BleCharacterCallback callback) {
        return bleManager.write(uuid_service, uuid_write, HexUtil.hexStringToBytes(hex), callback);
    }

    public boolean notify(String uuid_service, String uuid_notify, BleCharacterCallback callback) {
        return bleManager.notify(uuid_service, uuid_notify, callback);
    }

    public boolean indicate(String uuid_service, String uuid_indicate, BleCharacterCallback callback) {
        return bleManager.indicate(uuid_service, uuid_indicate, callback);
    }

    public boolean stopNotify(String uuid_service, String uuid_notify) {
        return bleManager.stopNotify(uuid_service, uuid_notify);
    }

    public boolean stopIndicate(String uuid_service, String uuid_indicate) {
        return bleManager.stopIndicate(uuid_service, uuid_indicate);
    }

    public boolean readRssi(BleRssiCallback callback) {
        return bleManager.readRssi(callback);
    }

    public void closeConnect() {
        bleManager.closeBluetoothGatt();
    }


    private void resetInfo() {
        name = null;
        mac = null;
        gatt = null;
        service = null;
        characteristic = null;
        charaProp = 0;
    }

    public String getName() {
        return name;
    }

    public String getMac() {
        return mac;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void setService(BluetoothGattService service) {
        this.service = service;
    }

    public BluetoothGattService getService() {
        return service;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharaProp(int charaProp) {
        this.charaProp = charaProp;
    }

    public int getCharaProp() {
        return charaProp;
    }


    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            threadHandler.post(runnable);
        }
    }


}
