package com.example.lukaskorous.maiaga;

import android.os.Handler;

public class Connector implements Runnable {
    Connector(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void run() {
        mStop = false;
    }

    public void stop() {
        mStop = true;
    }

    private Handler mHandler;
    private boolean mStop;
}
