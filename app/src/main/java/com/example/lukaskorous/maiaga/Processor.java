package com.example.lukaskorous.maiaga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

public class Processor implements Runnable {
    Processor(Handler handler) {
        mHandler = handler;
    }

    static {
        System.loadLibrary("tinyGps");
    }
    public native void encode(short s);

    public native double print();

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
                        Message msg = mHandler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putString("data", Double.toString(print()));
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }

                }
            }
            catch (IOException ex)
            {
                mStop = true;
                Message msg = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("status", "disconnected");
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        }
    }

    public void stop() {
        mStop = true;
    }

    public void setStream(InputStream inStream) {
        mInStream = inStream;
    }

    private Handler mHandler;
    private boolean mStop;
    private InputStream mInStream;
}
