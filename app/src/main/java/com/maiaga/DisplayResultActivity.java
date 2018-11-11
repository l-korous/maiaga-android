package com.maiaga;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Locale;

public class DisplayResultActivity extends AppCompatActivity {

    private TextView mStatusTextView;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getApplicationContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.setLocale(new Locale("cs")); // API 17+ only.
        res.updateConfiguration(conf, dm);

        setContentView(R.layout.activity_display_result);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        ThrowResult throwResult = ThrowResult.fromString(getIntent().getExtras().getString("results"));
        if (throwResult != null) {
            DecimalFormat df1 = new DecimalFormat("##0.0");
            mStatusTextView.setText(getResources().getText(R.string.distance).toString() + ":\t\t" +
                    df1.format(throwResult.distance) +
                    " m\n" + getResources().getText(R.string.maxAltitude).toString() + ":\t\t" +
                    df1.format(throwResult.maxAltitude) +
                    " m\n" + getResources().getText(R.string.maxSpeed).toString() + ":\t\t" +
                    df1.format(throwResult.maxSpeed) +
                    " km/h\n" + getResources().getText(R.string.airTime).toString() + ":\t\t" +
                    df1.format(throwResult.duration) +
                    " s"
            );
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
}