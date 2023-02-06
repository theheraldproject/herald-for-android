//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.BluetoothState;
import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadSharingData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.PseudoDeviceAddress;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.SignalCharacteristicData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.datatype.Triple;
import io.heraldprox.herald.sensor.PayloadDataSupplier;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.protocol.GPDMPLayer1BluetoothLEIncoming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;

public class ConcreteBLETransmitter implements BLETransmitter, BluetoothStateManagerDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLETransmitter");
    private final static long advertOffDurationMillis = TimeInterval.seconds(2).millis();
    private final static long advertOnDurationMillis = TimeInterval.seconds(5).millis();
    @NonNull
    private final Context context;
    @NonNull
    private final BluetoothStateManager bluetoothStateManager;
    @NonNull
    private final PayloadDataSupplier payloadDataSupplier;
    @NonNull
    private final BLEDatabase database;
    private final GPDMPLayer1BluetoothLEIncoming gpdmpIncoming;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final AtomicBoolean transmitterEnabled = new AtomicBoolean(false);

    // Referenced by startAdvert and stopExistingGattServer ONLY
    @Nullable
    private BluetoothGattServer bluetoothGattServer = null;

    @NonNull
    private AdvertLoopTask myLoopTask = new AdvertLoopTask();

    @NonNull
    private PseudoDeviceAddress pseudoDeviceAddress = new PseudoDeviceAddress();

    @NonNull
    private Date lastInteraction = new Date();

    /**
     * Transmitter starts automatically when Bluetooth is enabled.
     *
     * @param context The Herald execution environment Context
     * @param bluetoothStateManager To determine whether Bluetooth is enabled
     * @param timer Used to register a need for periodic events to occur
     * @param payloadDataSupplier Source of the payload to transmit
     * @param database BLE Device database to locate nearby device information
     * @param gpdmpIncoming Where to pass raw GPDMP data onto for processing
     */
    public ConcreteBLETransmitter(@NonNull final Context context, @NonNull final BluetoothStateManager bluetoothStateManager, @NonNull final BLETimer timer, @NonNull final PayloadDataSupplier payloadDataSupplier, @NonNull final BLEDatabase database, @NonNull final GPDMPLayer1BluetoothLEIncoming gpdmpIncoming) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.payloadDataSupplier = payloadDataSupplier;
        this.database = database;
        this.gpdmpIncoming = gpdmpIncoming;
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
        timer.add(myLoopTask);
    }

    @Override
    public void add(@NonNull final SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        if (transmitterEnabled.compareAndSet(false, true)) {
            logger.debug("start, transmitter enabled to follow bluetooth state");
        } else {
            logger.fault("start, transmitter already enabled to follow bluetooth state");
        }
        // TODO do we need to do something here in case Sensor.init() is called? E.g. stop and restart?
        // Override state to starting anyway to ensure we try to restart
        myLoopTask.doStart();
        // Immediately calling a healthcheck can cause advertising to be stopped before it has had chance to start
//        myLoopTask.healthCheck();
    }

    @Override
    public void stop() {
        if (transmitterEnabled.compareAndSet(true, false)) {
            logger.debug("stop, transmitter disabled");
        } else {
            logger.fault("stop, transmitter already disabled");
        }
        // Clean out this reference (forced)
        bluetoothGattServer = null;
    }


    // MARK:- Advert loop

    private enum AdvertLoopState {
        starting, started, stopping, stopped
    }

    /**
     * Get Bluetooth LE advertiser
     * @return Advertiser, or null if unsupported or unavailable
     */
    @Nullable
    private BluetoothLeAdvertiser bluetoothLeAdvertiser() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == bluetoothAdapter) {
            logger.debug("bluetoothLeAdvertiser, no Bluetooth Adapter available");
            return null;
        }
        boolean supported = bluetoothAdapter.isMultipleAdvertisementSupported();
        try {
            final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (null == bluetoothLeAdvertiser) {
                logger.debug("bluetoothLeAdvertiser, no LE advertiser present (multiSupported={}, exception=no)", supported);
                return null;
            }
            // log this, as this will allow us to identify handsets with a different API implementation
            // Disabling this log message as it is called once every second and no longer relevant as
            // result of isMultipleAdvertisementSupported() is not used in the logic
            // logger.debug("bluetoothLeAdvertiser, LE advertiser present (multiSupported={})", supported);
            return bluetoothLeAdvertiser;
        } catch (Exception e) {
            // log it, as this will allow us to identify handsets with the expected API implementation (from Android API source code)
            logger.debug("bluetoothLeAdvertiser, no LE advertiser present (multiSupported={}, exception={})", supported, e.getMessage());
            return null;
        }
    }

    private long timeSinceLastInteraction(final long now) {
        return now - lastInteraction.getTime();
    }

    private class AdvertLoopTask implements BLETimerDelegate {
        @NonNull
        private AdvertLoopState advertLoopState = AdvertLoopState.stopped;
        private long lastStateChangeAt = System.currentTimeMillis();
        @Nullable
        private BluetoothGattServer bluetoothGattServer;
        @Nullable
        private AdvertiseCallback advertiseCallback;

        private void state(final long now, @NonNull final AdvertLoopState state) {
            final long elapsed = now - lastStateChangeAt;
            logger.debug("advertLoopTask, state change (from={},to={},elapsed={}ms)", advertLoopState, state, elapsed);
            this.advertLoopState = state;
            lastStateChangeAt = now;
        }

        private void healthCheck() {
//            logger.debug("advertLoopTask, healthCheck");
            // Success should be silent, so not printing this general message unless we intervene
            final long now = System.currentTimeMillis();

            if (!transmitterEnabled.get() || !isSupported() || bluetoothStateManager.state() == BluetoothState.poweredOff) {
                if (advertLoopState != AdvertLoopState.stopped) {
                    logger.debug("advertLoopTask, healthCheck, stopping advert following bluetooth state change (isSupported={},bluetoothPowerOff={})", isSupported(), bluetoothStateManager.state() == BluetoothState.poweredOff);
                    doStop(now,true);
                } else {
                    logger.debug("advertLoopTask, healthCheck, transmitter is disabled, not supported, or Bluetooth is powered off");
                }
                return;
            }

            if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
//                logger.debug("advertLoopTask, healthCheck, Bluetooth powered on");
                // Success should be silent, so not printing this general message unless we intervene
                // Check if last interaction was longer than 20 minutes (slightly longer than BLE rotation period)
                final long interactionPeriod = timeSinceLastInteraction(now);
                if (interactionPeriod >= TimeInterval.minutes(20).millis()) {
                    if (advertLoopState != AdvertLoopState.stopping && advertLoopState != AdvertLoopState.stopped) {
                        logger.debug("advertLoopTask, healthCheck, Not received any interaction over our Bluetooth Herald Service for too long ({}ms). Restarting GATT server", interactionPeriod);
                        doStop(now, true);
                        return; // this is the only health check that prevents the other health checks from running
                    }
                }
                // Otherwise, check if last advert off was too long
                if (BLESensorConfiguration.manuallyEnforceAdvertGaps) {
                    final long period = timeSincelastStateChange(now);
                    switch (advertLoopState) {
                        case started:
                            if (period >= advertOnDurationMillis) {
                                logger.debug("advertLoopTask, healthCheck, manuallyEnforceAdvertGaps, advert on for too long ({}ms). In started state. Soft stopping advert.",period);
                                doStop(now, false);
                            }
                            break;
                        case stopped:
                            if (period >= advertOffDurationMillis) {
                                logger.debug("advertLoopTask, healthCheck, manuallyEnforceAdvertGaps, advert off for too long ({}ms). In Stopped state. Starting advert.",period);
                                doStart();
                            }
                            break;
                    }
                } else {
                    final long period = timeSincelastStateChange(now);
                    if (advertLoopState == AdvertLoopState.stopped && period > advertOffDurationMillis) {
                        logger.debug("advertLoopTask, healthCheck, isStopped check, advert off for too long ({}ms). In Stopped state. Starting advert.",period);
                        doStart();
                    }
                }

                // Check if we're actually advertising the service over GATT
                if (null != bluetoothGattServer &&
                        (bluetoothGattServer.getServices().size() == 0 ||
                                (BLESensorConfiguration.customServiceAdvertisingEnabled &&
                                        (null == bluetoothGattServer.getService(BLESensorConfiguration.customServiceUUID) ||
                                        null == bluetoothGattServer.getService(BLESensorConfiguration.customServiceUUID).getCharacteristics() ||
                                        0 == bluetoothGattServer.getService(BLESensorConfiguration.customServiceUUID).getCharacteristics().size())
                                ) ||
                                (!BLESensorConfiguration.customServiceAdvertisingEnabled &&
                                        (null == bluetoothGattServer.getService(BLESensorConfiguration.linuxFoundationServiceUUID) ||
                                        null == bluetoothGattServer.getService(BLESensorConfiguration.linuxFoundationServiceUUID).getCharacteristics() ||
                                        0 == bluetoothGattServer.getService(BLESensorConfiguration.linuxFoundationServiceUUID).getCharacteristics().size())
                                )
                        )
                ) {
                    logger.debug("advertLoopTask, healthCheck, shows GATT server is not advertising Herald. Setting GATT Service");
                    try {
                        setGattService(logger, context, bluetoothGattServer);
                    } catch (Throwable e) {
                        logger.fault("advertLoopTask, healthCheck, failed to set GATT service", e);
                        myLoopTask.disableGattServer();
                    }
                    // Return because this is async
                    return;
                }
            } else {
//                logger.fault("advertLoopTask, healthCheck, Bluetooth powered off");
                // Succeeding silently so we don't fill log files
            }
        }

        private void doStart() {
            final long now = System.currentTimeMillis();
            final long period = timeSincelastStateChange(now);
            logger.debug("advertLoopTask, start advert (stop={}ms)", period);
            final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothLeAdvertiser();
            if (null == bluetoothLeAdvertiser) {
                logger.fault("advertLoopTask, start advert denied, Bluetooth LE advertiser unavailable");
                return;
            }
            if (null != advertiseCallback) {
                logger.fault("advertLoopTask, advertiseCallback is not null, so we will not start advertising.");
//                doStop(now,false);
                return;
            }
            if (AdvertLoopState.starting == advertLoopState) {
                if (period >= advertOffDurationMillis) {
                    logger.fault("advertLoopTask, advert state has been in starting state for {}ms, so forcing start.",period);
                } else {
                    logger.fault("advertLoopTask, advert state is already in 'starting' - giving time to start, so returning for now.");
                    return;
                }
            }
            logger.debug("advertLoopTask, starting...");
            state(now, AdvertLoopState.starting);
            startAdvert(bluetoothLeAdvertiser, new Callback<Triple<Boolean, AdvertiseCallback, BluetoothGattServer>>() {
                @Override
                public void accept(@NonNull Triple<Boolean, AdvertiseCallback, BluetoothGattServer> value) {
                    advertiseCallback = value.b;
                    bluetoothGattServer = value.c;
                    state(now, value.a != null && value.a ? AdvertLoopState.started : AdvertLoopState.stopped);
                }
            });
        }

        /**
         * Adding in v2.1 to enable forcing of stop to ensure only one Herald advert is advertising
         * @param now The time to execute the stop for (useful for testing)
         */
        private void doStop(final long now,final boolean forceGattRenewal) {
            logger.debug("advertLoopTask, doStop called. Forcing GATT renewal?: {}", forceGattRenewal);
            state(now, AdvertLoopState.stopping);
            lastInteraction = new Date(); // Reset this to prevent continuous GATT server refreshes due to inactivity
            stopAdvert(bluetoothLeAdvertiser(), advertiseCallback, bluetoothGattServer, new Callback<Boolean>() {
                @Override
                public void accept(@NonNull final Boolean value) {
                    advertiseCallback = null;
                    if (forceGattRenewal) {
                        // Refresh pseudo mac too at this point
                        pseudoDeviceAddress = new PseudoDeviceAddress(System.currentTimeMillis());
                        disableGattServer();
                    }
                    state(now, AdvertLoopState.stopped);
                    logger.debug("advertLoopTask, stop advert (advert={}ms)", timeSincelastStateChange(now));
                }
            });
        }

        private long timeSincelastStateChange(final long now) {
            return now - lastStateChangeAt;
        }

        private void disableGattServer() {
            if (null != bluetoothGattServer) {
                logger.debug("disableGattServer called, gattServer is not null");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    logger.fault("disableGattServer, no BLUETOOTH_CONNECT permission to clear and stop GATT server");
                } else {
                    try {
//                        bluetoothGattServer.clearServices(); // Clears non-Herald GATT services (E.g. fetch device name, model, etc.)
                        UUID ourAdvertisedId = BLESensorConfiguration.linuxFoundationServiceUUID;
                        if (BLESensorConfiguration.customServiceAdvertisingEnabled) {
                            ourAdvertisedId = BLESensorConfiguration.customServiceUUID;
                        }
                        BluetoothGattService ourService = bluetoothGattServer.getService(ourAdvertisedId);
                        if (null != ourService) {
                            // Service is already advertised, so remove it
                            logger.debug("disableGattServer clearing single service for Herald (NOT calling clearServices())");
                            bluetoothGattServer.removeService(ourService);
                        }
                        bluetoothGattServer.close();
                        logger.debug("disableGattServer, GATT server stopped successfully");
                    } catch (Throwable e) {
                        logger.fault("disableGattServer found existing GATT server but failed to stop the server", e);
                    }
                }
                bluetoothGattServer = null;
            } else {
                logger.debug("disableGattServer called, gattServer is null");
            }
        }

        @Override
        public void bleTimer(final long now) {
            logger.debug("advertLoopTask, bleTimer function called");
            healthCheck(); // Check to see if (for example) we need to force stopping
            switch (advertLoopState) {
                case stopped: {
//                    healthCheck();
                    break;
                }
                // Since v2.1, don't try to be cleverer than Android and hard fix the advert period to 15 minutes
//                case started: {
//                    final long period = timeSincelastStateChange(now);
//                    if (period >= BLESensorConfiguration.advertRefreshTimeInterval.millis()) {
//                        logger.debug("advertLoopTask, bleTimer, stop advert (advert={}ms)", period);
//                        final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothLeAdvertiser();
//                        if (null == bluetoothLeAdvertiser) {
//                            logger.fault("advertLoopTask, bleTimer, stop advert denied, Bluetooth LE advertiser unavailable");
//                            return;
//                        }
//                        // Refresh pseudo mac address to ensure new advert period has a new address
//                        pseudoDeviceAddress = new PseudoDeviceAddress(System.currentTimeMillis());
//                        doStop(now,false);
////                        state(now, AdvertLoopState.stopping);
////                        stopAdvert(bluetoothLeAdvertiser, advertiseCallback, bluetoothGattServer, new Callback<Boolean>() {
////                            @Override
////                            public void accept(@NonNull final Boolean value) {
////                                advertiseCallback = null;
//////                                disableGattServer();
////                                state(now, AdvertLoopState.stopped);
////                                // New in v2.1 - immediately restart so we don't drop out of the active state
//////                                doStart();
////                            }
////                        });
//                    }
//                    break;
//                }
            }
        }
    }

    // MARK:- Start and stop advert

    private void startAdvert(@NonNull final BluetoothLeAdvertiser bluetoothLeAdvertiser, @NonNull final Callback<Triple<Boolean, AdvertiseCallback, BluetoothGattServer>> callback) {
        logger.debug("startAdvert");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        logger.fault("startAdvert, operationQueue, no BLUETOOTH_CONNECT permission");
                        return;
                    }
                }
                // Stop existing advert if there is already a proxy reference.
                // This should never happen because only the AdvertLoopTask calls
                // startAdvert and it should only call startAdvert after stopAdvert
                // has been called previously. Logging this condition to verify if
                // this condition can ever occur to support investigation.
                if (null != bluetoothGattServer) {
                    logger.debug("startAdvert found existing GATT server - reusing");
//                    myLoopTask.disableGattServer();
                }
                if (null == bluetoothGattServer) {
                    // Start new GATT server
                    try {
                        logger.debug("startAdvert creating new GATT server instance");
                        bluetoothGattServer = startGattServer(logger, context, payloadDataSupplier, database, myLoopTask);
                    } catch (Throwable e) {
                        logger.fault("startAdvert failed to start GATT server", e);
                        result = false;
                    }
//                    result = false;
                }
                if (!result && (bluetoothGattServer.getServices().size() == 0 ||
                        (BLESensorConfiguration.customServiceAdvertisingEnabled &&
                                (null == bluetoothGattServer.getService(BLESensorConfiguration.customServiceUUID) ||
                                null == bluetoothGattServer.getService(BLESensorConfiguration.customServiceUUID).getCharacteristics() ||
                                0 == bluetoothGattServer.getService(BLESensorConfiguration.customServiceUUID).getCharacteristics().size())
                        ) ||
                        (!BLESensorConfiguration.customServiceAdvertisingEnabled &&
                                (null == bluetoothGattServer.getService(BLESensorConfiguration.linuxFoundationServiceUUID) ||
                                null == bluetoothGattServer.getService(BLESensorConfiguration.linuxFoundationServiceUUID).getCharacteristics() ||
                                0 == bluetoothGattServer.getService(BLESensorConfiguration.linuxFoundationServiceUUID).getCharacteristics().size())
                )
                )) {
                    logger.debug("startAdvert shows GATT server is not advertising Herald. Setting GATT Service");
                    try {
                        setGattService(logger, context, bluetoothGattServer);
                    } catch (Throwable e) {
                        logger.fault("startAdvert failed to set GATT service", e);
                        myLoopTask.disableGattServer();
                        result = false;
                    }
                }
                if (!result) {
                    logger.fault("startAdvert failed");
                    callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(false, null, null));
                    return;
                }
                try {
                    final BluetoothGattServer bluetoothGattServerConfirmed = bluetoothGattServer;
                    final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
                        @Override
                        public void onStartSuccess(final AdvertiseSettings settingsInEffect) {
                            logger.debug("startAdvert successful");
                            callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(true, this, bluetoothGattServerConfirmed));
                        }

                        @Override
                        public void onStartFailure(final int errorCode) {
                            logger.fault("startAdvert failed (errorCode={})", onStartFailureErrorCodeToString(errorCode));
                            callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(false, this, bluetoothGattServerConfirmed));
                        }
                    };
                    startAdvertising(bluetoothLeAdvertiser, advertiseCallback);
                } catch (Throwable e) {
                    logger.fault("startAdvert failed");
                    callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(false, null, null));
                }
            }
        });
    }

    private void stopAdvert(@Nullable final BluetoothLeAdvertiser bluetoothLeAdvertiser, @Nullable final AdvertiseCallback advertiseCallback, @Nullable final BluetoothGattServer bluetoothGattServer, @NonNull final Callback<Boolean> callback) {
        logger.debug("stopAdvert");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        logger.fault("stopAdvert, operationQueue, no BLUETOOTH_CONNECT permission");
                        return;
                    }
                }
                try {
                    if (null != bluetoothLeAdvertiser && null != advertiseCallback) {
                        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                    }
                } catch (Throwable e) {
                    logger.fault("stopAdvert failed to stop advertising", e);
                    result = false;
                }
                try {
                    myLoopTask.disableGattServer();
                } catch (Throwable e) {
                    logger.fault("stopAdvert failed to stop GATT server", e);
                    result = false;
                }
                if (result) {
                    logger.debug("stopAdvert successful");
                } else {
                    logger.fault("stopAdvert failed");
                }
                callback.accept(result);
            }
        });
    }


    @NonNull
    @Override
    public PayloadData payloadData() {
        return payloadDataSupplier.payload(new PayloadTimestamp(new Date()), null);
    }

    @Override
    public boolean isSupported() {
        return null != bluetoothLeAdvertiser();
    }

    @Override
    public void bluetoothStateManager(@NonNull final BluetoothState didUpdateState) {
        logger.debug("didUpdateState (state={},transmitterEnabled={})", didUpdateState, transmitterEnabled.get());
        if (BluetoothState.poweredOff == didUpdateState) {
            // Clean out this reference (forced)
            bluetoothGattServer = null;
        }
        myLoopTask.healthCheck();
    }

    private void startAdvertising(@NonNull final BluetoothLeAdvertiser bluetoothLeAdvertiser, @NonNull final AdvertiseCallback advertiseCallback) {
        logger.debug("startAdvertising");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("startAdvertising, no BLUETOOTH_ADVERTISE permission");
                return;
            }
        }

        final AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();

        // Custom service logic added since v2.2 February 2023
        UUID advertServiceUUID = null;
        int advertManufacturerId = 0;
        if (BLESensorConfiguration.standardHeraldServiceAdvertisingEnabled) {
            advertServiceUUID = BLESensorConfiguration.linuxFoundationServiceUUID;
            advertManufacturerId = BLESensorConfiguration.linuxFoundationManufacturerIdForSensor;
        }
        if (BLESensorConfiguration.customServiceAdvertisingEnabled) {
            advertServiceUUID = BLESensorConfiguration.customServiceUUID;
            advertManufacturerId = BLESensorConfiguration.customManufacturerIdForSensor;
        }
        if (null != advertServiceUUID) { // We may not want to advertise Herald or Custom services - just scan.
            // This logic added since v2.1.0-beta4.
            // See BLESensorConfiguration.pseudoDeviceAddressEnabled for details.
            if (BLESensorConfiguration.pseudoDeviceAddressEnabled) {
                final AdvertiseData data = new AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .addServiceUuid(new ParcelUuid(advertServiceUUID))
                        .addManufacturerData(advertManufacturerId, pseudoDeviceAddress.data)
                        .build();
                bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
                logger.debug("startAdvertising successful (pseudoDeviceAddress={},settings={})", pseudoDeviceAddress, settings);
            } else {
                final AdvertiseData data = new AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .addServiceUuid(new ParcelUuid(advertServiceUUID))
                        .build();
                bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
                logger.debug("startAdvertising successful (pseudoDeviceAddress=nil,settings={})", settings);
            }
        }
    }

    @Nullable
    private BluetoothGattServer startGattServer(@NonNull final SensorLogger logger, @NonNull final Context context, @NonNull final PayloadDataSupplier payloadDataSupplier, @NonNull final BLEDatabase database, @NonNull final AdvertLoopTask myLoopTask) {
        logger.debug("startGattServer");
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (null == bluetoothManager) {
            logger.fault("Bluetooth unsupported");
            return null;
        }
        // Data = rssi (4 bytes int) + payload (remaining bytes)
        final AtomicReference<BluetoothGattServer> server = new AtomicReference<>(null);
        final BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
            private final Map<String, PayloadData> onCharacteristicReadPayloadData = new ConcurrentHashMap<>();
            private final Map<String, byte[]> onCharacteristicWriteSignalData = new ConcurrentHashMap<>();

            @Nullable
            private PayloadData onCharacteristicReadPayloadData(@NonNull final BluetoothDevice bluetoothDevice) {
                logger.debug("BluetoothGattServerCallback, onCharacteristicReadPayloadData");
                final BLEDevice device = database.device(bluetoothDevice);
                final String key = bluetoothDevice.getAddress();
                if (onCharacteristicReadPayloadData.containsKey(key)) {
                    return onCharacteristicReadPayloadData.get(key);
                }
                final PayloadData payloadData = payloadDataSupplier.payload(new PayloadTimestamp(), device);
                onCharacteristicReadPayloadData.put(key, payloadData);
                return payloadData;
            }

            @NonNull
            private byte[] onCharacteristicWriteSignalData(@NonNull final BluetoothDevice device, @Nullable final byte[] value) {
                logger.debug("BluetoothGattServerCallback, onCharacteristicWriteSignalData");
                final String key = device.getAddress();
                byte[] partialData = onCharacteristicWriteSignalData.get(key);
                if (null == partialData) {
                    partialData = new byte[0];
                }
                final byte[] data = new byte[partialData.length + (null == value ? 0 : value.length)];
                System.arraycopy(partialData, 0, data, 0, partialData.length);
                if (value != null) {
                    System.arraycopy(value, 0, data, partialData.length, value.length);
                }
                onCharacteristicWriteSignalData.put(key, data);
                return data;
            }

            private void removeData(@NonNull final BluetoothDevice device) {
                logger.debug("BluetoothGattServerCallback, removeData");
                final String deviceAddress = device.getAddress();
                for (final String deviceRequestId : new ArrayList<>(onCharacteristicReadPayloadData.keySet())) {
                    if (deviceRequestId.startsWith(deviceAddress)) {
                        onCharacteristicReadPayloadData.remove(deviceRequestId);
                    }
                }
                for (final String deviceRequestId : new ArrayList<>(onCharacteristicWriteSignalData.keySet())) {
                    if (deviceRequestId.startsWith(deviceAddress)) {
                        onCharacteristicWriteSignalData.remove(deviceRequestId);
                    }
                }
            }

            @Override
            public void onServiceAdded(int status, @NonNull BluetoothGattService service) {
                logger.debug("BluetoothGattServerCallback, onServiceAdded (successfully={},uuid={}",
                        (BluetoothGatt.GATT_SUCCESS == status),
                        service.getUuid().toString()
                );
            }

            @Override
            public void onConnectionStateChange(@NonNull final BluetoothDevice bluetoothDevice, final int status, final int newState) {
                lastInteraction = new Date();
                final BLEDevice device = database.device(bluetoothDevice);
                logger.debug("BluetoothGattServerCallback, onConnectionStateChange (device={},status={},newState={})",
                        device, status, onConnectionStateChangeStatusToString(newState));
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    device.state(BLEDeviceState.connected);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    device.state(BLEDeviceState.disconnected);
                    removeData(bluetoothDevice);
                }
                myLoopTask.healthCheck();
            }


            @Override
            public void onCharacteristicWriteRequest(@NonNull final BluetoothDevice device, final int requestId, @NonNull final BluetoothGattCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, @Nullable final byte[] value) {
                lastInteraction = new Date();
                final BLEDevice targetDevice = database.device(device);
                final TargetIdentifier targetIdentifier = targetDevice.identifier;
                logger.debug("BluetoothGattServerCallback, didReceiveWrite (central={},requestId={},offset={},characteristic={},value={})",
                        targetDevice, requestId, offset,
                        (characteristic.getUuid().equals(BLESensorConfiguration.androidSignalCharacteristicUUID) ? "signal" : "unknown"),
                        (null != value ? value.length : "null")
                );
                if (characteristic.getUuid() != BLESensorConfiguration.androidSignalCharacteristicUUID) {
                    if (responseNeeded) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                logger.fault("BluetoothGattServerCallback, onCharacteristicWriteRequest, no BLUETOOTH_CONNECT permission");
                                return;
                            }
                        }
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, value);
                    }
                    return;
                }
                final Data data = new Data(onCharacteristicWriteSignalData(device, value));
				if (characteristic.getUuid().equals(BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID)) {
                    //noinspection ConstantConditions
                    if (null == data.value) {
				        return;
                    }
                    final PayloadData payloadData = new PayloadData(data.value);
                    logger.debug("BluetoothGattServerCallback, didReceiveWrite (dataType=payload,central={},payload={})", targetDevice, payloadData);
                    targetDevice.payloadData(payloadData);
                    onCharacteristicWriteSignalData.remove(device.getAddress());
                    if (responseNeeded) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                logger.fault("BluetoothGattServerCallback, onCharacteristicWriteRequest, no BLUETOOTH_CONNECT permission");
                                return;
                            }
                        }
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    }
                    return;
                }
                switch (SignalCharacteristicData.detect(data)) {
                    case rssi: {
                        final RSSI rssi = SignalCharacteristicData.decodeWriteRSSI(data);
                        if (null == rssi) {
                            logger.fault("BluetoothGattServerCallback, didReceiveWrite, invalid request (central={},action=writeRSSI)", targetDevice);
                            break;
                        }
                        logger.debug("BluetoothGattServerCallback, didReceiveWrite (dataType=rssi,central={},rssi={})", targetDevice, rssi);
                        // Only receive-only Android devices write RSSI
                        targetDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                        targetDevice.receiveOnly(true);
                        targetDevice.rssi(rssi);
                        break;
                    }
                    case payload: {
                        final PayloadData payloadData = SignalCharacteristicData.decodeWritePayload(data);
                        if (null == payloadData) {
                            // Fragmented payload data may be incomplete
                            break;
                        }
                        logger.debug("BluetoothGattServerCallback, didReceiveWrite (dataType=payload,central={},payload={})", targetDevice, payloadData);
                        // Only receive-only Android devices write payload
                        targetDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                        targetDevice.receiveOnly(true);
                        targetDevice.payloadData(payloadData);
                        onCharacteristicWriteSignalData.remove(device.getAddress());
                        break;
                    }
                    case payloadSharing: {
                        final PayloadSharingData payloadSharingData = SignalCharacteristicData.decodeWritePayloadSharing(data);
                        if (null == payloadSharingData) {
                            // Fragmented payload sharing data may be incomplete
                            break;
                        }
                        final List<PayloadData> didSharePayloadData = payloadDataSupplier.payload(payloadSharingData.data);
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, didSharePayloadData, targetIdentifier);
                        }
                        // Only Android devices write payload sharing
                        targetDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                        targetDevice.rssi(payloadSharingData.rssi);
                        logger.debug("BluetoothGattServerCallback, didReceiveWrite (dataType=payloadSharing,central={},payloadSharingData={})", targetDevice, didSharePayloadData);
                        for (final PayloadData payloadData : didSharePayloadData) {
                            final BLEDevice sharedDevice = database.device(payloadData);
                            sharedDevice.operatingSystem(BLEDeviceOperatingSystem.shared);
                            sharedDevice.rssi(payloadSharingData.rssi);
                        }
                        break;
                    }
                    case immediateSend: {
                        final ImmediateSendData immediateSendData = SignalCharacteristicData.decodeImmediateSend(data);
                        if (null == immediateSendData) {
                            // Fragmented immediate send data may be incomplete
                            break;
                        }

                        // Immediate Send disabled for now so I can test GPDMP
                        // for (SensorDelegate delegate : delegates) {
                        //     delegate.sensor(SensorType.BLE, immediateSendData, targetIdentifier);
                        // }
                        // logger.debug("didReceiveWrite (dataType=immediateSend,central={},immediateSendData={})", targetDevice, immediateSendData.data);

                        break;
                    }
                }
                if (responseNeeded) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            logger.fault("BluetoothGattServerCallback, onCharacteristicWriteRequest, no BLUETOOTH_CONNECT permission");
                            return;
                        }
                    }
                    server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }

            @Override
            public void onCharacteristicReadRequest(@NonNull final BluetoothDevice device, final int requestId, final int offset, @NonNull final BluetoothGattCharacteristic characteristic) {
                lastInteraction = new Date();
                final BLEDevice targetDevice = database.device(device);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        logger.fault("BluetoothGattServerCallback, onCharacteristicReadRequest, no BLUETOOTH_CONNECT permission");
                        return;
                    }
                }
                if (characteristic.getUuid() == BLESensorConfiguration.payloadCharacteristicUUID || characteristic.getUuid().equals(BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID)) {
                    final PayloadData payloadData = onCharacteristicReadPayloadData(device);
                    if (payloadData != null && offset > payloadData.value.length) {
                        logger.fault("BluetoothGattServerCallback, didReceiveRead, invalid offset (central={},requestId={},offset={},characteristic=payload,dataLength={})", targetDevice, requestId, offset, payloadData.value.length);
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                    } else if (payloadData != null) {
                        final byte[] value = Arrays.copyOfRange(payloadData.value, offset, payloadData.value.length);
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                        logger.debug("BluetoothGattServerCallback, didReceiveRead (central={},requestId={},offset={},characteristic=payload)", targetDevice, requestId, offset);
                    }
                } else {
                    logger.fault("BluetoothGattServerCallback, didReceiveRead (central={},characteristic=unknown)", targetDevice);
                    server.get().sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null);
                }
            }
        };
        server.set(bluetoothManager.openGattServer(context, callback));
        logger.debug("startGattServer successful");
        return server.get();
    }

    private static void setGattService(@NonNull final SensorLogger logger, @NonNull final Context context, @Nullable final BluetoothGattServer bluetoothGattServer) {
        logger.debug("setGattService");
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (null == bluetoothManager) {
            logger.fault("Bluetooth unsupported");
            return;
        }
        if (null == bluetoothGattServer) {
            logger.fault("Bluetooth LE advertiser unsupported");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("setGattService, no BLUETOOTH_CONNECT permission");
                return;
            }
        }
        for (final BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            bluetoothGattServer.cancelConnection(device);
        }
        for (final BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
            bluetoothGattServer.cancelConnection(device);
        }
        // Print all services out before removing them - useful for device debug information
        List<BluetoothGattService> servicesList = bluetoothGattServer.getServices();
        for (final BluetoothGattService svc : servicesList) {
            logger.debug("setGattService, serviceList, Currently advertising service: {}",svc.getUuid());
            for (final BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                logger.debug("setGattService, serviceList,  - with characteristic: {}",chr.getUuid());
            }
        }
        // Only modify our own service (This should be the only service on GATT we can see anyway)
//        bluetoothGattServer.clearServices();
        // Custom service UUID added since v2.2 February 2023
        UUID ourAdvertisedId = BLESensorConfiguration.linuxFoundationServiceUUID;
        if (BLESensorConfiguration.customServiceAdvertisingEnabled) {
            ourAdvertisedId = BLESensorConfiguration.customServiceUUID;
        }
        BluetoothGattService ourService = bluetoothGattServer.getService(ourAdvertisedId);
        if (null != ourService) {
            // Service is already advertised, so remove it
            logger.debug("setGattService clearing single service for Herald (NOT calling clearServices())");
            boolean success = bluetoothGattServer.removeService(ourService);
            logger.debug("setGattService clearing single service result (successful={})",success);
        }

        // Logic check - ensure there are now no Gatt Services
        List<BluetoothGattService> services = bluetoothGattServer.getServices();
        for (final BluetoothGattService svc : services) {
            logger.debug("setGattService is advertising (non-Herald) service (service={})",svc.getUuid());
        }

        final BluetoothGattService service = new BluetoothGattService(ourAdvertisedId, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        final BluetoothGattCharacteristic signalCharacteristic = new BluetoothGattCharacteristic(
                BLESensorConfiguration.androidSignalCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        final BluetoothGattCharacteristic payloadCharacteristic = new BluetoothGattCharacteristic(
                BLESensorConfiguration.payloadCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(signalCharacteristic);
        // Interop with OpenTrace protocol
		if (BLESensorConfiguration.interopOpenTraceEnabled) {
			final BluetoothGattCharacteristic legacyPayloadCharacteristic = new BluetoothGattCharacteristic(
                    BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        	service.addCharacteristic(legacyPayloadCharacteristic);
		}
        service.addCharacteristic(payloadCharacteristic);
        bluetoothGattServer.addService(service);

        // Logic check - ensure there can be only one Herald service
        services = bluetoothGattServer.getServices();
        int count = 0;
        for (final BluetoothGattService svc : services) {
            if (svc.getUuid().equals(ourAdvertisedId)) {
                count++;
            }
        }
        if (count > 1) {
            logger.fault("setGattService device incorrectly sharing multiple Herald services (count={})", count);
        }
        if (0 == count) {
            // Note that the service will probably not be listed yet - updating the advert is asynchronous
            logger.fault("setGattService couldn't list Herald services after setting! Should be advertising now (or soon...).");
        }

        logger.debug("setGattService successful (service={},signalCharacteristic={},payloadCharacteristic={})",
                service.getUuid(), signalCharacteristic.getUuid(), payloadCharacteristic.getUuid());
    }

    @NonNull
    private static String onConnectionStateChangeStatusToString(final int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "STATE_CONNECTED";
            case BluetoothProfile.STATE_CONNECTING:
                return "STATE_CONNECTING";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "STATE_DISCONNECTING";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "STATE_DISCONNECTED";
            default:
                return "UNKNOWN_STATE_" + state;
        }
    }

    @NonNull
    private static String onStartFailureErrorCodeToString(final int errorCode) {
        switch (errorCode) {
            case ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "ADVERTISE_FAILED_DATA_TOO_LARGE";
            case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
            case ADVERTISE_FAILED_ALREADY_STARTED:
                return "ADVERTISE_FAILED_ALREADY_STARTED";
            case ADVERTISE_FAILED_INTERNAL_ERROR:
                return "ADVERTISE_FAILED_INTERNAL_ERROR";
            case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
            default:
                return "UNKNOWN_ERROR_CODE_" + errorCode;
        }
    }

}
