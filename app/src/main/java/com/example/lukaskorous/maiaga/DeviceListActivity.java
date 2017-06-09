package com.example.lukaskorous.maiaga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceListActivity extends AppCompatActivity
{
    protected static final String TAG = "TAG";
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle mSavedInstanceState)
    {
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));

        super.onCreate(mSavedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_device_name);

        ListView mPairedListView = (ListView) findViewById(R.id.paired_devices);
        mPairedListView.setAdapter(mPairedDevicesArrayAdapter);
        mPairedListView.setOnItemClickListener(mDeviceClickListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        List<BluetoothDevice> mAllPairedDevices = new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices());
        List<BluetoothDevice> mPairedDevices =  new ArrayList<BluetoothDevice>();
        for (BluetoothDevice mDevice : mAllPairedDevices) {
            if(mDevice.getName().startsWith("Maiaga"))
                mPairedDevices.add(mDevice);
        }

        if (mPairedDevices.size() > 0)
        {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice mDevice : mPairedDevices)
            {
                mPairedDevicesArrayAdapter.add(mDevice.getName() + "\n" + mDevice.getAddress());
            }
        }
        else
        {
            String mNoDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(mNoDevices);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mBluetoothAdapter != null)
        {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> mAdapterView, View mView, int mPosition, long mLong)
        {
            mBluetoothAdapter.cancelDiscovery();
            String mDeviceInfo = ((TextView) mView).getText().toString();
            String mDeviceAddress = mDeviceInfo.substring(mDeviceInfo.length() - 17);
            Bundle mBundle = new Bundle();
            mBundle.putString("DeviceAddress", mDeviceAddress);
            Intent mBackIntent = new Intent();
            mBackIntent.putExtras(mBundle);
            setResult(Activity.RESULT_OK, mBackIntent);
            finish();
        }
    };
}