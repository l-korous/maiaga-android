package com.maiaga;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

public class Connector implements Runnable {
    public static final UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int mmaxConnectionAttempts = 3;

    Connector(Handler handler, Processor processor, Context context) {
        mHandler = handler;
        mStop = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mProcessor = processor;
    }

    @Override
    public void run() {
        mStop = false;
        mConnectionAttempts = 0;
        sendMessage("connectorState", ConnectorConnectionState.Connecting.toString());
        mBluetoothAdapter.cancelDiscovery();
        try {
            if(mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(!shouldStopConnecting() && !isConnected()) {
            mConnectionAttempts++;
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
                mBluetoothSocket.connect();
                mProcessor.setStream(mBluetoothSocket.getInputStream());
                sendMessage("connectorState", ConnectorConnectionState.Connected.toString());
            } catch (IOException eConnectException) {
                try {
                    mBluetoothSocket.close();
                    mProcessor.setStream(null);
                }
                catch (IOException ex) {
                }
            }
        }
        if(!isConnected()) {
            sendMessage("connectorState", ConnectorConnectionState.CantConnect.toString());
            try {
                mBluetoothSocket.close();
                mProcessor.setStream(null);
            } catch (IOException ex) {
            }
        }
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

    public boolean isConnected() {
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
    private boolean mStop;

    private int mConnectionAttempts;
    private Processor mProcessor;
}
