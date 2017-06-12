package com.maiaga;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by lukas on 6/12/2017.
 */

public class MockDeviceListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle mSavedInstanceState)
    {
        super.onCreate(mSavedInstanceState);
        Intent mBackIntent = new Intent();
        setResult(Activity.RESULT_OK, mBackIntent);
        finish();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
