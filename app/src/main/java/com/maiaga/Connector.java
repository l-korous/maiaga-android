package com.maiaga;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

public class Connector implements Runnable {
    public static final UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int mmaxConnectionAttempts = 10;

    Connector(Handler handler, Processor processor) {
        mHandler = handler;
        mStop = false;
        mConnected = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mProcessor = processor;
    }

    @Override
    public void run() {
        mStop = false;
        mBluetoothAdapter.cancelDiscovery();
        while(!shouldStopConnecting() && !isConnected()) {
            mConnectionAttempts++;
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
                mBluetoothSocket.connect();
                mProcessor.setStream(mBluetoothSocket.getInputStream());
                sendMessage("connectorStatus", "connected");
            } catch (IOException eConnectException) {
                try {
                    mBluetoothSocket.close();
                }
                catch (IOException ex) {
                }
            }
        }
        if(!isConnected())
            sendMessage("connectorStatus", "cantConnect");
    }

    public void stop() {
        mStop = true;
    }

    public String setDeviceReturnName(String deviceAddress) {
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        return mBluetoothDevice.getName();
    }

    public boolean isBluetoothAvailable() {
        return mBluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public Intent createBluetoothEnableIntent() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }

    private boolean isConnected() {
        if(mBluetoothSocket != null)
            return mBluetoothSocket.isConnected();
        else
            return false;
    }

    private boolean shouldStopConnecting() {
        if(mStop)
            return true;
        if(mConnectionAttempts > mmaxConnectionAttempts)
            return true;
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

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;

    private Handler mHandler;
    private boolean mStop, mConnected;

    private int mConnectionAttempts;
    private Processor mProcessor;
}
