package com.clj.fastble;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.bluetooth.MultipleBluetoothController;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleConnectState;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.NotFoundDeviceException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.hanlder.DefaultBleExceptionHandler;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.scan.BleScanner;
import com.clj.fastble.utils.BleLog;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager {

    private Application context;
    private BleScanRuleConfig bleScanRuleConfig;
    private BleScanner bleScanner;
    private BluetoothAdapter bluetoothAdapter;
    private MultipleBluetoothController multipleBluetoothController;
    private DefaultBleExceptionHandler bleExceptionHandler;
    private BTStateReceiver mBtStateReceiver;

    public static final int DEFAULT_SCAN_TIME = 10000;
    private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
    private static final int DEFAULT_OPERATE_TIME = 5000;
    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;

    private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;
    private int operateTimeout = DEFAULT_OPERATE_TIME;

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
            multipleBluetoothController = new MultipleBluetoothController();
            bleScanRuleConfig = new BleScanRuleConfig();
            bleScanner = BleScanner.getInstance();
        }
    }

    /**
     * Get the Context
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * Get the BluetoothAdapter
     *
     * @return
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Get the BleScanner
     *
     * @return
     */
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
     * Handle Exception Information
     */
    public void handleException(BleException exception) {
        bleExceptionHandler.handleException(exception);
    }

    /**
     * Get the multiple Bluetooth Controller
     *
     * @return
     */
    public MultipleBluetoothController getMultipleBluetoothController() {
        return multipleBluetoothController;
    }

    /**
     * Configure scan and connection properties
     *
     * @param scanRuleConfig
     */
    public void initScanRule(BleScanRuleConfig scanRuleConfig) {
        this.bleScanRuleConfig = scanRuleConfig;
    }

    /**
     * Get the maximum number of connections
     *
     * @return
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * Set the maximum number of connections
     *
     * @param maxCount
     * @return BleManager
     */
    public BleManager setMaxConnectCount(int maxCount) {
        if (maxCount > DEFAULT_MAX_MULTIPLE_DEVICE)
            maxCount = DEFAULT_MAX_MULTIPLE_DEVICE;
        this.maxConnectCount = maxCount;
        return this;
    }

    /**
     * Get operate timeout
     *
     * @return
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * Set operate timeout
     *
     * @param operateTimeout
     * @return BleManager
     */
    public BleManager setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }

    /**
     * print log?
     *
     * @param enable
     * @return BleManager
     */
    public BleManager enableLog(boolean enable) {
        BleLog.isPrint = enable;
        return this;
    }

    /**
     * scan device around
     *
     * @param callback
     */
    public void scan(BleScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleScanCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new OtherException("BlueTooth not enable!"));
            return;
        }

        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getScanTimeOut();

        bleScanner.scan(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * scan device then connect
     *
     * @param callback
     */
    public void scanAndConnect(BleScanAndConnectCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleScanAndConnectCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new OtherException("BlueTooth not enable!"));
            return;
        }

        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getScanTimeOut();

        bleScanner.scanAndConnect(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * connect a known device
     *
     * @param bleDevice
     * @param bleGattCallback
     * @return
     */
    public BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback) {
        if (bleGattCallback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new OtherException("BlueTooth not enable!"));
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
     * Cancel scan
     */
    public void cancelScan() {
        bleScanner.stopLeScan();
    }

    /**
     * notify
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param callback
     */
    public void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       BleNotifyCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleNotifyCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onNotifyFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_notify)
                    .enableCharacteristicNotify(callback, uuid_notify);
        }
    }

    /**
     * indicate
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @param callback
     */
    public void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         BleIndicateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleIndicateCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onIndicateFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_indicate)
                    .enableCharacteristicIndicate(callback, uuid_indicate);
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @return
     */
    public boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify) {
        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
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
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @return
     */
    public boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate) {
        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
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
     * @param bleDevice
     * @param uuid_service
     * @param uuid_write
     * @param data
     * @param callback
     */
    public void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      BleWriteCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleWriteCallback can not be Null!");
        }

        if (data == null) {
            BleLog.e("data is Null!");
            callback.onWriteFailure(new OtherException("data is Null !"));
            return;
        }

        if (data.length > 20) {
            BleLog.w("data's length beyond 20!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onWriteFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_write)
                    .writeCharacteristic(data, callback, uuid_write);
        }
    }

    /**
     * read
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_read
     * @param callback
     */
    public void read(BleDevice bleDevice,
                     String uuid_service,
                     String uuid_read,
                     BleReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onReadFailure(new OtherException("this device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_read)
                    .readCharacteristic(callback, uuid_read);
        }
    }

    /**
     * read Rssi
     *
     * @param bleDevice
     * @param callback
     */
    public void readRssi(BleDevice bleDevice,
                         BleRssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onRssiFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector().readRemoteRssi(callback);
        }
    }

    /**
     * set Mtu
     *
     * @param bleDevice
     * @param mtu
     * @param callback
     */
    public void setMtu(BleDevice bleDevice,
                       int mtu,
                       BleMtuChangedCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
        }

        if (mtu > DEFAULT_MAX_MTU) {
            BleLog.e("requiredMtu should lower than 512 !");
            callback.onsetMTUFailure(new OtherException("requiredMtu should lower than 512 !"));
            return;
        }

        if (mtu < DEFAULT_MTU) {
            BleLog.e("requiredMtu should higher than 23 !");
            callback.onsetMTUFailure(new OtherException("requiredMtu should higher than 23 !"));
            return;
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onsetMTUFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector().setMtu(mtu, callback);
        }
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
     * Open bluetooth
     */
    public void enableBluetooth() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.enable();
        }
    }

    /**
     * Open bluetooth with bluetooth state listener
     * @param listener
     */
    public void enableBluetooth(OnBTOpenStateListener listener) {
        this.btOpenStateListener=listener;
        if (bluetoothAdapter != null) {
            mBtStateReceiver = new BTStateReceiver();
            registerBtStateReceiver();
            bluetoothAdapter.enable();
        }
    }


    /**
     * Disable bluetooth
     */
    public void disableBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled())
                bluetoothAdapter.disable();
        }
    }

    /**
     * judge Bluetooth is enable
     *
     * @return
     */
    public boolean isBlueEnable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }


    public BleDevice convertBleDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord, long timestampNanos) {
        return new BleDevice(bluetoothDevice, rssi, scanRecord, timestampNanos);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BleDevice convertBleDevice(ScanResult scanResult) {
        if (scanResult == null) {
            throw new IllegalArgumentException("scanResult can not be Null!");
        }
        BluetoothDevice bluetoothDevice = scanResult.getDevice();
        int rssi = scanResult.getRssi();
        ScanRecord scanRecord = scanResult.getScanRecord();
        byte[] bytes = null;
        if (scanRecord != null)
            bytes = scanRecord.getBytes();
        long timestampNanos = scanResult.getTimestampNanos();
        return new BleDevice(bluetoothDevice, rssi, bytes, timestampNanos);
    }

    public BleBluetooth getBleBluetooth(BleDevice bleDevice) {
        if (multipleBluetoothController != null) {
            return multipleBluetoothController.getBleBluetooth(bleDevice);
        }
        return null;
    }

    public List<BleDevice> getAllConnectedDevice() {
        if (multipleBluetoothController == null)
            return null;
        return multipleBluetoothController.getDeviceList();
    }

    public BluetoothGatt getBluetoothGatt(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            return bleBluetooth.getBluetoothGatt();
        return null;
    }

    public void removeConnectGattCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeConnectGattCallback();
    }

    public void removeRssiCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeRssiCallback();
    }

    public void removeMtuChangedCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeMtuChangedCallback();
    }

    public void removeNotifyCallback(BleDevice bleDevice, String uuid_notify) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeNotifyCallback(uuid_notify);
    }

    public void removeIndicateCallback(BleDevice bleDevice, String uuid_indicate) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeIndicateCallback(uuid_indicate);
    }

    public void removeWriteCallback(BleDevice bleDevice, String uuid_write) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeWriteCallback(uuid_write);
    }

    public void removeReadCallback(BleDevice bleDevice, String uuid_read) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeReadCallback(uuid_read);
    }

    public void clearCharacterCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.clearCharacterCallback();
    }

    public BleScanState getScanSate() {
        return bleScanner.getScanState();
    }

    public BleConnectState getConnectState(BleDevice bleDevice) {
        if (multipleBluetoothController != null) {
            return multipleBluetoothController.getConnectState(bleDevice);
        }
        return BleConnectState.CONNECT_IDLE;
    }

    public boolean isConnected(BleDevice bleDevice) {
        if (multipleBluetoothController != null) {
            return multipleBluetoothController.isContainDevice(bleDevice);
        }
        return false;
    }

    public void disconnect(BleDevice bleDevice) {
        if (multipleBluetoothController != null) {
            multipleBluetoothController.disconnect(bleDevice);
        }
    }

    public void disconnectAllDevice() {
        if (multipleBluetoothController != null) {
            multipleBluetoothController.disconnectAllDevice();
        }
    }

    public void destroy() {
        if (multipleBluetoothController != null) {
            multipleBluetoothController.destroy();
        }
    }
    private void registerBtStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mBtStateReceiver, filter);
    }

    private void unRegisterBtStateReceiver() {
        try {
            context.unregisterReceiver(mBtStateReceiver);
        } catch (Exception e) {
        } catch (Throwable e) {
        }

    }
    private OnBTOpenStateListener btOpenStateListener = null;

    public interface OnBTOpenStateListener {
        void onBTOpen();
    }

    private class BTStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BleLog.i("action=" + action);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent
                    .getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                BleLog.i("state=" + state);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        BleLog.i("ACTION_STATE_CHANGED:  STATE_TURNING_ON");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        BleLog.i("ACTION_STATE_CHANGED:  STATE_ON");
                        if (null != btOpenStateListener){
                            btOpenStateListener.onBTOpen();
                        }
                        unRegisterBtStateReceiver();
                        break;
                    default:
                }
            }
        }
    }

}
