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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String errorMessageExtra = "com.example.lukas.myapplication.message";

    private static final int requestDeviceConnect = 1;
    private static final int requestBluetooth = 2;

    private TextView mStatusTextView;
    private TextView mErrorTextView;
    private ProgressDialog mBluetoothConnectProgressDialog;

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
            String data = bundle.getString("data", "_");

            switch(key) {
                case "processorData":
                    mStatusTextView.setText(data);
                    break;
                case "processorStatus":
                case "connectorStatus":
                    switch(data) {
                        case "connected":
                            mStatusTextView.setText("");
                            Toast.makeText(MainActivity.this, getResources().getText(R.string.device_connected).toString(), Toast.LENGTH_SHORT).show();
                            mBluetoothConnectProgressDialog.dismiss();
                            new Thread(mProcessor).start();
                            break;
                        case "cantConnect":
                            mProcessor.stop();
                            mErrorTextView.setText(getResources().getText(R.string.disconnected).toString());
                            break;
                    }
                    break;
                default:
                    mBluetoothConnectProgressDialog.dismiss();
                    Toast.makeText(MainActivity.this, getResources().getText(R.string.device_connected).toString(), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        mProcessor = new Processor(mHandler);
        mConnector = new Connector(mHandler, mProcessor);

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
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(data.getExtras().getString("DeviceAddress"));
                    mConnector.setDevice(mBluetoothDevice);
                    mBluetoothConnectProgressDialog = ProgressDialog.show(this, "Connecting...", mBluetoothDevice.getName() + " : " + mBluetoothDevice.getAddress(), true, false);
                    new Thread(mConnector).start();
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
}