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
        mErrorTextView = (TextView) findViewById(R.id.errorTextView);
        mGifView = (GifTextView) findViewById(R.id.gifView);
        mPngView = (ImageView) findViewById(R.id.pngView);

        mGifView.setVisibility(View.VISIBLE);
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
        showOkStatus("Disconnected");
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
                    mCurrentConnectorConnectionState = connectorConnectionState;
                    switch(connectorConnectionState) {
                        case Connecting:
                            showOkProgress("Connecting...");
                            break;
                        case Connected:
                            showOkStatus("Connected");
                            updateConnectionState();
                            new Thread(mProcessor).start();
                            break;
                        case CantConnect:
                            showErrorStatus("Bad bluetooth signal, get closer to the MAIAGA device...");
                            updateConnectionState();
                            break;
                    }
                    break;
                case "processorConnectionState":
                    ProcessorConnectionState processorConnectionState = ProcessorConnectionState.valueOf(bundle.getString("data"));
                    mCurrentProcessorConnectionState = processorConnectionState;
                    switch(processorConnectionState) {
                        case TryingToFetchData:
                            showOkProgress("Initializing...");
                            break;
                        case FetchingDataGps:
                            clearStatuses();
                            break;
                        case FetchingDataNoGps:
                            showOkProgress("Connected, waiting for GPS data...");
                            break;
                        case FetchingDataNoDataTemporary:
                            showOkProgress("Bad bluetooth signal, get closer to MAIAGA device...");
                            break;
                        case FetchingDataNoDataShouldReconnect:
                            updateConnectionState();
                            showOkProgress("Reconnecting...");
                            new Thread(mConnector).start();
                            break;
                    }
                    break;
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

    private void connect() {
        if(!mConnector.isBluetoothAvailable()) {
            showErrorStatus(getResources().getText(R.string.no_bt).toString());
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

    private void updateConnectionState() {
        invalidateOptionsMenu();
        if(mConnector.isConnected())
            findViewById(R.id.button).setVisibility(View.GONE);
        else
            findViewById(R.id.button).setVisibility(View.VISIBLE);
    }

    private TextView mStatusTextView;
    private TextView mErrorTextView;
    private GifTextView mGifView;
    private ImageView mPngView;
    private ProgressDialog mProgressDialog;
    private ConnectorConnectionState mCurrentConnectorConnectionState;
    private ProcessorConnectionState mCurrentProcessorConnectionState;
    private Processor mProcessor;
    private MockConnector mConnector;
}