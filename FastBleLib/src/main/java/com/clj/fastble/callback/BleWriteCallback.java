package com.clj.fastble.callback;


import com.clj.fastble.exception.BleException;

public abstract class BleWriteCallback extends BleBaseCallback{

    public abstract void onWriteSuccess();

    public abstract void onWriteFailure(BleException exception);


}
