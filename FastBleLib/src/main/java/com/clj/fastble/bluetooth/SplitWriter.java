package com.clj.fastble.bluetooth;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
                if (msg.what == BleMsg.MSG_SPLIT_WRITE) {
                    write();
                }
            }
        };
    }

    public void splitWrite(BleBluetooth bleBluetooth,
                           String uuid_service,
                           String uuid_write,
                           byte[] data,
                           BleWriteCallback callback) {
        mBleBluetooth = bleBluetooth;
        mUuid_service = uuid_service;
        mUuid_write = uuid_write;
        mData = data;
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
        } else {
            byte[] data = mDataQueue.poll();
            mBleBluetooth.newBleConnector()
                    .withUUIDString(mUuid_service, mUuid_write)
                    .writeCharacteristic(data,
                            new BleWriteCallback() {
                                @Override
                                public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                    int position = mTotalNum - mDataQueue.size();
                                    if (mCallback != null) {
                                        mCallback.onWriteSuccess(position, mTotalNum, justWrite);
                                    }

                                    if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
                                        write();
                                    } else {
                                        Message message = mHandler.obtainMessage(BleMsg.MSG_SPLIT_WRITE);
                                        mHandler.sendMessage(message);
                                    }
                                }

                                @Override
                                public void onWriteFailure(BleException exception) {
                                    if (mCallback != null) {
                                        mCallback.onWriteFailure(new OtherException("exception occur while writing: " + exception.getDescription()));
                                    }
                                }
                            },
                            mUuid_write);
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
        if (data != null) {
            int index = 0;
            do {
                byte[] rawData = new byte[data.length - index];
                byte[] newData;
                System.arraycopy(data, index, rawData, 0, data.length - index);
                if (rawData.length <= count) {
                    newData = new byte[rawData.length];
                    System.arraycopy(rawData, 0, newData, 0, rawData.length);
                    index += rawData.length;
                } else {
                    newData = new byte[count];
                    System.arraycopy(data, index, newData, 0, count);
                    index += count;
                }
                byteQueue.offer(newData);
            } while (index < data.length);
        }
        return byteQueue;
    }


}
