package com.maiaga;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by lukas on 6/12/2017.
 */

public class MockConnector implements Runnable {

    MockConnector(Handler handler, Processor processor) {
        mHandler = handler;
        mProcessor = processor;
        try {
            File myFile = new File("data.txt");
            myFile.createNewFile();
            mFileInputStream = new FileInputStream("data.txt");
            mFileOutputStream = new FileOutputStream("data.txt", true);
        }
        catch(FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        mProcessor.setStream(mFileInputStream);
        sendMessage("connectorStatus", "connected");

        new Thread(new Runnable() {
            public void run() {
                String s = "$GPRMC,235316.000,A,4003.9040,N,10512.5792,W,0.09,144.75,141112,,*19\n";
                try {
                    mFileOutputStream.write(s.getBytes());
                    Thread.sleep(500);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //sendMessage("connectorStatus", "cantConnect");
    }

    public void stop() {
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

    private boolean isConnected() {
        return true;
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
    private Processor mProcessor;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
}
