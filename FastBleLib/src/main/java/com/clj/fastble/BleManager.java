package com.clj.fastble;

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

    private static final String TAG = BleManager.class.getSimpleName();

    private Context mContext;
    private BleBluetooth bleBluetooth;
    private DefaultBleExceptionHandler bleExceptionHandler;

    public BleManager(Context context) {
        this.mContext = context;

        if (bleBluetooth == null) {
            bleBluetooth = new BleBluetooth(context);
        }

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
        return bleBluetooth.startLeScan(callback);
    }

    /**
     * 已知周围某一设备，直接连接
     *
     * @param device      已知设备
     * @param autoConnect
     * @param callback
     */
    public void connectDevice(BluetoothDevice device,
                              boolean autoConnect,
                              BleGattCallback callback) {
        bleBluetooth.connect(device, autoConnect, callback);
    }

    /**
     * 扫描连接符合名称的设备，并持续监听连接状态
     *
     * @param deviceName  需要搜索的设备名称
     * @param time_out    搜索超时时间
     * @param autoConnect
     * @param callback
     * @return
     */
    public boolean scanNameAndConnect(String deviceName,
                                      long time_out,
                                      boolean autoConnect,
                                      BleGattCallback callback) {
        return bleBluetooth.scanNameAndConnect(deviceName, time_out, autoConnect, callback);
    }

    /**
     * 扫描连接符合地址的设备，并持续监听连接状态
     *
     * @param deviceMac   需要搜索的设备地址
     * @param time_out    搜索超时时间
     * @param autoConnect
     * @param callback
     * @return
     */
    public boolean scanMacAndConnect(String deviceMac,
                                     long time_out,
                                     boolean autoConnect,
                                     BleGattCallback callback) {
        return bleBluetooth.scanMacAndConnect(deviceMac, time_out, autoConnect, callback);
    }

    /**
     * notify
     *
     * @param uuid_service
     * @param uuid_notify
     * @param callback
     * @return
     */
    public boolean notifyDevice(String uuid_service,
                                String uuid_notify,
                                BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify, null)
                .enableCharacteristicNotify(callback, uuid_notify);
    }

    /**
     * indicate
     *
     * @param uuid_service
     * @param uuid_indicate
     * @param callback
     * @return
     */
    public boolean indicateDevice(String uuid_service,
                                  String uuid_indicate,
                                  BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indicate, null)
                .enableCharacteristicIndicate(callback, uuid_indicate);
    }

    /**
     * write
     *
     * @param uuid_service
     * @param uuid_write
     * @param data
     * @param callback
     * @return
     */
    public boolean writeDevice(String uuid_service,
                               String uuid_write,
                               byte[] data,
                               BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_write, null)
                .writeCharacteristic(data, callback, uuid_write);
    }

    /**
     * read
     *
     * @param uuid_service
     * @param uuid_read
     * @param callback
     * @return
     */
    public boolean readDevice(String uuid_service,
                              String uuid_read,
                              BleCharacterCallback callback) {
        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read, null)
                .readCharacteristic(callback, uuid_read);
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
        return mContext.getApplicationContext()
                .getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 开启蓝牙
     */
    public void enableBluetooth() {
        if (bleBluetooth != null) {
            bleBluetooth.enableBluetoothIfDisabled();
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
        return bleBluetooth != null && bleBluetooth.isBlueEnable();
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

}
