package com.clj.fastble.exception;

import java.io.Serializable;


public abstract class BleException implements Serializable {
    private static final long serialVersionUID = 8004414918500865564L;

    public static final int ERROR_CODE_TIMEOUT = 100;
    public static final int ERROR_CODE_GATT = 101;
    public static final int ERROR_CODE_OTHER = 102;
    public static final int ERROR_CODE_NOT_FOUND_DEVICE = 103;
    public static final int ERROR_CODE_BLUETOOTH_NOT_ENABLE = 104;
    public static final int ERROR_CODE_SCAN_FAILED = 105;


    private int code;
    private String description;

    public BleException(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public BleException setCode(int code) {
        this.code = code;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public BleException setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return "BleException { " +
               "code=" + code +
               ", description='" + description + '\'' +
               '}';
    }
}
