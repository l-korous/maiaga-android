package com.maiaga;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import pl.droidsonroids.gif.GifTextView;

public class MainActivity extends AppCompatActivity {
    private static final int requestDeviceConnect = 1;
    private static final int requestBluetooth = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        mProcessor = new Processor(mHandler);
        mConnector = new MockConnector(mHandler, mProcessor, this);

        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        mGifView = (GifTextView) findViewById(R.id.gifView);
        mPngView = (ImageView) findViewById(R.id.pngView);

        mGifView.setVisibility(View.INVISIBLE);
        mPngView.setVisibility(View.INVISIBLE);
        mGifView.setBackgroundResource(R.drawable.in_throw);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(mConnector.isConnected()) {
            menu.findItem(R.id.connect).setVisible(false);
            menu.findItem(R.id.disconnect).setVisible(true);
        }
        else {
            menu.findItem(R.id.connect).setVisible(true);
            menu.findItem(R.id.disconnect).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect:
                connect();
                return true;
            case R.id.disconnect:
                disconnect();
                return true;
            case R.id.results:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void disconnect() {
        mProcessor.reset();
        mConnector.stop();
        showOkToast("Disconnected");
        updateConnectionState();
    }

    @Override
    public void onBackPressed() {
        if(mCurrentConnectorConnectionState == ConnectorConnectionState.Connecting)
            disconnect();
    }

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            Bundle bundle = msg.getData();
            String key = bundle.getString("key", "_");

            Log.i("STATUS", "Key: " + key);
            switch(key) {
                case "processorData":
                    String data = bundle.getString("data", "_");
                    mStatusTextView.setText(data);
                    break;
                case "connectorState":
                    ConnectorConnectionState connectorConnectionState = ConnectorConnectionState.valueOf(bundle.getString("data"));
                    setCurrentConnectorConnectionState(connectorConnectionState);
                    break;
                case "processorConnectionState":
                    ProcessorConnectionState processorConnectionState = ProcessorConnectionState.valueOf(bundle.getString("data"));
                    setCurrentProcessorConnectionState(processorConnectionState);
                    break;
                case "processorThrowState":
                    ThrowState throwState = ThrowState.valueOf(bundle.getString("data"));
                    setCurrentThrowState(throwState);
                    break;
            }
        }
    };

    private void setCurrentConnectorConnectionState(ConnectorConnectionState connectorConnectionState) {
        mCurrentConnectorConnectionState = connectorConnectionState;

        switch (mCurrentConnectorConnectionState) {
            case Connecting:
                mGifView.setVisibility(View.VISIBLE);
                mPngView.setVisibility(View.INVISIBLE);
                mGifView.setBackgroundResource(R.drawable.connecting);
                showStatus("Bad bluetooth signal, get closer to the MAIAGA device...");
                break;
            case Connected:
                showOkToast("Connected");
                mGifView.setVisibility(View.INVISIBLE);
                mPngView.setVisibility(View.INVISIBLE);
                updateConnectionState();
                new Thread(mProcessor).start();
                break;
            case CantConnect:
                showStatus("Bad bluetooth signal, get closer to the MAIAGA device...");
                mGifView.setVisibility(View.INVISIBLE);
                mPngView.setVisibility(View.INVISIBLE);
                mGifView.setBackgroundResource(R.drawable.disconnected);
                updateConnectionState();
                break;
        }
    }

    private void setCurrentProcessorConnectionState(ProcessorConnectionState processorConnectionState) {
        mCurrentProcessorConnectionState = processorConnectionState;

        switch(mCurrentProcessorConnectionState) {
            case TryingToFetchData:
                mGifView.setVisibility(View.VISIBLE);
                mPngView.setVisibility(View.INVISIBLE);
                mGifView.setBackgroundResource(R.drawable.trying_to_fetch_fata);
                showStatus("Trying to fetch data...");
                break;
            case FetchingDataGps:
                mGifView.setVisibility(View.INVISIBLE);
                mPngView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.fetching_data_gps);
                showStatus("Waiting for throw...");
                break;
            case FetchingDataNoGps:
                mGifView.setVisibility(View.INVISIBLE);
                mPngView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.fetching_data_no_data_temporary);
                showStatus("Waiting for GPS data...");
                break;
            case FetchingDataNoDataTemporary:
                mGifView.setVisibility(View.INVISIBLE);
                mPngView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.fetching_data_no_data_temporary);
                showStatus("Bad bluetooth signal, get closer to MAIAGA device...");
                break;
            case FetchingDataNoDataShouldReconnect:
                updateConnectionState();
                mGifView.setVisibility(View.VISIBLE);
                mPngView.setVisibility(View.INVISIBLE);
                mGifView.setBackgroundResource(R.drawable.connecting);
                showStatus("Bad bluetooth signal, get closer to the MAIAGA device...");
                new Thread(mConnector).start();
                break;
        }
    }

    private void setCurrentThrowState(ThrowState throwState) {
        mCurrentThrowState = throwState;
        switch(throwState) {
            case NoThrow:
                mGifView.setVisibility(View.INVISIBLE);
                mPngView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.no_throw);
                showStatus("Waiting for a throw...");
                break;
            case InThrow:
                mGifView.setVisibility(View.VISIBLE);
                mPngView.setVisibility(View.INVISIBLE);
                mGifView.setBackgroundResource(R.drawable.in_throw);
                showStatus("Flying...");
            case AfterThrow:
                mProcessor.reset();
                showOkToast("Cooool");
                break;
        }
    }

    private void connect() {
        if(!mConnector.isBluetoothAvailable()) {
            showStatus(getResources().getText(R.string.no_bt).toString());
        }
        else {
            if (!mConnector.isBluetoothEnabled()) {
                Intent enableBtIntent = mConnector.createBluetoothEnableIntent();
                startActivityForResult(enableBtIntent, requestBluetooth);
            } else {
                Intent connectIntent = new Intent(MainActivity.this, MockDeviceListActivity.class);
                startActivityForResult(connectIntent, requestDeviceConnect);
            }
        }
    }

    public void buttonClick(View view) {
        connect();
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case requestDeviceConnect:
                if (resultCode == RESULT_OK)
                {
                    String deviceName = mConnector.setDeviceReturnName(data.getExtras().getString("DeviceAddress"));
                    new Thread(mConnector).start();
                }
                break;

            case requestBluetooth:
                if(resultCode == RESULT_OK) {
                    Intent connectIntent = new Intent(MainActivity.this, MockDeviceListActivity.class);
                    startActivityForResult(connectIntent, requestDeviceConnect);
                }
                else {
                    showStatus(getResources().getText(R.string.denied_bt).toString());
                }
                break;
        }
    }

    private void showOkToast(String status) {
        Toast.makeText(MainActivity.this, status, Toast.LENGTH_LONG).show();
    }

    private void showStatus(String status) {
        mStatusTextView.setText(status);
    }

    private void updateConnectionState() {
        invalidateOptionsMenu();
        if(mConnector.isConnected())
            findViewById(R.id.button).setVisibility(View.GONE);
        else
            findViewById(R.id.button).setVisibility(View.VISIBLE);
    }

    private TextView mStatusTextView;
    private GifTextView mGifView;
    private ImageView mPngView;
    private ProgressDialog mProgressDialog;
    private ConnectorConnectionState mCurrentConnectorConnectionState;
    private ProcessorConnectionState mCurrentProcessorConnectionState;
    private ThrowState mCurrentThrowState;
    private Processor mProcessor;
    private MockConnector mConnector;
}