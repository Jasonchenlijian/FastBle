package com.clj.blesample;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

/**
 * Created by 陈利健 on 2016/9/20.
 * 如何按照此框架编写代码
 */
public class DemoActivity extends AppCompatActivity {

    // 下面的所有UUID及指令请根据实际设备替换
    private static final String UUID_SERVICE = "00000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_INDICATE = "0000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_NOTIFY = "00000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";
    private static final String SAMPLE_WRITE_DATA = "000000000000000";                  // 要写入设备某一个character的指令

    private static final long TIME_OUT = 10000;                                         // 扫描超时时间
    private static final String DEVICE_NAME = "这里写你的设备名";                         // 符合连接规则的蓝牙设备名
    private static final String DEVICE_MAC = "这里写你的设备地址";                        // 符合连接规则的蓝牙设备地址
    private static final String TAG = "ble_sample";

    private BleManager bleManager;                                                      // Ble核心管理类

    private BluetoothDevice[] bluetoothDevices;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        bleManager = BleManager.getInstance();
        bleManager.init(this);
    }

    /**************************************user's operate****************************************/

    /**
     * 判断是否支持ble
     */
    private boolean isSupportBle() {
        return bleManager.isSupportBle();
    }

    /**
     * 手动开启蓝牙
     */
    private void enableBlue() {
        bleManager.enableBluetooth();
    }

    /**
     * 手动关闭蓝牙
     */
    private void disableBlue() {
        bleManager.disableBluetooth();
    }

    /**
     * 刷新缓存操作
     */
    private void refersh() {
        bleManager.refreshDeviceCache();
    }

    /**
     * 关闭操作
     */
    private void close() {
        bleManager.closeBluetoothGatt();
    }

    /**
     * 扫描出周围所有设备
     */
    private void scanDevice() {
        bleManager.scanDevice(new ListScanCallback(TIME_OUT) {
            @Override
            public void onDeviceFound(BluetoothDevice[] devices) {
                Log.i(TAG, "共发现" + devices.length + "台设备");
                for (int i = 0; i < devices.length; i++) {
                    Log.i(TAG, "name:" + devices[i].getName() + "------mac:" + devices[i].getAddress());
                }
                bluetoothDevices = devices;
            }

            @Override
            public void onScanTimeout() {
                super.onScanTimeout();
                Log.i(TAG, "Time Out");
            }
        });
    }

    /**
     * 当搜索到周围有设备之后，可以选择直接连某一个设备
     */
    private void connectDevice() {
        if (bluetoothDevices == null || bluetoothDevices.length < 1)
            return;
        BluetoothDevice sampleDevice = bluetoothDevices[0];

        bleManager.connectDevice(sampleDevice, true, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                Log.i(TAG, "连接成功！");
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "服务被发现！");
                bleManager.getBluetoothState();
            }

            @Override
            public void onConnectFailure(BleException exception) {
                Log.i(TAG, "连接失败或连接中断：" + exception.toString());
                bleManager.handleException(exception);
            }
        });
    }

    /**
     * 扫描出周围指定名称设备、并连接
     */
    private void scanAndConnect() {
        bleManager.connectDevice(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        bleManager.getBluetoothState();
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + exception.toString());
                        bleManager.handleException(exception);
                    }

                });
    }

    /**
     * 扫描出周围指定地址的设备、并连接
     */
    private void scanAndConnect2() {
        bleManager.connectMac(
                DEVICE_MAC,
                TIME_OUT,
                false,
                new BleGattCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        bleManager.getBluetoothState();
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + exception.toString());
                        bleManager.handleException(exception);
                    }

                });
    }

    /**
     * listen notify
     */
    private void notify_1() {
        bleManager.notifyDevice(UUID_SERVICE, UUID_NOTIFY, notifyCallback);
    }


    /**
     * stop listen notify
     */
    private void stop_notify_1() {
        bleManager.stopListenCharacterCallback(UUID_NOTIFY);
    }


    /**
     * indicate
     */
    private void indicate() {
        bleManager.indicateDevice(
                UUID_SERVICE,
                UUID_INDICATE,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "indicate： " + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "indicate: " + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
    }

    /**
     * write
     */
    private void write() {
        bleManager.writeDevice(
                UUID_SERVICE,
                UUID_WRITE,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "write: " + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "write: " + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
    }

    /**
     * read
     */
    private void read() {
        bleManager.readDevice(
                UUID_SERVICE,
                UUID_WRITE,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "read: " + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "read: " + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
    }

    /**
     * stop notify
     */
    private boolean stopNotify1() {
        return bleManager.stopNotify(UUID_SERVICE, UUID_NOTIFY);
    }

    /**
     * stop indicate
     */
    private boolean stopIndicate() {
        return bleManager.stopIndicate(UUID_SERVICE, UUID_INDICATE);
    }

    /*****************************************callback********************************************/

    /**
     * 构造某一character的callback
     */
    BleCharacterCallback notifyCallback = new BleCharacterCallback() {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "notifyCallback success： " + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
        }

        @Override
        public void onFailure(BleException exception) {
            bleManager.handleException(exception);
        }
    };


}
