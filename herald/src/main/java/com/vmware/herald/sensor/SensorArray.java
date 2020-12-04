//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.vmware.herald.BuildConfig;
import com.vmware.herald.sensor.analysis.Sample;
import com.vmware.herald.sensor.ble.ConcreteBLESensor;
import com.vmware.herald.sensor.data.BatteryLog;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.ContactLog;
import com.vmware.herald.sensor.data.DetectionLog;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.StatisticsDidReadLog;
import com.vmware.herald.sensor.data.StatisticsLog;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.PseudoDeviceAddress;
import com.vmware.herald.sensor.datatype.RandomSource;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.service.ForegroundService;

import java.lang.annotation.Native;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private final Context context;
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private final List<Sensor> sensorArray = new ArrayList<>();

    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";

    private final ConcreteBLESensor concreteBleSensor;

    public SensorArray(final Context context, PayloadDataSupplier payloadDataSupplier) {
        this.context = context;
        // Ensure logger has been initialised (should have happened in AppDelegate already)
        ConcreteSensorLogger.context(context);
        logger.debug("init");

        // Start foreground service to enable background scan
        final Intent intent = new Intent(context, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Define sensor array
        concreteBleSensor = new ConcreteBLESensor(context, payloadDataSupplier);
        sensorArray.add(concreteBleSensor);

        // Loggers
        payloadData = payloadDataSupplier.payload(new PayloadTimestamp());
		if (BuildConfig.DEBUG) {
	        add(new ContactLog(context, "contacts.csv"));
	        add(new StatisticsLog(context, "statistics.csv", payloadData));
	        add(new StatisticsDidReadLog(context, "statistics_didRead.csv", payloadData));
	        add(new DetectionLog(context,"detection.csv", payloadData));
	        new BatteryLog(context, "battery.csv");
		}
        logger.info("DEVICE (payload={},description={})", payloadData.shortName(), deviceDescription);

		// Experimentation
        try {
            final Sample sample = new Sample();
            for (int i=0; i<Integer.MAX_VALUE; i++) {
                final long t0 = System.nanoTime();
                final byte[] b = new byte[55];
                for (int j=100000; j-->0;) {
                    PseudoDeviceAddress.getNISTSecureRandomLong();
                }
                final long t1 = System.nanoTime();
                sample.add(t1 - t0);
                logger.debug("security, secureRandom test {}, {}", (t1-t0), sample);
            }
        } catch (Throwable e) {
            logger.fault("security, secureRandom unavailable", e);
        }
    }

    private void randomSourcePseudoDeviceAddressTest(final Context context) {
        logger.debug("randomSourcePseudoDeviceAddressTest, start");
        final TextFile logFile = new TextFile(context, "secureRandom.csv");
        logFile.write("count,mean,sd,min,max");
        long t0, t1, tDelta = 0;
        final Sample samples = new Sample();
        while (tDelta < 500000000) {
            t0 = System.nanoTime();
            PseudoDeviceAddress.getNISTSecureRandomLong();
            t1 = System.nanoTime();
            tDelta = t1 - t0;
            samples.add(tDelta);
            if (samples.count() % 100000 == 0) {
                logger.debug("randomSourcePseudoDeviceAddressTest, progress (statistics={})", samples);
                logFile.write(samples.count()+","+samples.mean()+","+samples.standardDeviation()+","+samples.min()+","+samples.max());
            }
        }
        logger.debug("randomSourcePseudoDeviceAddressTest, end (statistics={})", samples);
    }


    private void randomSourceTest(final Context context, final RandomSource.Method method) {
        logger.debug("randomSourceTest, start (method={})", method.name());
        final RandomSource randomSource = new RandomSource(method);
        final TextFile logFile = new TextFile(context, "random-" + method.name() + ".csv");
        logFile.write("elapsed");
        long t0, t1, tDelta = 0;
        final Sample samples = new Sample();
        while (tDelta < 500000000 && samples.count() < 1000000) {
            t0 = System.nanoTime();
            randomSource.nextLong();
            t1 = System.nanoTime();
            tDelta = t1 - t0;
            samples.add(tDelta);
            logFile.write(Long.toString(tDelta));
            if (samples.count() % 10000 == 0) {
                logger.debug("randomSourceTest, progress (method={},statistics={})", method.name(), samples);
            }
        }
        logger.debug("randomSourceTest, end (method={},statistics={})", method.name(), samples);
    }

    /// Immediate send data.
    public boolean immediateSend(Data data, TargetIdentifier targetIdentifier) {
        return concreteBleSensor.immediateSend(data,targetIdentifier);
    }

    public final PayloadData payloadData() {
        return payloadData;
    }

    @Override
    public void add(final SensorDelegate delegate) {
        for (Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        logger.debug("start");
        for (Sensor sensor : sensorArray) {
            sensor.start();
        }
    }

    @Override
    public void stop() {
        logger.debug("stop");
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
    }
}
