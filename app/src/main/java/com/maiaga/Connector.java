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
    private static final int mMaxConnectionAttempts = 3;

    Connector(Handler handler, Processor processor, Context context) {
        mHandler = handler;
        mStop = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectorConnectionState = ConnectorConnectionState.ReadyToConnect;
        mProcessor = processor;
    }

    @Override
    public void run() {
        if(mRunningThread != null)
            mRunningThread.interrupt();
        mStop = false;
        mRunningThread = Thread.currentThread();
        mConnectionAttempts = 0;
        mConnectorConnectionState = ConnectorConnectionState.Connecting;
        sendMessage("connectorState", mConnectorConnectionState.toString());
        mBluetoothAdapter.cancelDiscovery();
        closeSocket();

        while(!mStop && !shouldStopConnecting() && !isConnected()) {
            mConnectionAttempts++;
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
                mBluetoothSocket.connect();
                mProcessor.setStream(mBluetoothSocket.getInputStream());
                mConnectorConnectionState = ConnectorConnectionState.Connected;
                sendMessage("connectorState", mConnectorConnectionState.toString());
            } catch (IOException eConnectException) {
                closeSocket();
            }
        }

        if(!isConnected()) {
            mConnectorConnectionState = ConnectorConnectionState.CantConnect;
            sendMessage("connectorState", mConnectorConnectionState.toString());
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if(mBluetoothSocket != null)
                mBluetoothSocket.close();
            mProcessor.setStream(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        mStop = true;
        if(mRunningThread != null)
            mRunningThread.interrupt();
        closeSocket();
        mConnectorConnectionState = ConnectorConnectionState.ReadyToConnect;
        sendMessage("connectorState", mConnectorConnectionState.toString());
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
        if(mConnectionAttempts > mMaxConnectionAttempts)
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
    private ConnectorConnectionState mConnectorConnectionState;
    private Processor mProcessor;
    private Thread mRunningThread;
}
