package com.clj.fastble.utils;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.util.Log;
import com.clj.fastble.log.BleLog;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;


public class BluetoothUtil {

    private static final String TAG = "BluetoothUtil";

    /**
     * 启用蓝牙
     */
    public static void enableBluetooth(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 打印蓝牙设备所有属性
     */
    public static void printServices(BluetoothGatt gatt) {
        if (gatt != null) {
            for (BluetoothGattService service : gatt.getServices()) {
                BleLog.i(TAG, "service: " + service.getUuid());

                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    BleLog.d(TAG, "    characteristic: " + characteristic.getUuid()
                            + "   ------  value: " + Arrays.toString(characteristic.getValue())
                            + "   ------  properties: " + characteristic.getProperties());

                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        BleLog.v(TAG, "    descriptor: " + descriptor.getUuid()
                                + "   ------  value: " + Arrays.toString(descriptor.getValue()));
                    }
                }
            }
        }
    }

    /*------------  BluetoothGatt  ------------ */
    /**
     * Clears the device cache. After uploading new hello4 the DFU target will have other services than before.
     */
    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        /*
         * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
		 */
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                final boolean success = (Boolean) refresh.invoke(gatt);
                Log.i(TAG, "Refreshing result: " + success);
                return success;
            }
        } catch (Exception e) {
            Log.e(TAG, "An exception occured while refreshing device", e);
        }
        return false;
    }

    public static void closeBluetoothGatt(BluetoothGatt gatt) {
        if (gatt != null) {
            gatt.disconnect();
            refreshDeviceCache(gatt);
            gatt.close();
        }
    }

    /*------------  Service  ------------ */
    public static BluetoothGattService getService(BluetoothGatt gatt, String serviceUUID) {
        return gatt.getService(UUID.fromString(serviceUUID));
    }

    /*------------  Characteristic服务  ------------ */
    public static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String charactUUID) {
        if (service != null) {
            return service.getCharacteristic(UUID.fromString(charactUUID));
        }
        return null;
    }

    public static BluetoothGattCharacteristic getCharacteristic(BluetoothGatt gatt, String serviceUUID, String charactUUID) {
        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        if (service != null) {
            return service.getCharacteristic(UUID.fromString(charactUUID));
        }
        return null;
    }

}
