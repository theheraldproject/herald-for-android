//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.app;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.heraldprox.herald.sensor.Sensor;
import io.heraldprox.herald.sensor.SensorArray;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.analysis.SocialDistance;
import io.heraldprox.herald.sensor.analysis.algorithms.distance.SmoothedLinearModelAnalyser;
import io.heraldprox.herald.sensor.analysis.algorithms.distance.SelfCalibratedModel;
import io.heraldprox.herald.sensor.analysis.sampling.AnalysisDelegateManager;
import io.heraldprox.herald.sensor.analysis.sampling.AnalysisProviderManager;
import io.heraldprox.herald.sensor.analysis.sampling.AnalysisRunner;
import io.heraldprox.herald.sensor.analysis.sampling.ConcreteAnalysisDelegate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.analysis.sampling.SampleList;
import io.heraldprox.herald.sensor.analysis.sampling.SampledID;
import io.heraldprox.herald.sensor.analysis.views.Since;
import io.heraldprox.herald.sensor.data.Resettable;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements SensorDelegate, AdapterView.OnItemClickListener, Resettable {
    private final static String tag = MainActivity.class.getName();
    /// REQUIRED: Unique permission request code, used by requestPermission and onRequestPermissionsResult.
    private final static int permissionRequestCode = 1249951875;
    /// Test UI specific data, not required for production solution.
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
    private boolean foreground = false;

    // MARK:- Events
    private long didDetect = 0, didRead = 0, didMeasure = 0, didShare = 0, didReceive = 0;

    // MARK:- Detected payloads
    private final Map<TargetIdentifier,PayloadData> targetIdentifiers = new ConcurrentHashMap<>();
    private final Map<PayloadData,Target> payloads = new ConcurrentHashMap<>();
    private final List<Target> targets = new ArrayList<>();
    private TargetListAdapter targetListAdapter = null;

    // MARK:- Social mixing
    private final SocialDistance socialMixingScore = new SocialDistance();
    private TimeInterval socialMixingScoreUnit = new TimeInterval(60);

    // MARK:- Distance estimation
    // Demonstration model assumes, on average, distance between people is zero metres for up
    // to 5 minutes/day, and within 3.7 metres for up to 8 hours/day (e.g. work and home).
    // These are initial estimates based on proxemics data (Hall, 1966) for social distance
    // and social norms. Future work should analyse a sample of actual RSSI histogram
    // data to understand the distribution of distances between people and use normalisation
    // across the population using a common model (see histogram equalisation in RssiHistogram).
    // - Hall, E. (1966). The Hidden Dimension. Anchor Books. ISBN 978-0-385-08476-5
    private final SelfCalibratedModel<RSSI> smoothedLinearModel = new SelfCalibratedModel<>(
            new Distance(0), new Distance(3.7),
            TimeInterval.minutes(5), TimeInterval.hours(8),
            new TextFile(AppDelegate.getAppDelegate(), "rssi_histogram.csv"));
    private final SmoothedLinearModelAnalyser smoothedLinearModelAnalyser = new SmoothedLinearModelAnalyser(new TimeInterval(1), new TimeInterval(60), smoothedLinearModel);
    private final AnalysisProviderManager analysisProviderManager = new AnalysisProviderManager(smoothedLinearModelAnalyser);
    private final ConcreteAnalysisDelegate<Distance> analysisDelegate = new ConcreteAnalysisDelegate<>(Distance.class, 5);
    private final AnalysisDelegateManager analysisDelegateManager = new AnalysisDelegateManager(analysisDelegate);
    private final AnalysisRunner analysisRunner = new AnalysisRunner(analysisProviderManager, analysisDelegateManager, 1200);

    @Override
    public synchronized void reset() {
        didDetect = 0;
        didRead = 0;
        didMeasure = 0;
        didShare = 0;
        didReceive = 0;

        targetIdentifiers.clear();
        payloads.clear();
        targets.clear();

        socialMixingScore.reset();
        smoothedLinearModel.reset();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateCounts();
                updateTargets();
                updateSocialDistance(socialMixingScoreUnit);
            }
        });
    }

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
        targetListAdapter = new TargetListAdapter(this, targets);
        final ListView targetsListView = ((ListView) findViewById(R.id.targets));
        targetsListView.setAdapter(targetListAdapter);
        targetsListView.setOnItemClickListener(this);

        // Test programmatic control of sensor on/off
        final Switch onOffSwitch = findViewById(R.id.sensorOnOffSwitch);
        onOffSwitch.setChecked(false);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sensor.start();
                } else {
                    sensor.stop();
                }
            }
        });

        // Sensor is on by default, unless automated test has been enabled,
        // in which case, sensor is off by default and controlled by test
        // server remote commands.
        final AutomatedTestClient automatedTestClient = AppDelegate.getAppDelegate().automatedTestClient;
        if (null == automatedTestClient) {
            sensor.start();
        } else {
            automatedTestClient.add(this);
        }
    }

    /// REQUIRED : Request application permissions for sensor operation.
    private void requestPermissions() {
        // Check and request permissions
        final List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.BLUETOOTH);
        requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
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

    // Update targets table
    private synchronized void updateTargets() {
        // De-duplicate targets based on short name and last updated at time stamp
        final Map<String,Target> shortNames = new HashMap<>(payloads.size());
        for (Map.Entry<PayloadData,Target> entry : payloads.entrySet()) {
            final String shortName = entry.getKey().shortName();
            final Target target = entry.getValue();
            final Target duplicate = shortNames.get(shortName);
            if (duplicate == null || duplicate.lastUpdatedAt().getTime() < target.lastUpdatedAt().getTime()) {
                shortNames.put(shortName, target);
            }
        }
        // Get target list in alphabetical order
        final List<Target> targetList = new ArrayList<>(shortNames.values());
        Collections.sort(targetList, new Comparator<Target>() {
            @Override
            public int compare(Target t0, Target t1) {
                return t0.payloadData().shortName().compareTo(t1.payloadData().shortName());
            }
        });
        // Get target distance from analysis delegate
        for (final Target target : targetList) {
            if (target.payloadData() == null) {
                continue;
            }
            final SampledID sampledID = new SampledID(target.payloadData());
            final SampleList<Distance> sampleList = analysisDelegate.samples(sampledID);
            target.distance(sampleList.filter(Since.recent(90)).toView().latestValue());
        }
        // Update UI
        ((TextView) findViewById(R.id.detection)).setText("DETECTION (" + targetListAdapter.getCount() + ")");
        targetListAdapter.clear();
        targetListAdapter.addAll(targetList);
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

    private void updateCounts() {
        ((TextView) findViewById(R.id.didDetectCount)).setText(Long.toString(this.didDetect));
        ((TextView) findViewById(R.id.didReadCount)).setText(Long.toString(this.didRead));
        ((TextView) findViewById(R.id.didMeasureCount)).setText(Long.toString(this.didMeasure));
        ((TextView) findViewById(R.id.didShareCount)).setText(Long.toString(this.didShare));
        ((TextView) findViewById(R.id.didReceiveCount)).setText(Long.toString(this.didReceive));
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        Log.d(tag, "app (state=foreground)");
        updateCounts();
        updateTargets();
        updateSocialDistance(socialMixingScoreUnit);
    }

    @Override
    protected void onPause() {
        foreground = false;
        Log.d(tag, "app (state=background)");
        super.onPause();
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull TargetIdentifier didDetect) {
        this.didDetect++;
        if (foreground) {
            final String text = Long.toString(this.didDetect);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = findViewById(R.id.didDetectCount);
                    textView.setText(text);
                }
            });
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        this.didRead++;
        targetIdentifiers.put(fromTarget, didRead);
        Target target = payloads.get(didRead);
        if (target != null) {
            target.didRead(new Date());
        } else {
            payloads.put(didRead, new Target(fromTarget, didRead));
        }
        if (foreground) {
            final String text = Long.toString(this.didRead);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = findViewById(R.id.didReadCount);
                    textView.setText(text);
                    updateTargets();
                }
            });
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull List<PayloadData> didShare, @NonNull TargetIdentifier fromTarget) {
        this.didShare++;
        final Date now = new Date();
        for (PayloadData didRead : didShare) {
            targetIdentifiers.put(fromTarget, didRead);
            Target target = payloads.get(didRead);
            if (target != null) {
                target.didRead(new Date());
            } else {
                payloads.put(didRead, new Target(fromTarget, didRead));
            }
        }
        if (foreground) {
            final String text = Long.toString(this.didShare);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = findViewById(R.id.didShareCount);
                    textView.setText(text);
                    updateTargets();
                }
            });
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget) {
        this.didMeasure++;
        final PayloadData didRead = targetIdentifiers.get(fromTarget);
        if (didRead != null) {
            final Target target = payloads.get(didRead);
            if (target != null) {
                target.targetIdentifier(fromTarget);
                target.proximity(didMeasure);
                // Supply raw RSSI measurements to distance estimation algorithm
                if (didMeasure.unit == ProximityMeasurementUnit.RSSI) {
                    final SampledID sampledID = new SampledID(didRead);
                    analysisRunner.newSample(sampledID, new Sample<>(new RSSI((int) Math.round(didMeasure.value))));
                    // Analysis runner doesn't need to be executed as often as updates
                    // but the overhead is minimal as the demonstration distance analyser
                    // will only perform calculations and offer updates at fixed intervals
                    analysisRunner.run();
                }
            }
        }
        if (foreground) {
            final String text = Long.toString(this.didMeasure);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = findViewById(R.id.didMeasureCount);
                    textView.setText(text);
                    updateTargets();
                    updateSocialDistance(socialMixingScoreUnit);
                }
            });
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull ImmediateSendData didReceive, @NonNull TargetIdentifier fromTarget) {
        this.didReceive++;
        final PayloadData didRead = new PayloadData(didReceive.data.value);
        if (didRead != null) {
            final Target target = payloads.get(didRead);
            if (target != null) {
                targetIdentifiers.put(fromTarget, didRead);
                target.targetIdentifier(fromTarget);
                target.received(didReceive);
            }
        }
        if (foreground) {
            final String text = Long.toString(this.didReceive);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView textView = findViewById(R.id.didReceiveCount);
                    textView.setText(text);
                    updateTargets();
                }
            });
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Location didVisit) {
        // Not used
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget, @NonNull PayloadData withPayload) {
        // High level integration API is not used as the test app is using the low level API to present all the detection events.
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final SensorState didUpdateState) {
        // Sensor state is already presented by the operating system, so not duplicating in the test app.
        if (sensor == SensorType.ARRAY) {
            final Switch onOffSwitch = findViewById(R.id.sensorOnOffSwitch);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onOffSwitch.setChecked(didUpdateState == SensorState.on);
                }
            });
        }
    }

    // MARK:- OnItemClickListener

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final Target target = targetListAdapter.getItem(i);
        final SensorArray sensor = (SensorArray) AppDelegate.getAppDelegate().sensor();
        final PayloadData payloadData = sensor.payloadData();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean result = sensor.immediateSend(payloadData, target.targetIdentifier());
                Log.d(tag, "immediateSend (to=" + target.payloadData().shortName() + ",result=" + result + ")");
            }
        }).start();
    }
}