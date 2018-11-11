package com.maiaga;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import pl.droidsonroids.gif.GifImageView;
import pl.droidsonroids.gif.GifTextView;

public class MainActivity extends AppCompatActivity {
    private static final int requestDeviceConnect = 1;
    private static final int requestBluetooth = 2;
    private static final int displayResult = 3;
    private static final int displayThrowLibrary = 3;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getApplicationContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.setLocale(new Locale("cs")); // API 17+ only.
        res.updateConfiguration(conf, dm);

        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        mProcessor = new Processor(mHandler);
        mThrowLibrary = ThrowLibrary.getSingleton();
        mConnector = new Connector(mHandler, mProcessor, this);

        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        mDataTextView = (TextView) findViewById(R.id.dataTextView);
        mGifView = (GifImageView) findViewById(R.id.gifView);
        mPngView = (ImageView) findViewById(R.id.pngView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                Intent showResultsIntent = new Intent(MainActivity.this, ShowThrowLibraryActivity.class);
                startActivityForResult(showResultsIntent, displayThrowLibrary);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void disconnect() {
        mProcessor.stop();
        mConnector.stop();
        mDataTextView.setText("");
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
                Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(connectIntent, requestDeviceConnect);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(mCurrentConnectorConnectionState == ConnectorConnectionState.Connecting)
            disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mProcessor != null)
            mProcessor.resetThrow();
    }

    private Handler mHandler = new Handler()
    {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg)
        {
            Bundle bundle = msg.getData();
            String key = bundle.getString("key", "_");
            String data = bundle.getString("data", "_");
            String subData = bundle.getString("subData", "_");

            Log.i("STATUS", "Key: " + key + ", data: " + data);
            switch(key) {
                case "processorData":
                    if(mCurrentProcessorConnectionState == ProcessorConnectionState.FetchingDataGps)
                        mDataTextView.setText(data);
                    break;
                case "connectorState":
                    ConnectorConnectionState connectorConnectionState = ConnectorConnectionState.valueOf(data);
                    setCurrentConnectorConnectionState(connectorConnectionState);
                    break;
                case "processorConnectionState":
                    ProcessorConnectionState processorConnectionState = ProcessorConnectionState.valueOf(data);
                    setCurrentProcessorConnectionState(processorConnectionState);
                    break;
                case "processorThrowState":
                    ThrowState throwState = ThrowState.valueOf(data);
                    setCurrentThrowState(throwState, subData);
                    break;
                case "processorToConnector":
                    switch(data) {
                        case "reconnect":
                            mConnector.reconnect();
                            break;
                    }
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
        if(mCurrentConnectorConnectionState == connectorConnectionState)
            return;
        mCurrentConnectorConnectionState = connectorConnectionState;
        showStatus(mCurrentConnectorConnectionState.toHumanReadableString());

        // Has to be done this way - this is the way menu is handled.
        invalidateOptionsMenu();

        // Main button.
        if(mConnector.isConnected() || mCurrentConnectorConnectionState == ConnectorConnectionState.Connecting) {
            findViewById(R.id.button).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.button).setVisibility(View.VISIBLE);
        }

        mGifView.setVisibility(View.GONE);
        switch (mCurrentConnectorConnectionState) {
            case ReadyToConnect:
                break;
            case Connecting:
                mGifView.setVisibility(View.VISIBLE);
                mGifView.setImageResource(R.drawable.connecting);
                break;
            case Connected:
                mGifView.setImageResource(R.drawable.trying_to_fetch_data);
                mGifView.setVisibility(View.VISIBLE);
                new Thread(mProcessor).start();
                break;
            case CantConnect:
                mDataTextView.setText("");
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setCurrentProcessorConnectionState(ProcessorConnectionState processorConnectionState) {
        if(mCurrentProcessorConnectionState == processorConnectionState)
            return;
        mCurrentProcessorConnectionState = processorConnectionState;
        showStatus(mCurrentProcessorConnectionState.toHumanReadableString());
        mPngView.setVisibility(View.GONE);
        mGifView.setVisibility(View.GONE);

        switch(mCurrentProcessorConnectionState) {
            case TryingToFetchData:
                mGifView.setVisibility(View.VISIBLE);
                mGifView.setImageResource(R.drawable.trying_to_fetch_data);
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
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void setCurrentThrowState(ThrowState throwState, String data) {
        if(mCurrentThrowState == throwState) {
            if(mCurrentThrowState != ThrowState.NoThrow)
                return;
        }
        mCurrentThrowState = throwState;

        if(mCurrentThrowState != ThrowState.ResultsAvailable) {
            if(mCurrentProcessorConnectionState == ProcessorConnectionState.FetchingDataGps)
                showStatus(mCurrentThrowState.toHumanReadableString());

            mPngView.setVisibility(View.GONE);
        }

        mGifView.setVisibility(View.GONE);
        switch(mCurrentThrowState) {
            case NoThrow:
                if(mCurrentProcessorConnectionState == ProcessorConnectionState.FetchingDataGps) {
                    mPngView.setVisibility(View.VISIBLE);
                    mPngView.setImageResource(R.drawable.no_throw);
                }
                break;
            case InThrow:
                mGifView.setVisibility(View.VISIBLE);
                mGifView.setImageResource(R.drawable.in_throw);
                break;
            case AfterThrow:
                mPngView.setVisibility(View.VISIBLE);
                mPngView.setImageResource(R.drawable.after_throw);
                break;
            case ResultsAvailable:
                Intent displayResultsIntent = new Intent(MainActivity.this, DisplayResultActivity.class);
                Bundle mBundle = new Bundle();
                mBundle.putString("results", data);
                mThrowLibrary.add(data);
                displayResultsIntent.putExtras(mBundle);
                startActivityForResult(displayResultsIntent, displayResult);
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
                    Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(connectIntent, requestDeviceConnect);
                }
                else {
                    showStatus(getResources().getText(R.string.denied_bt).toString());
                }
                break;

            case displayResult:
                if(resultCode == RESULT_OK) {

                }
                else {
                    showStatus(getResources().getText(R.string.general_problem).toString());
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
    private GifImageView mGifView;
    private ImageView mPngView;
    private ConnectorConnectionState mCurrentConnectorConnectionState;
    private ProcessorConnectionState mCurrentProcessorConnectionState;
    private ThrowState mCurrentThrowState;
    private ThrowLibrary mThrowLibrary;
    private Processor mProcessor;
    private Connector mConnector;
}