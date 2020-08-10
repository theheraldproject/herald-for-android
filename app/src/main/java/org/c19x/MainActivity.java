package org.c19x;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.c19x.sensor.R;
import org.c19x.sensor.SensorArray;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.ble.ConcreteBLESensor;
import org.c19x.sensor.datatype.Location;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.TargetIdentifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorDelegate {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("MMdd HH:mm:ss");
    private long didDetect = 0, didRead = 0, didMeasure = 0, didShare = 0, didVisit = 0;
    private final Set<String> didReadPayloads = new HashSet<>();
    private final Set<String> didSharePayloads = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure app has location permission for Bluetooth
        ConcreteBLESensor.checkPermissions(this);

        // Gather data from sensor for presentation
        AppDelegate.getAppDelegate().sensor.add(this);

        ((TextView) findViewById(R.id.device)).setText(SensorArray.deviceDescription);
        ((TextView) findViewById(R.id.payload)).setText("PAYLOAD : " + ((SensorArray) AppDelegate.getAppDelegate().sensor).payloadData().shortName());
    }

    private void updateDetection() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Map<String, String> payloadShortNames = new HashMap<>();
                for (String payloadShortName : didReadPayloads) {
                    payloadShortNames.put(payloadShortName, "read");
                }
                for (String payloadShortName : didSharePayloads) {
                    payloadShortNames.put(payloadShortName, (payloadShortNames.containsKey(payloadShortName) ? "read,shared" : "shared"));
                }
                final List<String> payloadShortNameList = new ArrayList<>(payloadShortNames.keySet());
                Collections.sort(payloadShortNameList);
                final StringBuilder stringBuilder = new StringBuilder();
                for (String payloadShortName : payloadShortNameList) {
                    stringBuilder.append(payloadShortName);
                    stringBuilder.append(" (");
                    stringBuilder.append(payloadShortNames.get(payloadShortName));
                    stringBuilder.append(")\n");
                }
                ((TextView) findViewById(R.id.detection)).setText("DETECTION (" + payloadShortNames.size() + ")");
                final TextView textView = findViewById(R.id.payloads);
                textView.setText(stringBuilder.toString());
                textView.setMovementMethod(new ScrollingMovementMethod());
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        this.didDetect++;
        final TextView textView = findViewById(R.id.didDetect);
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didDetect: " + this.didDetect + " (" + timestamp + ")";
        textView.setText(text);
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        this.didRead++;
        this.didReadPayloads.add(didRead.shortName());
        final TextView textView = findViewById(R.id.didRead);
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didRead: " + this.didRead + " (" + timestamp + ")";
        textView.setText(text);
        updateDetection();
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        this.didShare++;
        for (PayloadData payloadData : didShare) {
            this.didSharePayloads.add(payloadData.shortName());
        }
        final TextView textView = findViewById(R.id.didShare);
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didShare: " + this.didShare + " (" + timestamp + ")";
        textView.setText(text);
        updateDetection();
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        this.didMeasure++;
        final TextView textView = findViewById(R.id.didMeasure);
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didMeasure: " + this.didMeasure + " (" + timestamp + ")";
        textView.setText(text);
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        this.didVisit++;
        final TextView textView = findViewById(R.id.didVisit);
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didVisit: " + this.didVisit + " (" + timestamp + ")";
        textView.setText(text);
    }
}