package com.maiaga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Processor implements Runnable {
    Processor(Handler handler) {
        mHandler = handler;
    }

    private ArrayList<LogItem> log;

    static {
        System.loadLibrary("tinyGps");
    }
    public native void encode(short s);

    public native double lat();
    public native double lng();
    public native double alt();
    public native double speed();
    public native long date();
    public native long time();
    public native boolean newDataAvailable();

    @Override
    public void run() {
        mStop = false;

        while(!Thread.currentThread().isInterrupted() && !mStop)
        {
            try
            {
                final int bytesAvailable = mInStream.available();
                if(bytesAvailable > 0)
                {
                    final byte[] packetBytes = new byte[bytesAvailable];
                    mInStream.read(packetBytes);
                    for(int i=0;i<bytesAvailable;i++)
                    {
                        byte b = packetBytes[i];
                        encode(b);
                    }

                }
                if(newDataAvailable()) {
                    LogItem logItem = new LogItem();
                    logItem.lat = lat();
                    logItem.lng = lng();
                    logItem.alt = alt();
                    logItem.speed = speed();
                    logItem.date = date();
                    logItem.time = time();
                    sendMessage("processorData", logItem.toString());
                }

            }
            catch (IOException ex)
            {
                mStop = true;
                sendMessage("processorStatus", "ioException");
            }
        }
    }

    public void stop() {
        mStop = true;
    }

    public void reset() {
        stop();
        log.clear();
    }

    public void setStream(InputStream inStream) {
        mInStream = inStream;
    }

    private void sendMessage(String key, String data) {
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("key", key);
        bundle.putString("data", data);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private Handler mHandler;
    private boolean mStop;
    private InputStream mInStream;
}
