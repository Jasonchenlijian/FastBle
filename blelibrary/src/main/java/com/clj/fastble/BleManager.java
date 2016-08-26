package com.clj.fastble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.clj.fastble.bluetooth.BleBleGattCallback;
import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.hanlder.DefaultBleExceptionHandler;
import com.clj.fastble.scan.ListNameScanCallback;
import com.clj.fastble.utils.BluetoothUtil;

/**
 * Created by 陈利健 on 2016/8/17.
 * 蓝牙管理类
 */
public class BleManager {

    private static final String TAG = "BleManager";

    /**
     * 单例
     */
    private static BleManager manager;

    /**
     * 蓝牙主要操作对象
     */
    private static BleBluetooth bleBluetooth;
    /**
     * 默认异常处理器
     */
    private DefaultBleExceptionHandler bleExceptionHandler;

    /**
     * 单例对象
     */
    public static BleManager getInstance() {
        if (manager == null) {
            manager = new BleManager();
        }
        return manager;
    }

    /**
     * 初始化
     *
     * @param context Activity
     */
    public void init(Context context) {

        if (bleBluetooth == null) {
            bleBluetooth = new BleBluetooth(context);
        }
        bleBluetooth.enableBluetoothIfDisabled((Activity) context, 1);
        bleExceptionHandler = new DefaultBleExceptionHandler(context);
    }

    /**
     * 扫描连接符合名称的设备，并监听数据变化
     */
    public boolean connectDevice(String deviceName,
                              long time_out,
                              BleManagerConnectCallback callback) {
        return scanAndConnect(deviceName, time_out, callback);
    }

    /**
     * notify
     */
    public boolean notifyDevice(String uuid_service,
                             String uuid_notification,
                             String uuid_client,
                             BleManagerNotifyCallback callback) {
        return enableNotificationOfCharacteristic(uuid_service, uuid_notification, uuid_client, callback);

    }

    /**
     * indicate
     */
    public boolean indicateDevice(String uuid_service,
                               String uuid_indication,
                               String uuid_client,
                               BleManagerIndicateCallback callback) {
        return enableIndicationOfCharacteristic(uuid_service, uuid_indication, uuid_client, callback);

    }

    /**
     * 向设备写特征值
     */
    public boolean writeDevice(String uuid_service,
                            String uuid_write,
                            String uuid_client,
                            byte[] data,
                            BleManagerWriteCallback callback) {
        return writeDataToCharacteristic(uuid_service, uuid_write, uuid_client, data, callback);
    }

    /**
     * 向设备读特征值
     */
    public boolean readDevice(String uuid_service,
                           String uuid_read,
                           String uuid_client,
                           BleManagerReadCallback callback) {
        return readDataFromCharacteristic(uuid_service, uuid_read, uuid_client, callback);
    }

    /**
     * 获取当前状态
     */
    public void getBluetoothState() {
        Log.d(TAG, "连接状态:  " + bleBluetooth.getConnectionState()
                + '\n' + "是否扫描中: " + bleBluetooth.isInScanning()
                + '\n' + "是否连接: " + bleBluetooth.isConnected()
                + '\n' + "服务是否被发现: " + bleBluetooth.isServiceDiscovered());
    }

    /**
     * 刷新蓝牙设备缓存
     */
    public void refreshDeviceCache() {
        bleBluetooth.refreshDeviceCache();
    }

    /**
     * 关闭连接
     */
    public void closeBluetoothGatt() {
        if (bleBluetooth != null && bleBluetooth.isConnectingOrConnected()) {
            bleBluetooth.closeBluetoothGatt();
            bleBluetooth.removeAllCallback();
        }
    }

    /**
     * 扫描到周围第一个符合名称的设备即连接，并持续监听与这个设备的连接状态
     */
    private boolean scanAndConnect(String deviceName, long time_out, final BleManagerConnectCallback connectCallback) {

        return bleBluetooth.scanNameAndConnect(deviceName, time_out, false, new BleBleGattCallback() {

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(gatt, status);
                }
                gatt.discoverServices();                // 连接上设备，则搜索服务
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (connectCallback != null) {
                    connectCallback.onServicesDiscovered(gatt, status);
                }
                BluetoothUtil.printServices(gatt);     // 打印该设备所有服务、特征值
                getBluetoothState();                   // 打印与该设备的当前状态
            }

            @Override
            public void onConnectFailure(BleException exception) {
                if (connectCallback != null) {
                    connectCallback.onConnectFailure(exception);
                }
                bleExceptionHandler.handleException(exception);
            }
        });
    }

    /**
     * 接收特征值改变通知--Notification
     */
    private boolean enableNotificationOfCharacteristic(String uuid_service, String uuid_notification,
                                                    String uuid_client, final BleManagerNotifyCallback notifyCallback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notification, null, uuid_client)
                .enableCharacteristicNotification(new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        if (notifyCallback != null) {
                            notifyCallback.onNotifyDataChangeSuccess(characteristic);
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        if (notifyCallback != null) {
                            notifyCallback.onNotifyDataChangeFailure(exception);
                        }
                        bleExceptionHandler.handleException(exception);
                    }
                });
    }

    /**
     * 接收特征值改变通知--Indication
     */
    private boolean enableIndicationOfCharacteristic(String uuid_service, String uuid_indication,
                                                  String uuid_client, final BleManagerIndicateCallback indicateCallback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indication, null, uuid_client)
                .enableCharacteristicIndication(new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        if (indicateCallback != null) {
                            indicateCallback.onIndicateDataChangeSuccess(characteristic);
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        if (indicateCallback != null) {
                            indicateCallback.onIndicateDataChangeFailure(exception);
                        }
                        bleExceptionHandler.handleException(exception);
                    }
                });
    }

    /**
     * 写特征值
     */
    private boolean writeDataToCharacteristic(String uuid_service, String uuid_write,
                                           String uuid_client, byte[] data, final BleManagerWriteCallback writeCallback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_write, null, uuid_client)
                .writeCharacteristic(data, new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        if (writeCallback != null) {
                            writeCallback.onDataWriteSuccess(characteristic);
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        if (writeCallback != null) {
                            writeCallback.onDataWriteFailure(exception);
                        }
                        bleExceptionHandler.handleException(exception);
                    }
                });
    }

    /**
     * 读特征值
     */
    private boolean readDataFromCharacteristic(String uuid_service, String uuid_read,
                                            String uuid_client, final BleManagerReadCallback readCallback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read, null, uuid_client)
                .readCharacteristic(new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        if (readCallback != null) {
                            readCallback.onDataReadSuccess(characteristic);
                        }
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        if (readCallback != null) {
                            readCallback.onDataReadFailure(exception);
                        }
                        bleExceptionHandler.handleException(exception);
                    }
                });
    }


    /********************************以下方法待完善*******************************/

    /**
     * 扫描所有符合名称的设备，并列出来
     * （获取周围所有的设备后，可以供用户自行选择与哪一个连接）
     */
    private void scanSpecifiedDevicePeriod(String deviceName, long timeOut) {

        bleBluetooth.startLeScan(new ListNameScanCallback(deviceName, timeOut) {

            @Override
            public void onScanTimeout() {

            }

            @Override
            public void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord) {

            }
        });
    }

    /**
     * 与某一指定的设备连接
     * (与 scanSpecifiedDevicePeriod方法 配合使用)
     */
    private void connect(BluetoothDevice device) {
        bleBluetooth.connect(device, true, new BleBleGattCallback() {
            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothUtil.printServices(gatt);
            }

            @Override
            public void onConnectFailure(BleException exception) {
                bleExceptionHandler.handleException(exception);
            }
        });
    }

}
