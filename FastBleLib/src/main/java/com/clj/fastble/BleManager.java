package com.clj.fastble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.bluetooth.BleBluetoothPool;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleRssiCallback;
import com.clj.fastble.conn.BleScanCallback;
import com.clj.fastble.data.ConnectState;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.BlueToothNotEnableException;
import com.clj.fastble.exception.NotFoundDeviceException;
import com.clj.fastble.exception.hanlder.DefaultBleExceptionHandler;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.scan.BleScanner;
import com.clj.fastble.utils.BleLog;

import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager {

    private Context mContext;
    private BleBluetooth mBleBluetooth;
    private BleScanner bleScanner;
    private BluetoothAdapter bluetoothAdapter;  // 蓝牙适配器
    private BleScanRuleConfig mScanRuleConfig;
    private DefaultBleExceptionHandler mBleExceptionHandler;
    private BleBluetoothPool bleBluetoothPool;          // 设备连接池

    public static BleManager getInstance() {
        return BleManagerHolder.sBleManager;
    }

    private static class BleManagerHolder {
        private static final BleManager sBleManager = new BleManager();
    }

    public void init(Context context) {
        if (this.mContext == null && context != null) {
            this.mContext = context.getApplicationContext();

            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null)
                bluetoothAdapter = bluetoothManager.getAdapter();
            mBleExceptionHandler = new DefaultBleExceptionHandler();
            bleBluetoothPool = new BleBluetoothPool();
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BleScanner getBleScanner() {
        return bleScanner;
    }

    /**
     * handle Exception Information
     */
    public void handleException(BleException exception) {
        mBleExceptionHandler.handleException(exception);
    }

    /**
     * Configuring scan and connection properties
     *
     * @param scanRuleConfig
     */
    public void initScanRule(BleScanRuleConfig scanRuleConfig) {
        this.mScanRuleConfig = scanRuleConfig;
    }

    /**
     * get the ScanRuleConfig
     *
     * @return
     */
    public BleScanRuleConfig getScanRuleConfig() {
        return mScanRuleConfig;
    }

    /**
     * scan device around
     *
     * @param callback
     * @return
     */
    public boolean scan(BleScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ScanCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new BlueToothNotEnableException());
            return false;
        }

        UUID[] serviceUuids = mScanRuleConfig.getServiceUuids();
        String[] deviceNames = mScanRuleConfig.getDeviceNames();
        String deviceMac = mScanRuleConfig.getDeviceMac();
        boolean fuzzy = mScanRuleConfig.isFuzzy();
        long timeOut = mScanRuleConfig.getTimeOut();

        return bleScanner.scan(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * connect a known device
     *
     * @param scanResult
     * @param callback
     */
    public BluetoothGatt connect(ScanResult scanResult, BleGattCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new BlueToothNotEnableException());
            return null;
        }

        if (scanResult == null || scanResult.getDevice() == null) {
            callback.onConnectError(new NotFoundDeviceException());
        } else {
            callback.onFoundDevice(scanResult);
            boolean autoConnect = mScanRuleConfig.isAutoConnect();
            return mBleBluetooth.connect(scanResult, autoConnect, callback);
        }

        return null;
    }

    /**
     * scan device then connect
     *
     * @param callback
     */
    public void scanAndConnect(BleGattCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new BlueToothNotEnableException());
            return;
        }

        UUID[] serviceUuids = mScanRuleConfig.getServiceUuids();
        String[] deviceNames = mScanRuleConfig.getDeviceNames();
        String deviceMac = mScanRuleConfig.getDeviceMac();
        boolean autoConnect = mScanRuleConfig.isAutoConnect();
        boolean fuzzy = mScanRuleConfig.isFuzzy();
        long timeOut = mScanRuleConfig.getTimeOut();

        bleScanner.scanAndConnect(serviceUuids, deviceNames, deviceMac, fuzzy, autoConnect, timeOut, callback);
    }

    /**
     * cancel scan
     */
    public void cancelScan() {
        bleScanner.stopLeScan();
    }

    /**
     * notify
     *
     * @param uuid_service
     * @param uuid_notify
     * @param callback
     * @return
     */
    public boolean notify(String uuid_service,
                          String uuid_notify,
                          BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        return mBleBluetooth.newBleConnector()
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
    public boolean indicate(String uuid_service,
                            String uuid_indicate,
                            BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        return mBleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indicate, null)
                .enableCharacteristicIndicate(callback, uuid_indicate);
    }

    /**
     * stop notify, remove callback
     *
     * @param uuid_service
     * @param uuid_notify
     * @return
     */
    public boolean stopNotify(String uuid_service, String uuid_notify) {
        boolean success = mBleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify, null)
                .disableCharacteristicNotify();
        if (success) {
            mBleBluetooth.removeGattCallback(uuid_notify);
        }
        return success;
    }

    /**
     * stop indicate, remove callback
     *
     * @param uuid_service
     * @param uuid_indicate
     * @return
     */
    public boolean stopIndicate(String uuid_service, String uuid_indicate) {
        boolean success = mBleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indicate, null)
                .disableCharacteristicIndicate();
        if (success) {
            mBleBluetooth.removeGattCallback(uuid_indicate);
        }
        return success;
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
    public boolean write(String uuid_service,
                         String uuid_write,
                         byte[] data,
                         BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        if (data == null) {
            BleLog.e("data is Null!");
            return false;
        }

        if (data.length > 20) {
            BleLog.w("data's length beyond 20!");
        }

        return mBleBluetooth.newBleConnector()
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
    public boolean read(String uuid_service,
                        String uuid_read,
                        BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        return mBleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read, null)
                .readCharacteristic(callback, uuid_read);
    }

    /**
     * read Rssi
     *
     * @param callback
     * @return
     */
    public boolean readRssi(BleRssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        return mBleBluetooth.newBleConnector()
                .readRemoteRssi(callback);
    }

    /**
     * refresh Device Cache
     */
    public void refreshDeviceCache() {
        mBleBluetooth.refreshDeviceCache();
    }

    /**
     * close gatt
     */
    public void closeBluetoothGatt() {
        if (mBleBluetooth != null) {
            mBleBluetooth.clearCallback();
            try {
                mBleBluetooth.closeBluetoothGatt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * is support ble?
     *
     * @return
     */
    public boolean isSupportBle() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && mContext.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * open bluetooth
     */
    public void enableBluetooth() {
        if (mBleBluetooth != null) {
            mBleBluetooth.enableBluetoothIfDisabled();
        }
    }

    /**
     * close bluetooth
     */
    public void disableBluetooth() {
        if (mBleBluetooth != null) {
            mBleBluetooth.disableBluetooth();
        }
    }

    public boolean isBlueEnable() {
        return mBleBluetooth != null && mBleBluetooth.isBlueEnable();
    }

    public boolean isInScanning() {
        return mBleBluetooth.isInScanning();
    }

    public boolean isConnectingOrConnected() {
        return mBleBluetooth.isConnectingOrConnected();
    }

    public boolean isConnected() {
        return mBleBluetooth.isConnected();
    }

    public boolean isServiceDiscovered() {
        return mBleBluetooth.isServiceDiscovered();
    }

    /**
     * remove callback form a character
     */
    public void stopListenCharacterCallback(String uuid) {
        mBleBluetooth.removeGattCallback(uuid);
    }

    /**
     * remove callback for gatt connect
     */
    public void stopListenConnectCallback() {
        mBleBluetooth.removeConnectGattCallback();
    }

    /**
     * 获取连接池中的设备镜像，如果没有连接则返回空
     *
     * @param bluetoothLeDevice
     * @return
     */
    public BleBluetooth getDeviceMirror(ScanResult bluetoothLeDevice) {
        if (bleBluetoothPool != null) {
            return bleBluetoothPool.getBleBluetooth(bluetoothLeDevice);
        }
        return null;
    }

    /**
     * 获取该设备连接状态
     *
     * @param bluetoothLeDevice
     * @return
     */
    public ConnectState getConnectState(ScanResult bluetoothLeDevice) {
        if (bleBluetoothPool != null) {
            return bleBluetoothPool.getConnectState(bluetoothLeDevice);
        }
        return ConnectState.CONNECT_DISCONNECT;
    }

    /**
     * 判断该设备是否已连接
     *
     * @param bluetoothLeDevice
     * @return
     */
    public boolean isConnect(ScanResult bluetoothLeDevice) {
        if (bleBluetoothPool != null) {
            return bleBluetoothPool.isContainDevice(bluetoothLeDevice);
        }
        return false;
    }

    /**
     * 断开某一个设备
     *
     * @param bluetoothLeDevice
     */
    public void disconnect(ScanResult bluetoothLeDevice) {
        if (bleBluetoothPool != null) {
            bleBluetoothPool.disconnect(bluetoothLeDevice);
        }
    }

    /**
     * 断开所有设备
     */
    public void disconnect() {
        if (bleBluetoothPool != null) {
            bleBluetoothPool.disconnect();
        }
    }

    /**
     * 清除资源，在退出应用时调用
     */
    public void clear() {
        if (bleBluetoothPool != null) {
            bleBluetoothPool.clear();
        }
    }

    /**
     * 获取Context
     *
     * @return 返回Context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * 获取设备镜像池
     *
     * @return
     */
    public BleBluetoothPool getDeviceMirrorPool() {
        return bleBluetoothPool;
    }

}
