
package com.clj.fastble.conn;

import com.clj.fastble.exception.BleException;

public abstract class BleCallback {

    public abstract void onFailure(BleException exception);

    public abstract void onInitiatedResult(boolean result);
}