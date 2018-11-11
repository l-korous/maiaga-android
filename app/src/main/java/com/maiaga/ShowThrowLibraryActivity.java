package com.maiaga;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by lukas on 6/12/2017.
 */

public class ShowThrowLibraryActivity extends AppCompatActivity {
    public static final String fileName = "throwLibrary";

    @Override
    protected void onCreate(Bundle mSavedInstanceState)
    {
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
        super.onCreate(mSavedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_show_throw_library);

        mThrowLibrary = ThrowLibrary.getSingleton();
        ListView mThrowLibraryView = (ListView) findViewById(R.id.throw_library);
        mThrowLibraryArrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_throw_result);
        mThrowLibraryView.setAdapter(mThrowLibraryArrayAdapter);
        DecimalFormat df1 = new DecimalFormat("##0.0");
        for (ThrowResult throwResult : mThrowLibrary.mThrowResultArray)
        {
            mThrowLibraryArrayAdapter.add(throwResult.time + "\n" + getResources().getText(R.string.distance).toString() + ":\t\t" +
                    df1.format(throwResult.distance) +
                    " m\n" + getResources().getText(R.string.maxAltitude).toString() + ":\t\t" +
                    df1.format(throwResult.maxAltitude) +
                    " m\n" + getResources().getText(R.string.maxSpeed).toString() + ":\t\t" +
                    df1.format(throwResult.maxSpeed) +
                    " km/h\n" + getResources().getText(R.string.airTime).toString() + ":\t\t" +
                    df1.format(throwResult.duration) +
                    " s");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent mBackIntent = new Intent();
        setResult(Activity.RESULT_OK, mBackIntent);
        finish();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    public ThrowLibrary mThrowLibrary;
    private ArrayAdapter<String> mThrowLibraryArrayAdapter;
}
