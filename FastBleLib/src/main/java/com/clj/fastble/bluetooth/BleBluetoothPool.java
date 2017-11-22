package com.clj.fastble.bluetooth;


import com.clj.fastble.data.BleConfig;
import com.clj.fastble.data.ConnectState;
import com.clj.fastble.data.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BleBluetoothPool {

    private final BleLruHashMap<String, BleBluetooth> bleLruHashMap;

    public BleBluetoothPool() {
        bleLruHashMap = new BleLruHashMap<>(BleConfig.getInstance().getMaxConnectCount());
    }

    public BleBluetoothPool(int BleBluetoothSize) {
        bleLruHashMap = new BleLruHashMap<>(BleBluetoothSize);
    }

    /**
     * 添加设备镜像
     *
     * @param bleBluetooth
     */
    public synchronized void addBleBluetooth(BleBluetooth bleBluetooth) {
        if (bleBluetooth == null) {
            return;
        }
        if (!bleLruHashMap.containsKey(bleBluetooth.getUniqueSymbol())) {
            bleLruHashMap.put(bleBluetooth.getUniqueSymbol(), bleBluetooth);
        }
    }

    /**
     * 删除设备镜像
     *
     * @param bleBluetooth
     */
    public synchronized void removeBleBluetooth(BleBluetooth bleBluetooth) {
        if (bleBluetooth == null) {
            return;
        }
        if (bleLruHashMap.containsKey(bleBluetooth.getUniqueSymbol())) {
            bleLruHashMap.remove(bleBluetooth.getUniqueSymbol());
        }
    }

    /**
     * 判断是否包含设备镜像
     */
    public synchronized boolean isContainDevice(BleBluetooth bleBluetooth) {
        if (bleBluetooth == null || !bleLruHashMap.containsKey(bleBluetooth.getUniqueSymbol())) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否包含设备镜像
     */
    public synchronized boolean isContainDevice(ScanResult scanResult) {
        if (scanResult == null || !bleLruHashMap.containsKey(scanResult.getDevice().getAddress() +
                scanResult.getDevice().getName())) {
            return false;
        }
        return true;
    }

    /**
     * 获取连接池中该设备镜像的连接状态，如果没有连接则返回CONNECT_DISCONNECT。
     */
    public synchronized ConnectState getConnectState(ScanResult scanResult) {
        BleBluetooth bleBluetooth = getBleBluetooth(scanResult);
        if (bleBluetooth != null) {
            return bleBluetooth.getConnectState();
        }
        return ConnectState.CONNECT_DISCONNECT;
    }

    /**
     * 获取连接池中的设备镜像，如果没有连接则返回空
     */
    public synchronized BleBluetooth getBleBluetooth(ScanResult scanResult) {
        if (scanResult != null) {
            String key = scanResult.getDevice().getAddress() + scanResult.getDevice().getName();
            if (bleLruHashMap.containsKey(key)) {
                return bleLruHashMap.get(key);
            }
        }
        return null;
    }

    /**
     * 断开连接池中某一个设备
     */
    public synchronized void disconnect(ScanResult scanResult) {
        if (isContainDevice(scanResult)) {
            getBleBluetooth(scanResult).disconnect();
        }
    }

    /**
     * 断开连接池中所有设备
     */
    public synchronized void disconnect() {
        for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleLruHashMap.entrySet()) {
            stringBleBluetoothEntry.getValue().disconnect();
        }
        bleLruHashMap.clear();
    }

    /**
     * 清除连接池
     */
    public synchronized void clear() {
        for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleLruHashMap.entrySet()) {
            stringBleBluetoothEntry.getValue().closeBluetoothGatt();
        }
        bleLruHashMap.clear();
    }

    /**
     * 获取连接池设备镜像Map集合
     *
     * @return
     */
    public Map<String, BleBluetooth> getBleBluetoothMap() {
        return bleLruHashMap;
    }

    /**
     * 获取连接池设备镜像List集合
     *
     * @return
     */
    public synchronized List<BleBluetooth> getBleBluetoothList() {
        final List<BleBluetooth> BleBluetooths = new ArrayList<>(bleLruHashMap.values());
        Collections.sort(BleBluetooths, new Comparator<BleBluetooth>() {
            @Override
            public int compare(final BleBluetooth lhs, final BleBluetooth rhs) {
                return lhs.getUniqueSymbol().compareToIgnoreCase(rhs.getUniqueSymbol());
            }
        });
        return BleBluetooths;
    }

    /**
     * 获取连接池设备详细信息List集合
     *
     * @return
     */
    public synchronized List<ScanResult> getDeviceList() {
        final List<ScanResult> deviceList = new ArrayList<>();
        for (BleBluetooth BleBluetooth : getBleBluetoothList()) {
            if (BleBluetooth != null) {
                deviceList.add(BleBluetooth.getBluetoothLeDevice());
            }
        }
        return deviceList;
    }

}
