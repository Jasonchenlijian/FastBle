package com.clj.fastble.exception;

import java.io.Serializable;


public abstract class BleException implements Serializable {
    private static final long serialVersionUID = 8004414918500865564L;

    public static final int ERROR_CODE_TIMEOUT = 1;
    public static final int ERROR_CODE_INITIAL = 2;
    public static final int ERROR_CODE_GATT = 3;
    public static final int ERROR_CODE_OTHER = 4;
    public static final int ERROR_CODE_NOT_FOUND_DEVICE = 5;
    public static final int ERROR_CODE_BLUETOOTH_NOT_ENABLE = 6;
    public static final int ERROR_CODE_SCAN_FAILED = 7;


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
