package com.clj.fastble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.hanlder.DefaultBleExceptionHandler;
import com.clj.fastble.scan.ListScanCallback;

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
     */
    public void init(Context context) {

        if (bleBluetooth == null) {
            bleBluetooth = new BleBluetooth(context);
        }
        bleBluetooth.enableBluetoothIfDisabled((Activity) context, 1);
        bleExceptionHandler = new DefaultBleExceptionHandler(context);
    }

    /**
     * 显示异常信息
     */
    public void handleException(BleException exception) {
        bleExceptionHandler.handleException(exception);
    }


    /**
     * 扫描周围所有设备
     */
    public boolean scanDevice(ListScanCallback callback) {
        return scanAllDevice(callback);
    }

    /**
     * 直接连接某一设备
     */
    public void connectDevice(BluetoothDevice device,
                                 BleGattCallback callback) {
        connect(device, callback);
    }

    /**
     * 扫描连接符合名称的设备，并监听数据变化
     */
    public boolean connectDevice(String deviceName,
                                 long time_out,
                                 BleGattCallback callback) {
        return scanNameAndConnect(deviceName, time_out, callback);
    }

    /**
     * notify
     */
    public boolean notifyDevice(String uuid_service,
                                String uuid_notification,
                                String uuid_client,
                                BleCharacterCallback callback) {
        return enableNotificationOfCharacteristic(uuid_service, uuid_notification, uuid_client, callback);
    }

    /**
     * indicate
     */
    public boolean indicateDevice(String uuid_service,
                                  String uuid_indication,
                                  String uuid_client,
                                  BleCharacterCallback callback) {
        return enableIndicationOfCharacteristic(uuid_service, uuid_indication, uuid_client, callback);
    }

    /**
     * 向设备写特征值
     */
    public boolean writeDevice(String uuid_service,
                               String uuid_write,
                               String uuid_client,
                               byte[] data,
                               BleCharacterCallback callback) {
        return writeDataToCharacteristic(uuid_service, uuid_write, uuid_client, data, callback);
    }

    /**
     * 向设备读特征值
     */
    public boolean readDevice(String uuid_service,
                              String uuid_read,
                              String uuid_client,
                              BleCharacterCallback callback) {
        return readDataFromCharacteristic(uuid_service, uuid_read, uuid_client, callback);
    }

    /**
     * 获取当前状态
     */
    public void getBluetoothState() {
        Log.d(TAG, "ConnectionState:  " + bleBluetooth.getConnectionState()
                + "\nisInScanning: " + bleBluetooth.isInScanning()
                + "\nisConnected: " + bleBluetooth.isConnected()
                + "\nisServiceDiscovered: " + bleBluetooth.isServiceDiscovered());
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
     * 开启蓝牙
     */
    public void enableBluetooth() {
        if (bleBluetooth != null) {
            bleBluetooth.enableBluetooth();
        }
    }

    /**
     * 关闭蓝牙
     */
    public void disableBluetooth() {
        if (bleBluetooth != null) {
            bleBluetooth.disableBluetooth();
        }
    }

    /**
     * 是否在扫描状态
     */
    public boolean isInScanning() {
        return bleBluetooth.isInScanning();
    }

    /**
     * 是否在连接或已连接状态
     */
    public boolean isConnectingOrConnected() {
        return bleBluetooth.isConnectingOrConnected();
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return bleBluetooth.isConnected();
    }

    /**
     * 服务是否已发现
     */
    public boolean isServiceDiscovered() {
        return bleBluetooth.isServiceDiscovered();
    }

    /**
     * 将某一不再需要的接口移除(notify、indicate、 write、read)
     */
    public void removeBleCharacterCallback(BleCharacterCallback callback) {
        bleBluetooth.removeGattCallback(callback.getBluetoothGattCallback());
    }

    /**
     * 将某一不再需要的接口移除(connect)
     */
    public void removeBleBleGattCallback(BleGattCallback callback) {
        bleBluetooth.removeGattCallback(callback);
    }


    /*************************************inner method****************************************************/

    /**
     * 扫描周围设备
     * （获取周围所有的设备后，可以供用户自行选择与哪一个连接）
     */
    private boolean scanAllDevice(ListScanCallback callback) {

        return bleBluetooth.startLeScan(callback);
    }

    /**
     * 与某一指定的设备连接
     * (与 scanSpecifiedDevicePeriod方法 配合使用)
     */
    private void connect(BluetoothDevice device, BleGattCallback callback) {
        bleBluetooth.connect(device, true, callback);
    }

    /**
     * 扫描到周围第一个符合名称的设备即连接，并持续监听与这个设备的连接状态
     */
    private boolean scanNameAndConnect(String deviceName, long time_out, BleGattCallback callback) {

        return bleBluetooth.scanNameAndConnect(deviceName, time_out, false, callback);
    }

    /**
     * 接收特征值改变通知--Notification
     */
    private boolean enableNotificationOfCharacteristic(String uuid_service, String uuid_notification,
                                                       String uuid_client, final BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notification, null, uuid_client)
                .enableCharacteristicNotification(callback);
    }

    /**
     * 接收特征值改变通知--Indication
     */
    private boolean enableIndicationOfCharacteristic(String uuid_service, String uuid_indication,
                                                     String uuid_client, final BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indication, null, uuid_client)
                .enableCharacteristicIndication(callback);
    }

    /**
     * 写特征值
     */
    private boolean writeDataToCharacteristic(String uuid_service, String uuid_write,
                                              String uuid_client, byte[] data, final BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_write, null, uuid_client)
                .writeCharacteristic(data, callback);
    }

    /**
     * 读特征值
     */
    private boolean readDataFromCharacteristic(String uuid_service, String uuid_read,
                                               String uuid_client, final BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read, null, uuid_client)
                .readCharacteristic(callback);
    }


    /********************************wait*******************************/

}
