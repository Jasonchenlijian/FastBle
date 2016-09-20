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
import com.clj.fastble.utils.BluetoothUtil;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;

/**
 * Created by 陈利健 on 2016/9/20.
 * 如何按照此框架编写代码
 */
public class DemoActivity extends AppCompatActivity {

    // 下面的所有UUID及指令请根据实际设备替换
    private static final String UUID_SERVICE = "00000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_INDICATE = "0000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_NOTIFY_1 = "00000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_NOTIFY_2 = "00000000-0000-1000-8000-00805f9b34fb";
    private static final String UUID_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";
    private static final String SAMPLE_WRITE_DATA = "55aa0bb2100705100600ee";     // 要写入设备某一个特征值的指令

    private static final long TIME_OUT = 10000;                                   // 扫描超时时间
    private static final String DEVICE_NAME = "这里写你的设备名";                   // 符合连接规则的蓝牙设备名，即：device.getName
    private static final String TAG = "ble_sample";

    private BleManager bleManager;                                                // Ble核心管理类

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
                Log.i(TAG, "搜索时间结束");
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

        bleManager.connectDevice(sampleDevice, new BleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                Log.i(TAG, "连接成功！");
                gatt.discoverServices();                // 连接上设备后搜索服务
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.i(TAG, "服务被发现！");
                BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                bleManager.getBluetoothState();               // 打印与该设备的当前状态
            }

            @Override
            public void onConnectFailure(BleException exception) {
                Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
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
                new BleGattCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();                // 连接上设备后搜索服务
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                        bleManager.getBluetoothState();               // 打印与该设备的当前状态
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }

                });
    }

    /**
     * listen notify1
     */
    private void notify_1() {
        bleManager.notifyDevice(UUID_SERVICE, UUID_NOTIFY_1, notifyCallback_1);
    }

    /**
     * listen notify2
     */
    private void notify_2() {
        bleManager.notifyDevice(UUID_SERVICE, UUID_NOTIFY_2, notifyCallback_2);
    }

    /**
     * stop listen notify1
     */
    private void stop_notify_1() {
        bleManager.stopListenCharacterCallback(UUID_NOTIFY_1);
    }

    /**
     * stop listen notify2
     */
    private void stop_notify_2() {
        bleManager.stopListenCharacterCallback(UUID_NOTIFY_2);
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
                        Log.d(TAG, "特征值Indicate通知数据回调： " + '\n' + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "特征值Indicate通知回调失败: " + '\n' + exception.toString());
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
                        Log.d(TAG, "写特征值成功: " + '\n' + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        Log.e(TAG, "写读特征值失败: " + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
    }

    /*****************************************callback********************************************/

    /**
     * 特征值1的回调函数
     */
    BleCharacterCallback notifyCallback_1 = new BleCharacterCallback() {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "notifyCallback_1 success： " + '\n' + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onFailure(BleException exception) {
            bleManager.handleException(exception);
        }
    };

    /**
     * 特征值2的回调函数
     */
    BleCharacterCallback notifyCallback_2 = new BleCharacterCallback() {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "notifyCallback_2 success： " + '\n' + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onFailure(BleException exception) {
            bleManager.handleException(exception);
        }
    };


}
