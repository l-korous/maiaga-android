package com.maiaga;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.maiaga.R;

public class DisplayResultActivity extends AppCompatActivity {

    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        Result result = Result.fromString(getIntent().getExtras().getString("results"));
        if (result != null) {
            mStatusTextView.setText("Distance: " +
                    result.distance +
                    " m\r\nElevation: " +
                    result.maxAltitude +
                    " m\r\nMax. speed: " +
                    result.maxSpeed +
                    "km/h\r\nAir time: " +
                    result.duration +
                    " s"
            );
        }
    }
}