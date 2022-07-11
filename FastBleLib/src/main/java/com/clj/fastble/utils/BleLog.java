package com.clj.fastble.utils;


import android.util.Log;

import com.clj.fastble.BuildConfig;

public final class BleLog {

    public static boolean isPrint = true;
    private static final String defaultTag = "FastBle";

    public static void d(String msg) {
        if (isPrint && BuildConfig.DEBUG && msg != null)
            Log.d(defaultTag, msg);
    }

    public static void i(String msg) {
        if (isPrint && BuildConfig.DEBUG && msg != null)
            Log.i(defaultTag, msg);
    }

    public static void w(String msg) {
        if (isPrint && BuildConfig.DEBUG && msg != null)
            Log.w(defaultTag, msg);
    }

    public static void e(String msg) {
        if (isPrint && BuildConfig.DEBUG && msg != null)
            Log.e(defaultTag, msg);
    }

}
