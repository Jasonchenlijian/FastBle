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
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.util.List;
import java.util.UUID;

public class BluetoothService extends Service {

    public BluetoothBinder mBinder = new BluetoothBinder();
    private Handler threadHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;
    private Callback2 mCallback2 = null;

    private BleDevice mBleDevice;
    private BluetoothGatt mGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    @Override
    public void onCreate() {
        BleManager.getInstance().init(getApplication());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().clear();
        mCallback = null;
        mCallback2 = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        BleManager.getInstance().disconnect();
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
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    public void scanDevice(boolean isConnect) {
        if (isConnect) {
            scanAndConnect();
        } else {
            scan();
        }
    }

    public void cancelScan() {
        BleManager.getInstance().cancelScan();
    }

    public void scan() {
        resetInfo();

        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                if (mCallback != null) {
                    if (success) {
                        mCallback.onStartScan();
                    } else {
                        mCallback.onScanComplete();
                    }
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
    }

    public void connect(final BleDevice scanResult) {
        BleManager.getInstance().connect(scanResult, new BleGattCallback() {

            @Override
            public void onScanStarted(boolean success) {

            }

            @Override
            public void onFoundDevice(BleDevice bleDevice) {
                if (mCallback != null) {
                    mCallback.onConnecting();
                }
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
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                mBleDevice = bleDevice;
                mGatt = gatt;
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
            public void onDisConnected(boolean isActive, BluetoothGatt gatt, int status) {
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

        BleManager.getInstance().scanAndConnect(new BleGattCallback() {

            @Override
            public void onScanStarted(boolean success) {
                if (mCallback != null) {
                    if (success) {
                        mCallback.onStartScan();
                    } else {
                        mCallback.onScanComplete();
                    }
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
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                mBleDevice = bleDevice;
                mGatt = gatt;
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
            public void onDisConnected(boolean isActive, BluetoothGatt gatt, int status) {
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

    public void read(String uuid_service, String uuid_read, BleReadCallback callback) {
        BleManager.getInstance().read(mBleDevice, uuid_service, uuid_read, callback);
    }

    public void write(String uuid_service, String uuid_write, String hex, BleWriteCallback callback) {
        BleManager.getInstance().write(mBleDevice, uuid_service, uuid_write, HexUtil.hexStringToBytes(hex), callback);
    }

    public void notify(String uuid_service, String uuid_notify, BleNotifyCallback callback) {
        BleManager.getInstance().notify(mBleDevice, uuid_service, uuid_notify, callback);
    }

    public void indicate(String uuid_service, String uuid_indicate, BleIndicateCallback callback) {
        BleManager.getInstance().indicate(mBleDevice, uuid_service, uuid_indicate, callback);
    }

    public void stopNotify(String uuid_service, String uuid_notify) {
        BleManager.getInstance().stopNotify(mBleDevice, uuid_service, uuid_notify);
    }

    public void stopIndicate(String uuid_service, String uuid_indicate) {
        BleManager.getInstance().stopIndicate(mBleDevice, uuid_service, uuid_indicate);
    }

    public void readRssi(BleRssiCallback callback) {
        BleManager.getInstance().readRssi(mBleDevice, callback);
    }

    public void closeConnect() {
        BleManager.getInstance().clear();
    }


    private void resetInfo() {
        mBleDevice = null;
        service = null;
        characteristic = null;
        charaProp = 0;
    }

    public BleDevice getBleDevice() {
        return mBleDevice;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
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
