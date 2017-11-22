package com.clj.fastble.bluetooth;


import java.util.LinkedHashMap;

public class BleLruHashMap<K, V> extends LinkedHashMap<K, V> {

    private final int MAX_SAVE_SIZE;

    public BleLruHashMap(int saveSize) {
        super((int) Math.ceil(saveSize / 0.75) + 1, 0.75f, true);
        MAX_SAVE_SIZE = saveSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry eldest) {
        if (size() > MAX_SAVE_SIZE && eldest.getValue() instanceof BleBluetooth) {
            ((BleBluetooth) eldest.getValue()).disconnect();
        }
        return size() > MAX_SAVE_SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

}
