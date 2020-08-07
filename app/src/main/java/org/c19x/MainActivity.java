package org.c19x;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.c19x.sensor.R;
import org.c19x.sensor.ble.ConcreteBLESensor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConcreteBLESensor.checkPermissions(this);
    }
}