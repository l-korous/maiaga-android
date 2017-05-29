package com.example.lukas.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendMessage(View view) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Intent intent = new Intent(this, DisplayMessageActivity.class);
            intent.putExtra(EXTRA_MESSAGE, "no BT");
            startActivity(intent);
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 123);
            }
        }
    }

    public void onActivityResult (int requestCode,
                                  int resultCode,
                                  Intent data) {
        if(requestCode == 123) {
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
        }
    }
}
