package com.example.lukaskorous.maiaga;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String errorMessageExtra = "com.example.lukas.myapplication.message";
    public static final UUID applicationUUID = UUID.fromString("7b258380-4c5d-11e7-b114-b2f933d5fe66");

    private static final int requestDeviceConnect = 1;
    private static final int requestBluetooth = 2;

    private TextView mStatusTextView;
    private TextView mErrorTextView;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private InputStream mInStream;

    private Processor mProcessor;
    private Connector mConnector;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            Bundle bundle = msg.getData();
            String key = bundle.getString("key", "_");
            switch(key) {
                case "processorStatus":
                case "connectorStatus":
                default:
                    Toast.makeText(MainActivity.this, getResources().getText(R.string.device_connected).toString(), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        mProcessor = new Processor(mHandler);
        mConnector = new Connector(mHandler);

        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        mErrorTextView = (TextView) findViewById(R.id.errorTextView);
    }

    public void buttonClick(View view) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mErrorTextView.setText(getResources().getText(R.string.no_bt).toString());
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, requestBluetooth);
            } else {
                Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(connectIntent, requestDeviceConnect);
            }
        }
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case requestDeviceConnect:
                if (resultCode == RESULT_OK)
                {
                    Bundle mExtra = data.getExtras();
                    String mDeviceAddress = mExtra.getString("DeviceAddress");
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    try
                    {
                        mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID);
                        mBluetoothSocket.connect();
                        mHandler.sendEmptyMessage(0);
                        mProcessor.setStream(mBluetoothSocket.getInputStream());

                        beginListenForData();
                    }
                    catch (IOException eConnectException)
                    {
                        closeSocket(mBluetoothSocket);
                        mErrorTextView.setText(getResources().getText(R.string.cant_connect).toString());
                    }
                }
                break;

            case requestBluetooth:
                if(resultCode == RESULT_OK) {
                    mBluetoothAdapter.cancelDiscovery();
                    Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(connectIntent, requestDeviceConnect);
                }
                else {
                    mErrorTextView.setText(getResources().getText(R.string.denied_bt).toString());
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
            mErrorTextView.setText(getResources().getText(R.string.could_not_close_socket).toString());
        }
    }


    void beginListenForData()
    {
        mStatusTextView.setText("");

        final Handler handler = new Handler();

        Thread workerThread = new Thread(mProcessor);

        workerThread.start();
    }
}
