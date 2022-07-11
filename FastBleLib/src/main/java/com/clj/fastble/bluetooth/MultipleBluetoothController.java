package com.clj.fastble.bluetooth;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Build;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.utils.BleLruHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleBluetoothController {

    private final BleLruHashMap<String, BleBluetooth> bleLruHashMap;
    private final HashMap<String, BleBluetooth> bleTempHashMap;

    public MultipleBluetoothController() {
        bleLruHashMap = new BleLruHashMap<>(BleManager.getInstance().getMaxConnectCount());
        bleTempHashMap = new HashMap<>();
    }

    public BleBluetooth buildConnectingBle(BleDevice bleDevice) {
        synchronized (this) {
            if (!bleTempHashMap.containsKey(bleDevice.getKey())) {
                BleBluetooth bleBluetooth = new BleBluetooth(bleDevice);
                bleTempHashMap.put(bleDevice.getKey(), bleBluetooth);
                return bleBluetooth;
            }
            return bleTempHashMap.get(bleDevice.getKey());
        }
    }

    public void removeConnectingBle(BleBluetooth bleBluetooth) {
        synchronized (this) {
            if (bleBluetooth == null) {
                return;
            }
            bleTempHashMap.remove(bleBluetooth.getDeviceKey());
        }
    }

    public void addBleBluetooth(BleBluetooth bleBluetooth) {
        synchronized (this) {
            if (bleBluetooth == null) {
                return;
            }
            if (bleLruHashMap.containsKey(bleBluetooth.getDeviceKey())) {
                return;
            }
            bleLruHashMap.put(bleBluetooth.getDeviceKey(), bleBluetooth);
        }
    }

    public void removeBleBluetooth(BleBluetooth bleBluetooth) {
        synchronized (this) {
            if (bleBluetooth == null) {
                return;
            }
            bleLruHashMap.remove(bleBluetooth.getDeviceKey());
        }
    }

    public boolean isContainDevice(BleDevice bleDevice) {
        return bleDevice != null && bleLruHashMap.containsKey(bleDevice.getKey());
    }

    @SuppressLint("MissingPermission")
    public boolean isContainDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && bleLruHashMap.containsKey(bluetoothDevice.getName() + bluetoothDevice.getAddress());
    }

    public BleBluetooth getBleBluetooth(BleDevice bleDevice) {
        synchronized (this) {
            if (bleDevice != null) {
                return bleLruHashMap.get(bleDevice.getKey());
            }
            return null;
        }
    }

    public void disconnect(BleDevice bleDevice) {
        getBleBluetooth(bleDevice).disconnect();
    }

    public void disconnectAllDevice() {
        synchronized (this) {
            for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleLruHashMap.entrySet()) {
                stringBleBluetoothEntry.getValue().disconnect();
            }
            bleLruHashMap.clear();
        }
    }

    public void destroy(String mac) {
        synchronized (this) {
            for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleLruHashMap.entrySet()) {
                if (mac != null) {
                    if (stringBleBluetoothEntry.getValue().getDevice().getMac().equals(mac)) {
                        stringBleBluetoothEntry.getValue().destroy();
                        bleLruHashMap.remove(stringBleBluetoothEntry.getKey());
                    }
                } else {
                    stringBleBluetoothEntry.getValue().destroy();
                    bleLruHashMap.remove(stringBleBluetoothEntry.getKey());
                }
            }
            for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleTempHashMap.entrySet()) {
                if (mac != null) {
                    if (stringBleBluetoothEntry.getValue().getDevice().getMac().equals(mac)) {
                        stringBleBluetoothEntry.getValue().destroy();
                        bleTempHashMap.remove(stringBleBluetoothEntry.getKey());
                    }
                } else {
                    stringBleBluetoothEntry.getValue().destroy();
                    bleTempHashMap.remove(stringBleBluetoothEntry.getKey());
                }
            }
        }
    }

    public List<BleBluetooth> getBleBluetoothList() {
        synchronized (this) {
            List<BleBluetooth> bleBluetoothList = new ArrayList<>(bleLruHashMap.values());
            Collections.sort(bleBluetoothList, (lhs, rhs) -> lhs.getDeviceKey().compareToIgnoreCase(rhs.getDeviceKey()));
            return bleBluetoothList;
        }
    }

    public List<BleDevice> getDeviceList() {
        synchronized (this) {
            refreshConnectedDevice();
            List<BleDevice> deviceList = new ArrayList<>();
            for (BleBluetooth BleBluetooth : getBleBluetoothList()) {
                if (BleBluetooth != null) {
                    deviceList.add(BleBluetooth.getDevice());
                }
            }
            return deviceList;
        }
    }

    public void refreshConnectedDevice() {
        List<BleBluetooth> bluetoothList = getBleBluetoothList();
        for (int i = 0; bluetoothList != null && i < bluetoothList.size(); i++) {
            BleBluetooth bleBluetooth = bluetoothList.get(i);
            if (!BleManager.getInstance().isConnected(bleBluetooth.getDevice())) {
                removeBleBluetooth(bleBluetooth);
            }
        }
    }


}
