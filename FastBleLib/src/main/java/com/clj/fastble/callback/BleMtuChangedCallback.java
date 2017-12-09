package com.clj.fastble.callback;


import com.clj.fastble.bluetooth.BleConnector;
import com.clj.fastble.exception.BleException;

public abstract class BleMtuChangedCallback {

    public abstract void onsetMTUFailure(BleException exception);

    public abstract void onMtuChanged(int mtu);

    private BleConnector bleConnector;

    public void setBleConnector(BleConnector bleConnector) {
        this.bleConnector = bleConnector;
    }

    public BleConnector getBleConnector() {
        return bleConnector;
    }

}
