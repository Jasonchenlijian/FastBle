package com.clj.fastble.utils;


import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.exception.OtherException;

import java.util.LinkedList;
import java.util.Queue;

public class SplitTransferUtil {

    private static final int DEFAULT_WRITE_DATA_SPLIT_COUNT = 20;

    public static void splitWrite(BleBluetooth bleBluetooth,
                                  String uuid_service,
                                  String uuid_write,
                                  byte[] data,
                                  BleWriteCallback callback) {
        splitWrite(bleBluetooth, uuid_service, uuid_write, data, DEFAULT_WRITE_DATA_SPLIT_COUNT, callback);
    }

    public static void splitWrite(BleBluetooth bleBluetooth,
                                  String uuid_service,
                                  String uuid_write,
                                  byte[] data,
                                  int count,
                                  BleWriteCallback callback) {
        if (data == null) {
            throw new IllegalArgumentException("data is Null!");
        }
        if (count < 1) {
            throw new IllegalArgumentException("split count should higher than 0!");
        }
        write(bleBluetooth, uuid_service, uuid_write, callback, splitByte(data, count));
    }

    private static void write(final BleBluetooth bleBluetooth,
                              final String uuid_service,
                              final String uuid_write,
                              final BleWriteCallback callback,
                              final Queue<byte[]> dataInfoQueue) {
        if (dataInfoQueue.peek() == null) {
            if (callback != null) {
                callback.onWriteSuccess();
            }
        } else {
            final byte[] data = dataInfoQueue.poll();
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_write)
                    .writeCharacteristic(data,
                            new BleWriteCallback() {
                                @Override
                                public void onWriteSuccess() {
                                    BleLog.d(HexUtil.formatHexString(data, true) + " been written!");
                                    write(bleBluetooth, uuid_service, uuid_write, callback, dataInfoQueue);
                                }

                                @Override
                                public void onWriteFailure(BleException exception) {
                                    if (callback != null) {
                                        callback.onWriteFailure(new OtherException("exception occur while writing: " + exception.getDescription()));
                                    }
                                }
                            },
                            uuid_write);
        }
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
