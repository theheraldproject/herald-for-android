//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.app;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vmware.herald.sensor.Sensor;
import com.vmware.herald.sensor.SensorArray;
import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.analysis.SocialDistance;
import com.vmware.herald.sensor.datatype.ImmediateSendData;
import com.vmware.herald.sensor.datatype.Location;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.SensorState;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements SensorDelegate {
    private final static String tag = MainActivity.class.getName();
    /// REQUIRED: Unique permission request code, used by requestPermission and onRequestPermissionsResult.
    private final static int permissionRequestCode = 1249951875;
    /// Test UI specific data, not required for production solution.
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("MMdd HH:mm:ss");
    private long didDetect = 0, didRead = 0, didReceive = 0, didMeasure = 0, didShare = 0, didVisit = 0;
    private final Map<TargetIdentifier, String> payloads = new ConcurrentHashMap<>();
    private final Map<String, Date> didReadPayloads = new ConcurrentHashMap<>();
    private final Map<String, Date> didSharePayloads = new ConcurrentHashMap<>();

    // MARK:- Social mixing
    private final SocialDistance socialMixingScore = new SocialDistance();
    private TimeInterval socialMixingScoreUnit = new TimeInterval(60);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // REQUIRED : Ensure app has all required permissions
        requestPermissions();

        // Test UI specific process to gather data from sensor for presentation
        final Sensor sensor = AppDelegate.getAppDelegate().sensor();
        sensor.add(this);
        sensor.add(socialMixingScore);
        ((TextView) findViewById(R.id.device)).setText(SensorArray.deviceDescription);
        ((TextView) findViewById(R.id.payload)).setText("PAYLOAD : " + ((SensorArray) AppDelegate.getAppDelegate().sensor()).payloadData().shortName());
    }

    /// REQUIRED : Request application permissions for sensor operation.
    private void requestPermissions() {
        // Check and request permissions
        final List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.BLUETOOTH);
        requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        requiredPermissions.add(Manifest.permission.WAKE_LOCK);
        final String[] requiredPermissionsArray = requiredPermissions.toArray(new String[0]);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requiredPermissionsArray, permissionRequestCode);
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissionsArray, permissionRequestCode);
        }
    }

    /// REQUIRED : Handle permission results.
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == permissionRequestCode) {
            boolean permissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                final String permission = permissions[i];
                if (grantResults[i] != PERMISSION_GRANTED) {
                    Log.e(tag, "Permission denied (permission=" + permission + ")");
                    permissionsGranted = false;
                } else {
                    Log.d(tag, "Permission granted (permission=" + permission + ")");
                }
            }

            if (!permissionsGranted) {
                Log.e(tag, "Application does not have all required permissions to start (permissions=" + Arrays.asList(permissions) + ")");
            }
        }
    }

    // MARK:- Test UI specific functions, not required in production solution.

    /// Update list of detected devices, including detection method(s) and last seen timestamp.
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
                    final Date didReadPayloadTime = didReadPayloads.get(payloadShortName);
                    final Date didSharePayloadTime = didSharePayloads.get(payloadShortName);
                    payloadLastSeenDates.put(payloadShortName, new Date(
                            Math.max((didReadPayloadTime == null ? 0 : didReadPayloadTime.getTime()),
                                    (didSharePayloadTime == null ? 0 : didSharePayloadTime.getTime()))));
                }
                final List<String> payloadShortNameList = new ArrayList<>(payloadShortNames.keySet());
                Collections.sort(payloadShortNameList);
                final StringBuilder stringBuilder = new StringBuilder();
                for (String payloadShortName : payloadShortNameList) {
                    stringBuilder.append(payloadShortName);
                    stringBuilder.append(" [");
                    stringBuilder.append(payloadShortNames.get(payloadShortName));
                    stringBuilder.append("]");
                    final Date lastSeen = payloadLastSeenDates.get(payloadShortName);
                    if (lastSeen != null) {
                        stringBuilder.append(" (");
                        stringBuilder.append(dateFormatter.format(lastSeen));
                        stringBuilder.append(")");
                    }
                    stringBuilder.append("\n");
                }
                ((TextView) findViewById(R.id.detection)).setText("DETECTION (" + payloadShortNames.size() + ")");
                final TextView textView = findViewById(R.id.payloads);
                textView.setText(stringBuilder.toString());
                textView.setMovementMethod(new ScrollingMovementMethod());
            }
        });
    }

    // Update social distance score
    private void updateSocialDistance(TimeInterval unit) {
        final long millisecondsPerUnit = unit.value * 1000;
        final List<TextView> labels = new ArrayList<>();
        labels.add((TextView) findViewById(R.id.socialMixingScore00));
        labels.add((TextView) findViewById(R.id.socialMixingScore01));
        labels.add((TextView) findViewById(R.id.socialMixingScore02));
        labels.add((TextView) findViewById(R.id.socialMixingScore03));
        labels.add((TextView) findViewById(R.id.socialMixingScore04));
        labels.add((TextView) findViewById(R.id.socialMixingScore05));
        labels.add((TextView) findViewById(R.id.socialMixingScore06));
        labels.add((TextView) findViewById(R.id.socialMixingScore07));
        labels.add((TextView) findViewById(R.id.socialMixingScore08));
        labels.add((TextView) findViewById(R.id.socialMixingScore09));
        labels.add((TextView) findViewById(R.id.socialMixingScore10));
        labels.add((TextView) findViewById(R.id.socialMixingScore11));
        final long epoch = (new Date().getTime() / millisecondsPerUnit) - 11;
        for (int i=0; i<=11; i++) {
            // Compute score for time slot
            final Date start = new Date((epoch + i) * millisecondsPerUnit);
            final Date end = new Date((epoch + i + 1) * millisecondsPerUnit);
            final double score = socialMixingScore.scoreByProximity(start, end, -25, -70);
            // Present textual score
            final String scoreForPresentation = Integer.toString((int) Math.round(score * 100));
            labels.get(i).setText(scoreForPresentation);
            // Change color according to score
            if (score < 0.1) {
                labels.get(i).setBackgroundColor(ContextCompat.getColor(this, R.color.systemGreen));
            } else if (score < 0.5) {
                labels.get(i).setBackgroundColor(ContextCompat.getColor(this, R.color.systemOrange));
            } else {
                labels.get(i).setBackgroundColor(ContextCompat.getColor(this, R.color.systemRed));
            }
        }
    }

    public void onClickSocialMixingScoreUnit(View v) {
        final Map<TextView, TimeInterval> mapping = new HashMap<>(12);
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitH24), new TimeInterval(24 * 60 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitH12), new TimeInterval(12 * 60 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitH4), new TimeInterval(4 * 60 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitH1), new TimeInterval(1 * 60 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitM30), new TimeInterval(30 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitM15), new TimeInterval(15 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitM5), new TimeInterval(5 * 60));
        mapping.put((TextView) findViewById(R.id.socialMixingScoreUnitM1), new TimeInterval(1 * 60));
        final int active = ContextCompat.getColor(this, R.color.systemBlue);
        final int inactive = ContextCompat.getColor(this, R.color.systemGray);
        final TextView setTo = (TextView) v;
        for (TextView key : mapping.keySet()) {
            if (setTo.getId() == key.getId()) {
                key.setTextColor(active);
                socialMixingScoreUnit = mapping.get(key);
            } else {
                key.setTextColor(inactive);
            }
        }
        updateSocialDistance(socialMixingScoreUnit);
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        this.didDetect++;
        final String text = Long.toString(this.didDetect);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didDetectCount);
                textView.setText(text);
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        this.didRead++;
        this.didReadPayloads.put(didRead.shortName(), new Date());
        this.payloads.put(fromTarget, didRead.shortName());
        final String text = Long.toString(this.didRead);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didReadCount);
                textView.setText(text);
                updateDetection();
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget) {
        this.didReceive++;
        // TODO expose received immediate send data on the demo UI
//        final String timestamp = dateFormatter.format(new Date());
//        final String text = "didReceive: " + this.didReceive + " (" + timestamp + ")";
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                final TextView textView = findViewById(R.id.didReceive);
//                textView.setText(text);
//                updateDetection();
//            }
//        });
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        this.didShare++;
        final Date now = new Date();
        for (PayloadData payloadData : didShare) {
            this.didSharePayloads.put(payloadData.shortName(), now);
        }
        final String text = Long.toString(this.didShare);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didShareCount);
                textView.setText(text);
            }
        });
        updateDetection();
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        this.didMeasure++;
        final String text = Long.toString(this.didMeasure);
        final String payloadShortName = payloads.get(fromTarget);
        if (payloadShortName != null) {
            didReadPayloads.put(payloadShortName, new Date());
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didMeasureCount);
                textView.setText(text);
                if (payloadShortName != null) {
                    updateDetection();
                }
                updateSocialDistance(socialMixingScoreUnit);
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        this.didVisit++;
        final String text = Long.toString(this.didVisit);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView textView = findViewById(R.id.didVisitCount);
                textView.setText(text);
            }
        });
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
        // High level integration API is not used as the test app is using the low level API to present all the detection events.
    }

    @Override
    public void sensor(SensorType sensor, SensorState didUpdateState) {
        // Sensor state is already presented by the operating system, so not duplicating in the test app.
    }
}