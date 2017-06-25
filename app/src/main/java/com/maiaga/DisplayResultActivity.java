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

        mStatusTextView.setText("Distance: 78 m\r\nElevation: 17.5 m\r\nMax. velocity: 42.8 km/h");
    }
}