//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.vmware.squire.sensor.SensorArray;
import com.vmware.squire.sensor.SensorDelegate;
import com.vmware.squire.sensor.data.ConcreteSensorLogger;
import com.vmware.squire.sensor.data.SensorLogger;
import com.vmware.squire.sensor.datatype.Location;
import com.vmware.squire.sensor.datatype.PayloadData;
import com.vmware.squire.sensor.datatype.Proximity;
import com.vmware.squire.sensor.datatype.SensorState;
import com.vmware.squire.sensor.datatype.SensorType;
import com.vmware.squire.sensor.datatype.TargetIdentifier;

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
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "MainActivity");
    /// REQUIRED: Unique permission request code, used by requestPermission and onRequestPermissionsResult.
    private final static int permissionRequestCode = 1249951875;
    /// Test UI specific data, not required for production solution.
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("MMdd HH:mm:ss");
    private long didDetect = 0, didRead = 0, didMeasure = 0, didShare = 0, didVisit = 0;
    private final Map<TargetIdentifier, String> payloads = new ConcurrentHashMap<>();
    private final Map<String, Date> didReadPayloads = new ConcurrentHashMap<>();
    private final Map<String, Date> didSharePayloads = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // REQUIRED : Ensure app has all required permissions
        requestPermissions();

        // Test UI specific process to gather data from sensor for presentation
        AppDelegate.getAppDelegate().sensor().add(this);
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
                    logger.fault("Permission denied (permission={})", permission);
                    permissionsGranted = false;
                } else {
                    logger.debug("Permission granted (permission={})", permission);
                }
            }

            if (!permissionsGranted) {
                logger.fault("Application does not have all required permissions to start (permissions={})", Arrays.asList(permissions));
            }
        }
    }

    // MARK:- Test UI specific functions, not required in production solution.

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
            updateDetection();
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

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
    }

    @Override
    public void sensor(SensorType sensor, SensorState didUpdateState) {
    }
}