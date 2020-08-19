package org.c19x;

import android.os.Bundle;
import android.os.PowerManager;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity implements SensorDelegate {
    private PowerManager.WakeLock wakeLock;
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("MMdd HH:mm:ss");
    private long didDetect = 0, didRead = 0, didMeasure = 0, didShare = 0, didVisit = 0;
    private final Map<TargetIdentifier, String> payloads = new ConcurrentHashMap<>();
    private final Map<String, Date> didReadPayloads = new ConcurrentHashMap<>();
    private final Map<String, Date> didSharePayloads = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure app has location permission for Bluetooth
        ConcreteBLESensor.checkPermissions(this);

        // Use wake lock to keep CPU awake
        //wakeLock = ConcreteBLESensor.getWakeLock(this);


        // Gather data from sensor for presentation
        AppDelegate.getAppDelegate().sensor.add(this);

        ((TextView) findViewById(R.id.device)).setText(SensorArray.deviceDescription);
        ((TextView) findViewById(R.id.payload)).setText("PAYLOAD : " + ((SensorArray) AppDelegate.getAppDelegate().sensor).payloadData().shortName());
    }

    @Override
    protected void onDestroy() {
        if (wakeLock != null) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    private void updateDetection() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Map<String, String> payloadShortNames = new HashMap<>();
                final Map<String, Date> payloadLastSeenDates = new HashMap<>();
                for (String payloadShortName : didReadPayloads.keySet()) {
                    payloadShortNames.put(payloadShortName, "read");
                    payloadLastSeenDates.put(payloadShortName, didReadPayloads.get(payloadShortName));
                }
                for (String payloadShortName : didSharePayloads.keySet()) {
                    payloadShortNames.put(payloadShortName, (payloadShortNames.containsKey(payloadShortName) ? "read,shared" : "shared"));
                    payloadLastSeenDates.put(payloadShortName, new Date(Math.max(didReadPayloads.get(payloadShortName).getTime(), didSharePayloads.get(payloadShortName).getTime())));
                }
                final List<String> payloadShortNameList = new ArrayList<>(payloadShortNames.keySet());
                Collections.sort(payloadShortNameList);
                final StringBuilder stringBuilder = new StringBuilder();
                for (String payloadShortName : payloadShortNameList) {
                    stringBuilder.append(payloadShortName);
                    stringBuilder.append(" [");
                    stringBuilder.append(payloadShortNames.get(payloadShortName));
                    stringBuilder.append("] (");
                    final String timestamp = dateFormatter.format(payloadLastSeenDates.get(payloadShortName));
                    stringBuilder.append(timestamp);
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
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didDetect: " + this.didDetect + " (" + timestamp + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didDetect);
                textView.setText(text);
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        this.didRead++;
        this.didReadPayloads.put(didRead.shortName(), new Date());
        this.payloads.put(fromTarget, didRead.shortName());
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didRead: " + this.didRead + " (" + timestamp + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didRead);
                textView.setText(text);
                updateDetection();
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        this.didShare++;
        final Date now = new Date();
        for (PayloadData payloadData : didShare) {
            this.didSharePayloads.put(payloadData.shortName(), now);
        }
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didShare: " + this.didShare + " (" + timestamp + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didShare);
                textView.setText(text);
            }
        });
        updateDetection();
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        this.didMeasure++;
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didMeasure: " + this.didMeasure + " (" + timestamp + ")";
        final String payloadShortName = payloads.get(fromTarget);
        if (payloadShortName != null) {
            didReadPayloads.put(payloadShortName, new Date());
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didMeasure);
                textView.setText(text);
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        this.didVisit++;
        final String timestamp = dateFormatter.format(new Date());
        final String text = "didVisit: " + this.didVisit + " (" + timestamp + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didVisit);
                textView.setText(text);
            }
        });
    }
}