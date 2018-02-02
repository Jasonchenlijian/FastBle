package com.clj.fastble.data;


public enum BleConnectState {

    CONNECT_IDLE(0x00),
    CONNECT_CONNECTING(0x01),
    CONNECT_CONNECTED(0x02),
    CONNECT_FAILURE(0x03),
    CONNECT_DISCONNECT(0x05);

    private int code;

    BleConnectState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
