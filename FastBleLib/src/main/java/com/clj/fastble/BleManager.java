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
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.ConnectState;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.BlueToothNotEnableException;
import com.clj.fastble.exception.NotFoundDeviceException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.hanlder.DefaultBleExceptionHandler;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.scan.BleScanner;
import com.clj.fastble.utils.BleLog;

import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager {

    private Context mContext;
    private BleScanRuleConfig mScanRuleConfig;
    private BleScanner bleScanner;
    private BluetoothAdapter bluetoothAdapter;
    private BleBluetoothPool bleBluetoothPool;
    private DefaultBleExceptionHandler mBleExceptionHandler;


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
            bleScanner = BleScanner.getInstance();
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
     * connect a known device
     *
     * @param bleDevice
     * @param bleGattCallback
     */
    public BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback) {
        if (bleGattCallback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new BlueToothNotEnableException());
            return null;
        }

        if (bleDevice == null || bleDevice.getDevice() == null) {
            bleGattCallback.onConnectError(new NotFoundDeviceException());
        } else {
            bleGattCallback.onFoundDevice(bleDevice);
            BleBluetooth bleBluetooth = new BleBluetooth(bleDevice);
            boolean autoConnect = mScanRuleConfig.isAutoConnect();
            return bleBluetooth.connect(bleDevice, autoConnect, bleGattCallback);
        }

        return null;
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
    public boolean notify(BleDevice bleDevice,
                          String uuid_service,
                          String uuid_notify,
                          BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onFailure(new OtherException("this device not connect!"));
            return false;
        } else {
            return bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_notify)
                    .enableCharacteristicNotify(callback, uuid_notify);
        }
    }

    /**
     * indicate
     *
     * @param uuid_service
     * @param uuid_indicate
     * @param callback
     * @return
     */
    public boolean indicate(BleDevice bleDevice,
                            String uuid_service,
                            String uuid_indicate,
                            BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onFailure(new OtherException("this device not connect!"));
            return false;
        } else {
            return bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_indicate)
                    .enableCharacteristicIndicate(callback, uuid_indicate);
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param uuid_service
     * @param uuid_notify
     * @return
     */
    public boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify) {
        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            return false;
        }
        boolean success = bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify)
                .disableCharacteristicNotify();
        if (success) {
            bleBluetooth.removeCharacterCallback(uuid_notify);
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
    public boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate) {
        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            return false;
        }
        boolean success = bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indicate)
                .disableCharacteristicIndicate();
        if (success) {
            bleBluetooth.removeCharacterCallback(uuid_indicate);
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
    public boolean write(BleDevice bleDevice,
                         String uuid_service,
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

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onFailure(new OtherException("this device not connect!"));
            return false;
        }

        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_write)
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
    public boolean read(BleDevice bleDevice,
                        String uuid_service,
                        String uuid_read,
                        BleCharacterCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onFailure(new OtherException("this device not connect!"));
            return false;
        }

        return bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read)
                .readCharacteristic(callback, uuid_read);
    }

    /**
     * read Rssi
     *
     * @param callback
     * @return
     */
    public boolean readRssi(BleDevice bleDevice,
                            BleRssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onFailure(new OtherException("this device not connect!"));
            return false;
        }

        return bleBluetooth.newBleConnector().readRemoteRssi(callback);
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
        if (bluetoothAdapter != null) {
            bluetoothAdapter.enable();
        }
    }

    /**
     * close bluetooth
     */
    public void disableBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled())
                bluetoothAdapter.disable();
        }
    }

    public boolean isBlueEnable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }


    /**
     * 获取连接池中的设备镜像，如果没有连接则返回空
     *
     * @param bluetoothLeDevice
     * @return
     */
    public BleBluetooth getDeviceMirror(BleDevice bluetoothLeDevice) {
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
    public ConnectState getConnectState(BleDevice bluetoothLeDevice) {
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
    public boolean isConnect(BleDevice bluetoothLeDevice) {
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
    public void disconnect(BleDevice bluetoothLeDevice) {
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
