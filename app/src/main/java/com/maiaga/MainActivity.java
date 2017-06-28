package com.maiaga;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
        mDataTextView = (TextView) findViewById(R.id.dataTextView);
        mGifView = (GifTextView) findViewById(R.id.gifView);
        mPngView = (ImageView) findViewById(R.id.pngView);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(mConnector.isConnected() || mCurrentConnectorConnectionState == ConnectorConnectionState.Connecting) {
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
        mProcessor.stopAndReset();
        mConnector.reset();
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

    @Override
    public void onBackPressed() {
        if(mCurrentConnectorConnectionState == ConnectorConnectionState.Connecting)
            disconnect();
    }

    private Handler mHandler = new Handler()
    {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg)
        {
            Bundle bundle = msg.getData();
            String key = bundle.getString("key", "_");

            Log.i("STATUS", "Key: " + key + ", data: " + bundle.getString("data", "_"));
            switch(key) {
                case "processorData":
                    String data = bundle.getString("data", "_");
                    mDataTextView.setText(data);
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

    private void showConnectDisconnect(boolean showConnect) {
        if(showConnect) {
            findViewById(R.id.button).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.button).setVisibility(View.GONE);
        }
    }

    private void setCurrentConnectorConnectionState(ConnectorConnectionState connectorConnectionState) {
        mCurrentConnectorConnectionState = connectorConnectionState;
        showStatus(mCurrentConnectorConnectionState.toHumanReadableString());
        mPngView.setVisibility(View.GONE);
        mGifView.setVisibility(View.GONE);

        // Has to be done this way - this is the way menu is handled.
        invalidateOptionsMenu();

        // Main button.
        if(mConnector.isConnected() || mCurrentConnectorConnectionState == ConnectorConnectionState.Connecting) {
            findViewById(R.id.button).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.button).setVisibility(View.VISIBLE);
        }

        switch (mCurrentConnectorConnectionState) {
            case ReadyToConnect:
                break;
            case Connecting:
                mGifView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.connecting);
                break;
            case Connected:
                new Thread(mProcessor).start();
                break;
            case CantConnect:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setCurrentProcessorConnectionState(ProcessorConnectionState processorConnectionState) {
        mCurrentProcessorConnectionState = processorConnectionState;
        showStatus(mCurrentProcessorConnectionState.toHumanReadableString());
        mPngView.setVisibility(View.GONE);
        mGifView.setVisibility(View.GONE);

        switch(mCurrentProcessorConnectionState) {
            case TryingToFetchData:
                mGifView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.trying_to_fetch_fata);
                break;
            case FetchingDataGps:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.fetching_data_gps);
                break;
            case FetchingDataNoGps:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.fetching_data_no_data_temporary);
                break;
            case FetchingDataNoDataTemporary:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.fetching_data_no_data_temporary);
                break;
            case FetchingDataNoDataShouldReconnect:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.fetching_data_no_data_temporary);
                new Thread(mConnector).start();
                break;
        }
    }

    private void setCurrentThrowState(ThrowState throwState) {
        mCurrentThrowState = throwState;
        showStatus(mCurrentThrowState.toHumanReadableString());
        mPngView.setVisibility(View.GONE);
        mGifView.setVisibility(View.GONE);
        switch(throwState) {
            case NoThrow:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.no_throw);
                break;
            case InThrow:
                mGifView.setVisibility(View.VISIBLE);
                mGifView.setBackgroundResource(R.drawable.in_throw);
                break;
            case AfterThrow:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.after_throw);
                mProcessor.stopAndGetResults();
                break;
            case ResultsAvailable:
                Intent displayResultsIntent = new Intent(MainActivity.this, DisplayResultActivity.class);
                startActivity(displayResultsIntent);
                break;
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

    private TextView mStatusTextView;
    private TextView mDataTextView;
    private GifTextView mGifView;
    private ImageView mPngView;
    private ProgressDialog mProgressDialog;
    private ConnectorConnectionState mCurrentConnectorConnectionState;
    private ProcessorConnectionState mCurrentProcessorConnectionState;
    private ThrowState mCurrentThrowState;
    private Processor mProcessor;
    private MockConnector mConnector;
}