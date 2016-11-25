package com.clj.fastble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
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

    private Context mContext;

    private static BleManager manager;

    private static BleBluetooth bleBluetooth;

    private DefaultBleExceptionHandler bleExceptionHandler;

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

        // mContext = context;
           mContext = context.getApplicationContext();
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
                                BleCharacterCallback callback) {
        return enableNotifyOfCharacteristic(uuid_service, uuid_notification, callback);
    }

    /**
     * indicate
     */
    public boolean indicateDevice(String uuid_service,
                                  String uuid_indication,
                                  BleCharacterCallback callback) {
        return enableIndicateOfCharacteristic(uuid_service, uuid_indication, callback);
    }

    /**
     * 向设备写特征值
     */
    public boolean writeDevice(String uuid_service,
                               String uuid_write,
                               byte[] data,
                               BleCharacterCallback callback) {
        return writeDataToCharacteristic(uuid_service, uuid_write, data, callback);
    }

    /**
     * 向设备读特征值
     */
    public boolean readDevice(String uuid_service,
                              String uuid_read,
                              BleCharacterCallback callback) {
        return readDataFromCharacteristic(uuid_service, uuid_read, callback);
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
        if (bleBluetooth != null) {
            bleBluetooth.clearCallback();
            try {
                bleBluetooth.closeBluetoothGatt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 当前设备是否支持BLE
     */
    public boolean isSupportBle() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
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
     * 本机蓝牙是否打开
     */
    public boolean isBlueEnable() {
        if (bleBluetooth != null) {
            return bleBluetooth.isBlueEnable();
        }
        return false;
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
     * 移除某一特征值的监听回调
     */
    public void stopListenCharacterCallback(String uuid) {
        bleBluetooth.removeGattCallback(uuid);
    }

    /**
     * 移除连接状态监听回调
     */
    public void stopListenConnectCallback() {
        bleBluetooth.removeConnectGattCallback();
    }

    /**
     * 停止notify
     */
    public boolean stopNotify(String uuid_service, String uuid_notify) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify, null)
                .disableCharacteristicNotify();
    }

    /**
     * 停止indicate
     */
    public boolean stopIndicate(String uuid_service, String uuid_notify) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify, null)
                .disableCharacteristicIndicate();
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
     * notify
     */
    private boolean enableNotifyOfCharacteristic(String uuid_service, String uuid_notify,
                                                 BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify, null)
                .enableCharacteristicNotify(callback, uuid_notify);
    }

    /**
     * indicate
     */
    private boolean enableIndicateOfCharacteristic(String uuid_service, String uuid_indicate,
                                                   BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indicate, null)
                .enableCharacteristicIndicate(callback, uuid_indicate);
    }

    /**
     * write
     */
    private boolean writeDataToCharacteristic(String uuid_service, String uuid_write,
                                              byte[] data, BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_write, null)
                .writeCharacteristic(data, callback, uuid_write);
    }

    /**
     * read
     */
    private boolean readDataFromCharacteristic(String uuid_service, String uuid_read,
                                               BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read, null)
                .readCharacteristic(callback, uuid_read);
    }

}
