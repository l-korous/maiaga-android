package com.maiaga;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lukaskorous.maiaga.R;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    public static final String errorMessageExtra = "com.maiaga.message";

    private static final int requestDeviceConnect = 1;
    private static final int requestBluetooth = 2;

    private TextView mStatusTextView;
    private TextView mErrorTextView;
    private ProgressDialog mProgressDialog;

    private BluetoothAdapter mBluetoothAdapter;
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
                    switch(data) {
                        case "ioException":
                            showOkProgress("Reconnecting...");
                            new Thread(mConnector).start();
                            break;
                        case "noGpsData":
                            break;
                        case "throwBegin":
                            break;
                        case "throwEnd":
                            break;
                    }
                case "connectorStatus":
                    switch(data) {
                        case "connected":
                            mStatusTextView.setText("");
                            showOkStatus(getResources().getText(R.string.device_connected).toString());
                            new Thread(mProcessor).start();
                            break;
                        case "cantConnect":
                            mProcessor.reset();
                            showErrorStatus(getResources().getText(R.string.disconnected).toString());
                            break;
                    }
                    break;
                // Empty message
                default:
                    hideProgress();
                    showOkStatus(getResources().getText(R.string.device_connected).toString());
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
            showErrorStatus(getResources().getText(R.string.no_bt).toString());
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
                    showOkProgress("Connecting...", mBluetoothDevice.getName());
                    mHandler.sendEmptyMessage(0);
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
                    showErrorStatus(getResources().getText(R.string.denied_bt).toString());
                }
                break;
        }
    }

    private void showOkProgress(String status) {
        clearStatuses();
        mProgressDialog = ProgressDialog.show(this, status, "", true, false);
    }

    private void showOkProgress(String status, String detail) {
        clearStatuses();
        mProgressDialog = ProgressDialog.show(this, status, detail, true, false);
    }

    private void showOkStatus(String status) {
        clearStatuses();
        Toast.makeText(MainActivity.this, status, Toast.LENGTH_LONG).show();
    }

    private void showErrorStatus(String status) {
        clearStatuses();
        mErrorTextView.setText(status);
    }

    private void clearStatuses() {
        mErrorTextView.setText("");
        hideProgress();
    }

    private void hideProgress() {
        mProgressDialog.dismiss();
    }
}