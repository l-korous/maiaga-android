package com.maiaga;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.PipedInputStream;
import java.io.FileNotFoundException;
import java.io.PipedOutputStream;
import java.io.IOException;

/**
 * Created by lukas on 6/12/2017.
 */

public class MockConnector implements Runnable {

    MockConnector(Handler handler, Processor processor, Context context) {
        mHandler = handler;
        mStop = true;
        mProcessor = processor;
        try {
            String filePath = context.getFilesDir().getPath() + "/data.txt";
            File myFile = new File(filePath);
            myFile.delete();
            myFile.createNewFile();
            mPipedOutputStream = new PipedOutputStream();
            mPipedInputStream = new PipedInputStream(mPipedOutputStream);
        }
        catch(FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        mStop = false;
        mProcessor.setStream(mPipedInputStream);
        sendMessage("connectorState", ConnectorConnectionState.Connected.toString());

        new Thread(new Runnable() {
            public void run() {
                int timeInt = 235316;
                int i = 0;
                while (!Thread.currentThread().isInterrupted() && !mStop) {
                    int r = (int) Math.floor(Math.random() * 10);
                    int r1 = (int) Math.floor(Math.random() * 10);
                    String s = "$GPRMC," + Integer.toString(timeInt) + ".000,A,4003.0" + Integer.toString(r) + "40,N,10512.7" + Integer.toString(r) + "92,W," + (i++ % 30 > 15 ? "5" : "0") + "." + Integer.toString(r1) + "0,144.75,141112,,*19\n" +
                            "$GPGGA," + Integer.toString(timeInt) + ".000,4003.0" + Integer.toString(r1) + "40,N,10512.7" + Integer.toString(r1) + "92,W,1,08,1.6,157" + Integer.toString(r) + ".9,M,-20.7,M,,0000*5F\n";
                    timeInt = ((timeInt / 100) * 100) + ((timeInt - ((timeInt / 100) * 100) + 1) % 60);

                    Log.d("GPS DATA", s);
                    try {
                        mPipedOutputStream.write(s.getBytes());
                        Thread.sleep(750);
                    } catch (IOException e) {
                        mStop = true;
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        mStop = true;
                        e.printStackTrace();
                    }
                }

                sendMessage("connectorState", "CantConnect");
            }
        }).start();
    }

    public void stop() {
        mStop = true;
        Thread.currentThread().interrupt();
    }

    public void reconnect() {
        stop();
        run();
    }

    public String setDeviceReturnName(String deviceAddress) {
        return deviceAddress;
    }

    public boolean isBluetoothAvailable() {
        return true;
    }

    public boolean isBluetoothEnabled() {
        return true;
    }

    public Intent createBluetoothEnableIntent() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }

    public boolean isConnected() {
        return !mStop;
    }

    private boolean shouldStopConnecting() {
        return false;
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
    private Processor mProcessor;
    private PipedInputStream mPipedInputStream;
    private PipedOutputStream mPipedOutputStream;
}
