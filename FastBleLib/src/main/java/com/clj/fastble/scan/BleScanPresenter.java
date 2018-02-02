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

import com.clj.fastble.BleManager;
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

    private static final String TAG = "BleScanPresenter";
    private long mScanTimeout = BleManager.DEFAULT_SCAN_TIME;
    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private boolean mNeedConnect = false;
    private List<BleDevice> mBleDeviceList = new ArrayList<>();

    private Handler mMainHandler;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private static final class ScanHandler extends Handler {

        private final WeakReference<BleScanPresenter> mBleScanPresenter;

        ScanHandler(Looper looper, BleScanPresenter bleScanPresenter) {
            super(looper);
            mBleScanPresenter = new WeakReference<>(bleScanPresenter);
        }

        @Override
        public void handleMessage(Message msg) {
            BleScanPresenter bleScanPresenter = mBleScanPresenter.get();
            if (bleScanPresenter != null) {
                if (msg.what == BleMsg.MSG_SCAN_DEVICE) {
                    final BleDevice bleDevice = (BleDevice) msg.obj;
                    if (bleDevice != null) {
                        bleScanPresenter.handleResult(bleDevice);
                    }
                }
            }
        }
    }

    private void handleResult(final BleDevice bleDevice) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onLeScan(bleDevice);
            }
        });
        checkDevice(bleDevice);
    }

    public BleScanPresenter(String[] names, String mac, boolean fuzzy, boolean needConnect, long timeOut) {
        mDeviceNames = names;
        mDeviceMac = mac;
        mFuzzy = fuzzy;
        mNeedConnect = needConnect;
        mScanTimeout = timeOut;

        mMainHandler = new Handler(Looper.getMainLooper());

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();

        mHandler = new ScanHandler(mHandlerThread.getLooper(), this);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        BleDevice bleDevice = new BleDevice(device, rssi, scanRecord, System.currentTimeMillis());

        Message message = mHandler.obtainMessage();
        message.what = BleMsg.MSG_SCAN_DEVICE;
        message.obj = bleDevice;
        mHandler.sendMessage(message);
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
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    BleManager.getInstance().getBleScanner().stopLeScan();
                }
            });

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
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onScanning(bleDevice);
                    }
                });
            }
        }
    }

    public final void notifyScanStarted(final boolean success) {
        mBleDeviceList.clear();

        removeHandlerMsg();

        if (success && mScanTimeout > 0) {
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BleManager.getInstance().getBleScanner().stopLeScan();
                }
            }, mScanTimeout);
        }

        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onScanStarted(success);
            }
        });
    }

    public final void notifyScanStopped() {
        removeHandlerMsg();
        mHandlerThread.quit();
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onScanFinished(mBleDeviceList);
            }
        });
    }

    public final void removeHandlerMsg() {
        mMainHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onLeScan(BleDevice bleDevice);

    public abstract void onScanning(BleDevice bleDevice);

    public abstract void onScanFinished(List<BleDevice> bleDeviceList);
}
