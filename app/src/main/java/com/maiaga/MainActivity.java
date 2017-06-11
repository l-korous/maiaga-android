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

import com.maiaga.R;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int requestDeviceConnect = 1;
    private static final int requestBluetooth = 2;

    private TextView mStatusTextView;
    private TextView mErrorTextView;
    private ProgressDialog mProgressDialog;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;

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
                case "processorData":
                    String data = bundle.getString("data", "_");
                    mStatusTextView.setText(data);
                    break;
                case "processorConnectionState":
                    ConnectionState connectionState = ConnectionState.valueOf(bundle.getString("data"));
                    switch(connectionState) {
                        case TryingToFetchData:
                            showOkProgress("Initializing...");
                            break;
                        case FetchingDataNoGps:
                            showOkProgress("Connected, waiting GPS data...");
                            break;
                        case FetchingDataNoDataTemporary:
                            showOkProgress("Bad bluetooth signal, get closer to MAIAGA device...");
                            break;
                        case FetchingDataNoDataShouldReconnect:
                            showOkProgress("Reconnecting...");
                            new Thread(mConnector).start();
                            break;
                    }
                case "processorThrowState":
                    ThrowState throwState = ThrowState.valueOf(bundle.getString("data"));
                    switch(throwState) {
                        case NoThrow:
                            // Tohle je blby, to se nema stavat, na NoThrow se dostane Processor tak, ze by nemel poslat zpravu (pri inicializaci runu)
                            break;
                        case InThrow:
                            showOkProgress("Flying...");
                        case AfterThrow:
                            mProcessor.reset();
                            showOkStatus("Cooool");
                            break;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mProcessor = new Processor(mHandler);
        mConnector = new Connector(mHandler, mProcessor, mBluetoothAdapter);

        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        mErrorTextView = (TextView) findViewById(R.id.errorTextView);
    }

    public void buttonClick(View view) {

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
        if(mProgressDialog != null)
            mProgressDialog.dismiss();
    }
}