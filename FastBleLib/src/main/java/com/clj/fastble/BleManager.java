package com.clj.fastble;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.bluetooth.BleBluetoothPool;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleConnectState;
import com.clj.fastble.data.BleDevice;
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

    private static Application context;
    private BleScanRuleConfig bleScanRuleConfig;
    private BleScanner bleScanner;
    private BluetoothAdapter bluetoothAdapter;
    private BleBluetoothPool bleBluetoothPool;
    private DefaultBleExceptionHandler bleExceptionHandler;


    public static BleManager getInstance() {
        return BleManagerHolder.sBleManager;
    }

    private static class BleManagerHolder {
        private static final BleManager sBleManager = new BleManager();
    }

    public void init(Application app) {
        if (context == null && app != null) {
            context = app;
            BluetoothManager bluetoothManager = (BluetoothManager) context
                    .getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null)
                bluetoothAdapter = bluetoothManager.getAdapter();
            bleExceptionHandler = new DefaultBleExceptionHandler();
            bleBluetoothPool = new BleBluetoothPool();
            bleScanner = BleScanner.getInstance();
        }
    }

    public Context getContext() {
        return context;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BleScanner getBleScanner() {
        return bleScanner;
    }

    /**
     * get the ScanRuleConfig
     *
     * @return
     */
    public BleScanRuleConfig getScanRuleConfig() {
        return bleScanRuleConfig;
    }

    /**
     * handle Exception Information
     */
    public void handleException(BleException exception) {
        bleExceptionHandler.handleException(exception);
    }

    /**
     * 获取设备镜像池
     *
     * @return
     */
    public BleBluetoothPool getBleBluetoothPool() {
        return bleBluetoothPool;
    }

    /**
     * Configuring scan and connection properties
     *
     * @param scanRuleConfig
     */
    public void initScanRule(BleScanRuleConfig scanRuleConfig) {
        this.bleScanRuleConfig = scanRuleConfig;
    }

    /**
     * scan device around
     *
     * @param callback
     * @return
     */
    public void scan(BleScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleScanPresenter can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new BlueToothNotEnableException());
            return;
        }

        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getTimeOut();

        bleScanner.scan(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * scan device then connect
     *
     * @param callback
     */
    public void scanAndConnect(BleScanAndConnectCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new BlueToothNotEnableException());
            return;
        }

        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getTimeOut();

        bleScanner.scanAndConnect(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
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
            bleGattCallback.onConnectFail(new NotFoundDeviceException());
        } else {
            BleBluetooth bleBluetooth = new BleBluetooth(bleDevice);
            boolean autoConnect = bleScanRuleConfig.isAutoConnect();
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
    public void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       BleNotifyCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onNotifyFailure(new OtherException("this device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
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
    public void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         BleIndicateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onIndicateFailure(new OtherException("this device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
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
            bleBluetooth.removeNotifyCallback(uuid_notify);
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
            bleBluetooth.removeIndicateCallback(uuid_indicate);
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
    public void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      BleWriteCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        if (data == null) {
            BleLog.e("data is Null!");
            return;
        }

        if (data.length > 20) {
            BleLog.w("data's length beyond 20!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onWriteFailure(new OtherException("this device not connect!"));
            return;
        }

        bleBluetooth.newBleConnector()
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
    public void read(BleDevice bleDevice,
                     String uuid_service,
                     String uuid_read,
                     BleReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleCharacterCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onReadFailure(new OtherException("this device not connect!"));
            return;
        }

        bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_read)
                .readCharacteristic(callback, uuid_read);
    }

    /**
     * read Rssi
     *
     * @param callback
     * @return
     */
    public void readRssi(BleDevice bleDevice,
                         BleRssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = bleBluetoothPool.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onRssiFailure(new OtherException("this device not connect!"));
            return;
        }

        bleBluetooth.newBleConnector().readRemoteRssi(callback);
    }

    /**
     * is support ble?
     *
     * @return
     */
    public boolean isSupportBle() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
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
     * @param bleDevice
     * @return
     */
    public BleBluetooth getBleBluetooth(BleDevice bleDevice) {
        if (bleBluetoothPool != null) {
            return bleBluetoothPool.getBleBluetooth(bleDevice);
        }
        return null;
    }

    /**
     * 获取该设备连接状态
     *
     * @param bleDevice
     * @return
     */
    public BleConnectState getConnectState(BleDevice bleDevice) {
        if (bleBluetoothPool != null) {
            return bleBluetoothPool.getConnectState(bleDevice);
        }
        return BleConnectState.CONNECT_DISCONNECT;
    }

    /**
     * 判断该设备是否已连接
     *
     * @param bleDevice
     * @return
     */
    public boolean isConnect(BleDevice bleDevice) {
        if (bleBluetoothPool != null) {
            return bleBluetoothPool.isContainDevice(bleDevice);
        }
        return false;
    }

    /**
     * 断开某一个设备
     *
     * @param bleDevice
     */
    public void disconnect(BleDevice bleDevice) {
        if (bleBluetoothPool != null) {
            bleBluetoothPool.disconnect(bleDevice);
        }
    }

    /**
     * 断开所有设备
     */
    public void disconnectAllDevice() {
        if (bleBluetoothPool != null) {
            bleBluetoothPool.disconnectAllDevice();
        }
    }

    /**
     * 清除资源，在退出应用时调用
     */
    public void destroy() {
        if (bleBluetoothPool != null) {
            bleBluetoothPool.destroy();
        }
    }

}
