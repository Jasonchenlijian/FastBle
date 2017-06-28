package com.clj.blesample.demo;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.clj.blesample.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleRssiCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

/**
 * 代码示范
 */
public class CodeDemoActivity extends AppCompatActivity {

    // 下面的所有UUID及指令请根据实际设备替换
    private static final String UUID_SERVICE = "00000000-0000-0000-8000-00805f9b0000";
    private static final String UUID_INDICATE = "0000000-0000-0000-8000-00805f9b0000";
    private static final String UUID_NOTIFY = "00000000-0000-0000-8000-00805f9b0000";
    private static final String UUID_WRITE = "00000000-0000-0000-8000-00805f9b0000";
    private static final String UUID_READ = "00000000-0000-0000-8000-00805f9b0000";
    private static final String SAMPLE_WRITE_DATA = "000000000000000";                  // 要写入设备某一个character的指令

    private static final long TIME_OUT = 5000;                                          // 扫描超时时间
    private static final String DEVICE_NAME = "这里写你的设备名";                         // 符合连接规则的蓝牙设备名
    private static final String[] DEVICE_NAMES = new String[]{};                        // 符合连接规则的蓝牙设备名
    private static final String DEVICE_MAC = "这里写你的设备地址";                        // 符合连接规则的蓝牙设备地址

    private BleManager bleManager;
    private ScanResult scanResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
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
            public void onScanning(ScanResult result) {
                scanResult = result;
            }

            @Override
            public void onScanComplete(ScanResult[] results) {

            }
        });
    }

    /**
     * 当搜索到周围有设备之后，可以选择直接连某一个设备
     */
    private void connectDevice() {
        bleManager.connectDevice(scanResult, true, new BleGattCallback() {

            @Override
            public void onConnecting(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onConnectError(BleException exception) {

            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onDisConnected(BleException exception) {

            }

        });
    }

    /**
     * 扫描指定广播名的设备，并连接（唯一广播名）
     */
    private void scanAndConnect1() {
        bleManager.scanNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BleException exception) {

                    }

                });
    }

    /**
     * 扫描指定广播名的设备，并连接（模糊广播名）
     */
    private void scanAndConnect2() {
        bleManager.scanfuzzyNameAndConnect(
                DEVICE_NAME,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BleException exception) {

                    }

                });
    }

    /**
     * 扫描指定广播名的设备，并连接（多个广播名）
     */
    private void scanAndConnect3() {
        bleManager.scanNamesAndConnect(
                DEVICE_NAMES,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BleException exception) {

                    }

                });
    }

    /**
     * 扫描指定广播名的设备，并连接（模糊、多个广播名）
     */
    private void scanAndConnect4() {
        bleManager.scanfuzzyNamesAndConnect(
                DEVICE_NAMES,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BleException exception) {

                    }

                });
    }

    /**
     * 扫描指定物理地址的设备，并连接
     */
    private void scanAndConnect5() {
        bleManager.scanMacAndConnect(
                DEVICE_MAC,
                TIME_OUT,
                false,
                new BleGattCallback() {

                    @Override
                    public void onFoundDevice(ScanResult scanResult) {

                    }

                    @Override
                    public void onConnecting(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onConnectError(BleException exception) {

                    }

                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

                    }

                    @Override
                    public void onDisConnected(BleException exception) {

                    }

                });
    }

    /**
     * 取消搜索
     */
    private void cancelScan() {
        bleManager.cancelScan();
    }

    /**
     * notify
     */
    private boolean addNotify() {
        return bleManager.notify(
                UUID_SERVICE,
                UUID_NOTIFY,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {

                    }

                    @Override
                    public void onFailure(BleException exception) {

                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    /**
     * stop notify
     */
    private boolean stopNotify() {
        return bleManager.stopNotify(UUID_SERVICE, UUID_NOTIFY);
    }

    /**
     * indicate
     */
    private boolean addIndicate() {
        return bleManager.indicate(
                UUID_SERVICE,
                UUID_INDICATE,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {

                    }

                    @Override
                    public void onFailure(BleException exception) {

                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    /**
     * stop indicate
     */
    private boolean stopIndicate() {
        return bleManager.stopIndicate(UUID_SERVICE, UUID_INDICATE);
    }

    /**
     * write
     */
    private boolean write() {
        return bleManager.writeDevice(
                UUID_SERVICE,
                UUID_WRITE,
                HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {

                    }

                    @Override
                    public void onFailure(BleException exception) {

                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    /**
     * read
     */
    private boolean read() {
        return bleManager.readDevice(
                UUID_SERVICE,
                UUID_READ,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {

                    }

                    @Override
                    public void onFailure(BleException exception) {

                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    private boolean readRssi() {
        return bleManager.readRssi(new BleRssiCallback() {
            @Override
            public void onSuccess(int rssi) {

            }

            @Override
            public void onFailure(BleException exception) {

            }

            @Override
            public void onInitiatedResult(boolean result) {

            }
        });
    }


}
