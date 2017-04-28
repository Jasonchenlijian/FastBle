package com.clj.blesample.tool;


import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;

public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";

    public BluetoothBinder mBinder = new BluetoothBinder();
    private BleManager bleManager;
    private Handler threadHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;

    private String name;
    private String mac;
    private BluetoothGatt gatt;
    private BluetoothGattService service;

    @Override
    public void onCreate() {
        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bleManager.closeBluetoothGatt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * 服务的回调接口定义(全部回到UI线程)
     */
    public interface Callback {

        void onStartScan();

        void onLeScan(BluetoothDevice device);

        void onDeviceFound(BluetoothDevice[] devices);

        void onConnecting();

        void onConnectFail();

        void onDisConnected();

        void onServicesDiscovered();

    }


    public void scanDevice() {
        if (bleManager.isInScanning())
            return;

        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        bleManager.scanDevice(new ListScanCallback(5000) {
            @Override
            public void onDeviceScan(final BluetoothDevice device) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onLeScan(device);
                        }
                    }
                });
            }

            @Override
            public void onDeviceFound(final BluetoothDevice[] devices) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDeviceFound(devices);
                        }
                    }
                });
            }
        });
    }


    public void stopScan() {

    }

    public void connectDevice(final BluetoothDevice device) {
        if (mCallback != null) {
            mCallback.onConnecting();
        }

        bleManager.connectDevice(device, true, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
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
            public void onFoundDevice(BluetoothDevice device) {
                name = device.getName();
                mac = device.getAddress();
            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
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
            public void onConnectFailure(BleException exception) {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) {
                            mCallback.onDisConnected();
                        }
                    }
                });
            }
        });
    }


    private void resetInfo() {
        name = null;
        mac = null;
        gatt = null;
        service = null;
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


    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            threadHandler.post(runnable);
        }
    }

    private void runOnMainThreadDelayed(Runnable runnable, long delayTime) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            threadHandler.postDelayed(runnable, delayTime);
        }
    }


}
