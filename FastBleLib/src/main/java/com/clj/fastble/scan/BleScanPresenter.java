package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.clj.fastble.callback.BleScanListener;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.utils.BleLog;
import com.clj.fastble.utils.HexUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleScanPresenter implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames;
    private String mDeviceMac;
    private boolean mFuzzy;
    private boolean mNeedConnect;
    private long mScanTimeout;
    private BleScanListener mBleScanListener;

    private final List<BleDevice> mBleDeviceList = new ArrayList<>();

    final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private void handleResult(final BleDevice bleDevice) {
        onLeScan(bleDevice);
        checkDevice(bleDevice);
    }

    public void prepare(String[] names, String mac, boolean fuzzy, boolean needConnect,
                        long timeOut, BleScanListener bleScanListener) {
        mDeviceNames = names;
        mDeviceMac = mac;
        mFuzzy = fuzzy;
        mNeedConnect = needConnect;
        mScanTimeout = timeOut;
        mBleScanListener = bleScanListener;
    }

    public boolean ismNeedConnect() {
        return mNeedConnect;
    }

    public BleScanListener getBleScanListener() {
        return mBleScanListener;
    }

    public void setCancelBleScanListener() {
        this.mBleScanListener = null;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;
        handleResult(new BleDevice(device, rssi, scanRecord, System.currentTimeMillis()));
    }

    private void checkDevice(BleDevice bleDevice) {
        if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
            correctDeviceAndNextStep(bleDevice);
            return;
        }

        if (!TextUtils.isEmpty(mDeviceMac)) {
            if (!mDeviceMac.equalsIgnoreCase(bleDevice.getMac()))
                return;
        }

        if (mDeviceNames != null && mDeviceNames.length > 0) {
            AtomicBoolean equal = new AtomicBoolean(false);
            for (String name : mDeviceNames) {
                String remoteName = bleDevice.getName();
                if (remoteName == null)
                    remoteName = "";
                if (mFuzzy ? remoteName.contains(name) : remoteName.equals(name)) {
                    equal.set(true);
                }
            }
            if (!equal.get()) {
                return;
            }
        }

        correctDeviceAndNextStep(bleDevice);
    }


    private void correctDeviceAndNextStep(final BleDevice bleDevice) {
        if (mNeedConnect) {
            BleLog.i("devices detected  ------"
                    + "  name:" + bleDevice.getName()
                    + "  mac:" + bleDevice.getMac()
                    + "  Rssi:" + bleDevice.getRssi()
                    + "  scanRecord:" + HexUtil.formatHexString(bleDevice.getScanRecord()));

            mBleDeviceList.add(bleDevice);
            BleScanner.getInstance().stopLeScan(true);
        } else {
            AtomicBoolean hasFound = new AtomicBoolean(false);
            for (BleDevice result : mBleDeviceList) {
                if (result.getDevice().equals(bleDevice.getDevice())) {
                    hasFound.set(true);
                }
            }
            if (!hasFound.get()) {
                BleLog.i("device detected  ------"
                        + "  name: " + bleDevice.getName()
                        + "  mac: " + bleDevice.getMac()
                        + "  Rssi: " + bleDevice.getRssi()
                        + "  scanRecord: " + HexUtil.formatHexString(bleDevice.getScanRecord(), true));

                mBleDeviceList.add(bleDevice);
                onScanning(bleDevice);
            }
        }
    }

    public final void notifyScanStarted(final boolean success) {
        mBleDeviceList.clear();

        removeHandlerMsg();

        if (success && mScanTimeout > 0) {
            mMainHandler.postDelayed(() -> BleScanner.getInstance().stopLeScan(true), mScanTimeout);
        }

        onScanStarted(success);
    }

    public final void notifyScanStopped() {
        removeHandlerMsg();
        onScanFinished(mBleDeviceList);
    }

    public final void removeHandlerMsg() {
        mMainHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onLeScan(BleDevice bleDevice);

    public abstract void onScanning(BleDevice bleDevice);

    public abstract void onScanFinished(List<BleDevice> bleDeviceList);
}
