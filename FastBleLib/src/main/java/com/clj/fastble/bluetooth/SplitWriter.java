package com.clj.fastble.bluetooth;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.utils.BleLog;

import java.util.LinkedList;
import java.util.Queue;

public class SplitWriter {

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private BleBluetooth mBleBluetooth;
    private String mUuid_service;
    private String mUuid_write;
    private byte[] mData;
    private int mCount;
    private boolean mSendNextWhenLastSuccess;
    private long mIntervalBetweenTwoPackage;
    private BleWriteCallback mCallback;
    private Queue<byte[]> mDataQueue;
    private int mTotalNum;

    public SplitWriter() {
        mHandlerThread = new HandlerThread("splitWriter");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == BleMsg.MSG_SPLIT_WRITE_NEXT) {
                    write();
                }
            }
        };
    }

    public void splitWrite(BleBluetooth bleBluetooth,
                           String uuid_service,
                           String uuid_write,
                           byte[] data,
                           boolean sendNextWhenLastSuccess,
                           long intervalBetweenTwoPackage,
                           BleWriteCallback callback) {
        mBleBluetooth = bleBluetooth;
        mUuid_service = uuid_service;
        mUuid_write = uuid_write;
        mData = data;
        mSendNextWhenLastSuccess = sendNextWhenLastSuccess;
        mIntervalBetweenTwoPackage = intervalBetweenTwoPackage;
        mCount = BleManager.getInstance().getSplitWriteNum();
        mCallback = callback;

        splitWrite();
    }

    private void splitWrite() {
        if (mData == null) {
            throw new IllegalArgumentException("data is Null!");
        }
        if (mCount < 1) {
            throw new IllegalArgumentException("split count should higher than 0!");
        }
        mDataQueue = splitByte(mData, mCount);
        mTotalNum = mDataQueue.size();
        write();
    }

    private void write() {
        if (mDataQueue.peek() == null) {
            release();
            return;
        }

        byte[] data = mDataQueue.poll();
        mBleBluetooth.newBleConnector()
                .withUUIDString(mUuid_service, mUuid_write)
                .writeCharacteristic(
                        data,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                int position = mTotalNum - mDataQueue.size();
                                if (mCallback != null) {
                                    mCallback.onWriteSuccess(position, mTotalNum, justWrite);
                                }
                                if (mSendNextWhenLastSuccess) {
                                    Message message = mHandler.obtainMessage(BleMsg.MSG_SPLIT_WRITE_NEXT);
                                    mHandler.sendMessageDelayed(message, mIntervalBetweenTwoPackage);
                                }
                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                if (mCallback != null) {
                                    mCallback.onWriteFailure(new OtherException("exception occur while writing: " + exception.getDescription()));
                                }
                                if (mSendNextWhenLastSuccess) {
                                    Message message = mHandler.obtainMessage(BleMsg.MSG_SPLIT_WRITE_NEXT);
                                    mHandler.sendMessageDelayed(message, mIntervalBetweenTwoPackage);
                                }
                            }
                        },
                        mUuid_write);

        if (!mSendNextWhenLastSuccess) {
            Message message = mHandler.obtainMessage(BleMsg.MSG_SPLIT_WRITE_NEXT);
            mHandler.sendMessageDelayed(message, mIntervalBetweenTwoPackage);
        }
    }

    private void release() {
        mHandlerThread.quit();
        mHandler.removeCallbacksAndMessages(null);
    }

    private static Queue<byte[]> splitByte(byte[] data, int count) {
        if (count > 20) {
            BleLog.w("Be careful: split count beyond 20! Ensure MTU higher than 23!");
        }
        Queue<byte[]> byteQueue = new LinkedList<>();
        int pkgCount;
        if (data.length % count == 0) {
            pkgCount = data.length / count;
        } else {
            pkgCount = Math.round(data.length / count + 1);
        }

        if (pkgCount > 0) {
            for (int i = 0; i < pkgCount; i++) {
                byte[] dataPkg;
                int j;
                if (pkgCount == 1 || i == pkgCount - 1) {
                    j = data.length % count == 0 ? count : data.length % count;
                    System.arraycopy(data, i * count, dataPkg = new byte[j], 0, j);
                } else {
                    System.arraycopy(data, i * count, dataPkg = new byte[count], 0, count);
                }
                byteQueue.offer(dataPkg);
            }
        }

        return byteQueue;
    }


}
