package com.example.lukaskorous.maiaga;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.lukas.myapplication.MESSAGE";
    private UUID applicationUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private TextView textView;

    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mBluetoothConnectProgressDialog;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private InputStream mmInStream;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    final int handlerState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView2);
        textView.setText("");
        textView.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    public void buttonClick(View view) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Intent intent = new Intent(this, DisplayMessageActivity.class);
            intent.putExtra(EXTRA_MESSAGE, "no BT");
            startActivity(intent);
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else {
                Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
            }
        }
    }

    public void onActivityResult (int requestCode,
                                  int resultCode,
                                  Intent data) {
        switch (requestCode)
        {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == RESULT_OK)
                {
                    Bundle mExtra = data.getExtras();
                    String mDeviceAddress = mExtra.getString("DeviceAddress");
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mBluetoothConnectProgressDialog = ProgressDialog.show(this, "Connecting...", mBluetoothDevice.getName() + " : " + mBluetoothDevice.getAddress(), true, false);
                    try
                    {
                        mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
                        mBluetoothAdapter.cancelDiscovery();
                        mBluetoothSocket.connect();
                        mHandler.sendEmptyMessage(0);
                        mmInStream = mBluetoothSocket.getInputStream();

                        beginListenForData();
                    }
                    catch (IOException eConnectException)
                    {
                        closeSocket(mBluetoothSocket);
                        Intent intent = new Intent(this, DisplayMessageActivity.class);
                        intent.putExtra(EXTRA_MESSAGE, "CouldNotConnectToSocket");
                        startActivity(intent);
                    }
                }
                break;

            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        // There are paired devices. Get the name and address of each paired device.
                        for (BluetoothDevice device : pairedDevices) {
                            String deviceName = device.getName();
                            String deviceHardwareAddress = device.getAddress(); // MAC address
                        }
                    }
                }
                else {
                    Intent intent = new Intent(this, DisplayMessageActivity.class);
                    intent.putExtra(EXTRA_MESSAGE, "denied BT");
                    startActivity(intent);
                }
                break;
        }
    }

    private void closeSocket(BluetoothSocket nOpenSocket)
    {
        try
        {
            nOpenSocket.close();
        }
        catch (IOException ex)
        {
            Intent intent = new Intent(this, DisplayMessageActivity.class);
            intent.putExtra(EXTRA_MESSAGE, "CouldNotCloseSocket");
            startActivity(intent);
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            textView.append(data);
                                            textView.append(" \r\n \n \r " + System.getProperty("line.separator"));
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private Handler mHandler = new Handler()
    {
        byte[] buffer = new byte[256];
        int bytes;

        @Override
        public void handleMessage(Message msg)
        {
            mBluetoothConnectProgressDialog.dismiss();
            Toast.makeText(MainActivity.this, "Device Connected", Toast.LENGTH_LONG).show();
        }
    };
}
