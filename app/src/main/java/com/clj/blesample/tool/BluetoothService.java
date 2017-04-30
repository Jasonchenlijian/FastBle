package com.clj.blesample.tool;


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
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

public class BluetoothService extends Service {

    public BluetoothBinder mBinder = new BluetoothBinder();
    private BleManager bleManager;
    private Handler threadHandler = new Handler(Looper.getMainLooper());
    private Callback mCallback = null;

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

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {

        void onStartScan();

        void onScanning(ScanResult scanResult);

        void onScanComplete();

        void onConnecting();

        void onConnectFail();

        void onDisConnected();

        void onServicesDiscovered();
    }

    public void scanDevice() {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanDevice(new ListScanCallback(5000) {

            @Override
            public void onScanning(final ScanResult result) {
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
            public void onScanComplete(final ScanResult[] results) {
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

    public void cancelScan() {
        bleManager.cancelScan();
    }

    public void connectDevice(final ScanResult scanResult) {
        if (mCallback != null) {
            mCallback.onConnecting();
        }

        bleManager.connectDevice(scanResult, true, new BleGattCallback() {
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
            public void onFoundDevice(ScanResult scanResult) {
                BluetoothService.this.name = scanResult.getDevice().getName();
                BluetoothService.this.mac = scanResult.getDevice().getAddress();
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

    public void scanAndConnect1(String name) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanNameAndConnect(
                name,
                5000,
                false,
                new BleGattCallback() {
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
                    public void onFoundDevice(ScanResult scanResult) {
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
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
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

        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete();
            }
        }
    }

    public void scanAndConnect2(String name) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanfuzzyNameAndConnect(
                name,
                5000,
                false,
                new BleGattCallback() {
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
                    public void onFoundDevice(ScanResult scanResult) {
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
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
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

        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete();
            }
        }
    }

    public void scanAndConnect3(String[] names) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanNamesAndConnect(
                names,
                5000,
                false,
                new BleGattCallback() {
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
                    public void onFoundDevice(ScanResult scanResult) {
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
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
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

        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete();
            }
        }
    }

    public void scanAndConnect4(String[] names) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanfuzzyNamesAndConnect(
                names,
                5000,
                false,
                new BleGattCallback() {
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
                    public void onFoundDevice(ScanResult scanResult) {
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
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
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

        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete();
            }
        }
    }

    public void scanAndConnect5(String mac) {
        resetInfo();

        if (mCallback != null) {
            mCallback.onStartScan();
        }

        boolean b = bleManager.scanMacAndConnect(
                mac,
                5000,
                false,
                new BleGattCallback() {
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
                    public void onFoundDevice(ScanResult scanResult) {
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
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        gatt.discoverServices();
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

        if (!b) {
            if (mCallback != null) {
                mCallback.onScanComplete();
            }
        }
    }

    public void read(String uuid_service, String uuid_read, BleCharacterCallback callback) {
        bleManager.readDevice(uuid_service, uuid_read, callback);
    }

    public void write(String uuid_service, String uuid_write, String hex, BleCharacterCallback callback) {
        bleManager.writeDevice(uuid_service, uuid_write, HexUtil.hexStringToBytes(hex), callback);
    }

    public void notify(String uuid_service, String uuid_notify, BleCharacterCallback callback) {
        bleManager.notify(uuid_service, uuid_notify, callback);
    }

    public void indicate(String uuid_service, String uuid_indicate, BleCharacterCallback callback) {
        bleManager.indicate(uuid_service, uuid_indicate, callback);
    }

    public void stopNotify(String uuid_service, String uuid_notify) {
        bleManager.stopNotify(uuid_service, uuid_notify);
    }

    public void stopIndicate(String uuid_service, String uuid_indicate) {
        bleManager.stopIndicate(uuid_service, uuid_indicate);
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
