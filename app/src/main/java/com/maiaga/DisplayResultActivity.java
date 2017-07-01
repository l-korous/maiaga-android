package com.maiaga;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.maiaga.R;

import java.text.DecimalFormat;

public class DisplayResultActivity extends AppCompatActivity {

    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        Result result = Result.fromString(getIntent().getExtras().getString("results"));
        if (result != null) {
            DecimalFormat df1 = new DecimalFormat("#.0");
            mStatusTextView.setText("Distance: " +
                    df1.format(result.distance) +
                    " m\r\nElevation: " +
                    df1.format(result.maxAltitude) +
                    " m\r\nMax. speed: " +
                    df1.format(result.maxSpeed) +
                    "km/h\r\nAir time: " +
                    df1.format(result.duration) +
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
        finish();
    }
}