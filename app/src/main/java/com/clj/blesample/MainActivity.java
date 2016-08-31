package com.clj.blesample;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleBleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.BluetoothUtil;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    // 下面的所有UUID及指令请根据实际设备替换
    private static final String UUID_SERVICE_LISTEN = "00001810-0000-1000-8000-00805f9b34fb";       // 下面两个特征值所对应的service的UUID
    private static final String UUID_LISTEN_INDICATE = "00002A35-0000-1000-8000-00805f9b34fb";      // indicate特征值的UUID
    private static final String UUID_LISTEN_NOTIFY = "00002A36-0000-1000-8000-00805f9b34fb";        // notify特征值的UUID

    private static final String UUID_SERVICE_OPERATE = "0000fff0-0000-1000-8000-00805f9b34fb";      // 下面两个特征值所对应的service的UUID
    private static final String UUID_OPERATE_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";        // 设备写特征值的UUID
    private static final String UUID_OPERATE_NOTIFY = "0000fff2-0000-1000-8000-00805f9b34fb";       // 设备监听写完之后特征值数据改变的UUID

    private static final String SAMPLE_WRITE_DATA = "55aa0bb2100705100600ee";     // 要写入设备某一个特征值的指令

    private static final long TIME_OUT = 10000;                                   // 扫描超时时间
    private static final String DEVICE_NAME = "这里写你的设备名";                   // 符合连接规则的蓝牙设备名，即：device.getName
    private static final String TAG = "ble_sample";

    private BleManager bleManager;                                                // Ble核心管理类


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        bleManager = BleManager.getInstance();
        bleManager.init(this);
    }

    private void initView() {

        /**扫描指定名称设备、并连接*/
        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.connectDevice(
                        DEVICE_NAME,
                        TIME_OUT,
                        new BleBleGattCallback() {
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
        });

        /**notify*/
        findViewById(R.id.btn_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.notifyDevice(
                        UUID_SERVICE_LISTEN,
                        UUID_LISTEN_NOTIFY,
                        UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "特征值Notification通知数据回调： "
                                        + '\n' + Arrays.toString(characteristic.getValue())
                                        + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        /**indicate*/
        findViewById(R.id.btn_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.indicateDevice(
                        UUID_SERVICE_LISTEN,
                        UUID_LISTEN_INDICATE,
                        UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "特征值Indication通知数据回调： "
                                        + '\n' + Arrays.toString(characteristic.getValue())
                                        + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "特征值Indication通知回调失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        /**写指令*/
        findViewById(R.id.btn_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.writeDevice(
                        UUID_SERVICE_OPERATE,
                        UUID_OPERATE_WRITE,
                        UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "写特征值成功: "
                                        + '\n' + Arrays.toString(characteristic.getValue())
                                        + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "写读特征值失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });


        /**刷新缓存操作*/
        findViewById(R.id.btn_6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.refreshDeviceCache();
            }
        });

        /**关闭操作*/
        findViewById(R.id.btn_7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.closeBluetoothGatt();
            }
        });
    }


    /*******************************移除某一个回调示例**********************************/

    /**
     * 将回调实例化，而不是以匿名对象的形式
     */
    BleCharacterCallback bleCharacterCallback = new BleCharacterCallback() {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "特征值Notification通知数据回调： "
                    + '\n' + Arrays.toString(characteristic.getValue())
                    + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        @Override
        public void onFailure(BleException exception) {
            Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
            bleManager.handleException(exception);
        }
    };

    private void addAndRemove() {

        /**需要使用的时候，作为参数传入*/
        bleManager.notifyDevice(
                UUID_SERVICE_OPERATE,
                UUID_OPERATE_NOTIFY,
                UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                bleCharacterCallback);

        /**不需要再监听特征值变化的时候，将该回调接口对象移除*/
        bleManager.removeBleCharacterCallback(bleCharacterCallback);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.closeBluetoothGatt();
        bleManager.disableBluetooth();
    }

}
