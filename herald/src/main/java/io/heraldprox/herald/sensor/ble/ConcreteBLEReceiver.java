//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import io.heraldprox.herald.BuildConfig;
import io.heraldprox.herald.sensor.PayloadDataSupplier;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.ble.filter.BLEAdvertParser;
import io.heraldprox.herald.sensor.ble.filter.BLEAdvertServiceData;
import io.heraldprox.herald.sensor.ble.filter.BLEDeviceFilter;
import io.heraldprox.herald.sensor.ble.filter.BLEScanResponseData;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.BluetoothState;
import io.heraldprox.herald.sensor.datatype.Callback;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Histogram;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadSharingData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.SignalCharacteristicData;
import io.heraldprox.herald.sensor.datatype.SignalCharacteristicDataType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConcreteBLEReceiver extends BluetoothGattCallback implements BLEReceiver {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEReceiver");
    // Scan ON/OFF/PROCESS durations
    private final static long scanOnDurationMillis = TimeInterval.seconds(4).millis();
    private final static long scanRestDurationMillis = TimeInterval.seconds(1).millis();
    private final static long scanProcessDurationMillis = TimeInterval.seconds(60).millis();
    private final static long scanOffDurationMillis = TimeInterval.seconds(2).millis();
    /**
     * Connection timeout data collected from 34,394 successful connections
     * from 6 Android phones along with 4 iPhones (10 in total) over 15 hours.
     * <table>
     *   <col width="25%"/>
     *   <col width="75%"/>
     *   <thead>
     *     <tr><th>Timeout (seconds)</th><th>Cumulative count</th><th>Cumulative distribution (%)</th></tr>
     *   <thead>
     *   <tbody>
     *      <tr><td>0</td><td>20099</td><td>58.4%</td></tr>
     *      <tr><td>1</td><td>30694</td><td>89.2%</td></tr>
     *      <tr><td>2</td><td>33041</td><td>96.1%</td></tr>
     *      <tr><td>3</td><td>33746</td><td>98.1%</td></tr>
     *      <tr><td>4</td><td>34066</td><td>99.0%</td></tr>
     *      <tr><td>5</td><td>34289</td><td>99.7%</td></tr>
     *      <tr><td>6</td><td>34337</td><td>99.8%</td></tr>
     *      <tr><td>7</td><td>34347</td><td>99.9%</td></tr>
     *      <tr><td>8</td><td>34354</td><td>99.9%</td></tr>
     *      <tr><td>9</td><td>34358</td><td>99.9%</td></tr>
     *      <tr><td>10</td><td>34358</td><td>99.9%</td></tr>
     *      <tr><td>11</td><td>34360</td><td>99.9%</td></tr>
     *      <tr><td>12</td><td>34367</td><td>99.9%</td></tr>
     *      <tr><td>13</td><td>34370</td><td>99.9%</td></tr>
     *      <tr><td>14</td><td>34373</td><td>99.9%</td></tr>
     *      <tr><td>15</td><td>34377</td><td>100.0%</td></tr>
     *      <tr><td>16</td><td>34385</td><td>100.0%</td></tr>
     *      <tr><td>17</td><td>34387</td><td>100.0%</td></tr>
     *      <tr><td>18</td><td>34389</td><td>100.0%</td></tr>
     *      <tr><td>19</td><td>34393</td><td>100.0%</td></tr>
     *      <tr><td>20</td><td>34394</td><td>100.0%</td></tr>
     *   </tbody>
     * </table>
     * 10 device test was conducted using 12s, 8s, 7s, 6s, and 3s timeouts. Test results show
     * 8s timeout offers optimal performance, achieving 98.9% continuity and 2.78%/hr battery drain.
     */
    private final static long timeToConnectDeviceLimitMillis = TimeInterval.seconds(8).millis();
    // Collect connection and processing statistics to determine timeouts based on actual data
    @NonNull
    private final Histogram timeToConnectDevice;
    @NonNull
    private final Histogram timeToProcessDevice;
    private final static int defaultMTU = 20;
    // Proxy for fixing CVE-2020-12856
    private final BLEBluetoothGattProxy bluetoothGattProxy = new BLEBluetoothGattProxy();
    @NonNull
    private final Context context;
    @NonNull
    private final BluetoothStateManager bluetoothStateManager;
    @NonNull
    private final BLEDatabase database;
    @NonNull
    private final BLETransmitter transmitter;
    @NonNull
    private final PayloadDataSupplier payloadDataSupplier;
    @NonNull
    private final BLEDeviceFilter deviceFilter;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final Queue<ScanResult> scanResults = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean receiverEnabled = new AtomicBoolean(false);

    private enum NextTask {
        nothing, readPayload, writePayload, writeRSSI, writePayloadSharing, immediateSend,
        readModel, readDeviceName
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult scanResult) {
            final ScanRecord scanRecord = scanResult.getScanRecord();
            final Data data = new Data(scanRecord != null ? scanRecord.getBytes() : new byte[0]);
            logger.debug("onScanResult (result={}, data={})", scanResult, data.hexEncodedString());

            scanResults.add(scanResult);
            // Create or update device in database
            final BLEDevice device = database.device(scanResult);
            device.registerDiscovery();
            // Read RSSI from scan result
            device.rssi(new RSSI(scanResult.getRssi()));
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
            for (ScanResult scanResult : results) {
                onScanResult(0, scanResult);
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            logger.fault("onScanFailed (error={})", onScanFailedErrorCodeToString(errorCode));
            super.onScanFailed(errorCode);
        }
    };

    /**
     * Receiver starts automatically when Bluetooth is enabled.
     *
     * @param context The Herald execution environment Context
     * @param bluetoothStateManager To determine whether Bluetooth is enabled
     * @param timer Used to register a need for periodic events to occur
     * @param database BLE Device database to locate nearby device information
     * @param transmitter The associated BLETransmitter instance
     * @param payloadDataSupplier Source of the payload to transmit
     */
    public ConcreteBLEReceiver(@NonNull final Context context, @NonNull final BluetoothStateManager bluetoothStateManager, @NonNull final BLETimer timer, @NonNull final BLEDatabase database, @NonNull final BLETransmitter transmitter, @NonNull final PayloadDataSupplier payloadDataSupplier) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.database = database;
        this.transmitter = transmitter;
        this.payloadDataSupplier = payloadDataSupplier;
        timer.add(new ScanLoopTask());

        // Enable device introspection if device filter training is enabled
        // to obtain device name and model data for all devices, and also
        // log the device and advert data to "filter.csv" file
        if (BLESensorConfiguration.deviceFilterTrainingEnabled) {
            // Obtain device model and name where available
            BLESensorConfiguration.deviceIntrospectionEnabled = true;
            // Trigger connection every minute to gather sample advert data
            BLESensorConfiguration.payloadDataUpdateTimeInterval = TimeInterval.minute;
            // Log results to file for analysis
            this.deviceFilter = new BLEDeviceFilter(context, "filter.csv");
        } else {
            // Standard rule-based filter
            this.deviceFilter = new BLEDeviceFilter();
        }
        // Only collect and store histogram of connection and processing time in debug mode
        this.timeToConnectDevice = (BuildConfig.DEBUG ? new Histogram(0, 20, TimeInterval.minute, new TextFile(context, "timeToConnectDevice.csv")) : null);
        this.timeToProcessDevice = (BuildConfig.DEBUG ? new Histogram(0, 60, TimeInterval.minute, new TextFile(context, "timeToProcessDevice.csv")) : null);
    }

    // MARK:- BLEReceiver

    @Override
    public void add(@NonNull final SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        if (receiverEnabled.compareAndSet(false, true)) {
            logger.debug("start, receiver enabled to follow bluetooth state");
        } else {
            logger.fault("start, receiver already enabled to follow bluetooth state");
        }
    }

    @Override
    public void stop() {
        if (receiverEnabled.compareAndSet(true, false)) {
            logger.debug("stop, receiver disabled");
        } else {
            logger.fault("stop, receiver already disabled");
        }
    }

    @Override
    public boolean immediateSend(@NonNull final Data data, @NonNull final TargetIdentifier targetIdentifier) {
        logger.debug("immediateSend (targetIdentifier: {})", targetIdentifier);
        final BLEDevice device = database.device(targetIdentifier);
        if (null == device) {
            logger.fault("immediateSend denied, peripheral not found (targetIdentifier: {})",
                    targetIdentifier);
            return false;
        }

        final Data dataToSend = SignalCharacteristicData.encodeImmediateSend(new ImmediateSendData(data));
        logger.debug("immediateSend (device={},dataLength={})", device, dataToSend.value.length);

        // Immediate send process
        // 1. Set immediate send data for device
        // 2. Initiate connection to device
        // 3. onConnectionStateChange() will trigger service and characteristic discovery
        // 4. signalCharacteristic discovery will trigger nextTask()
        // 5. nextTask() will be .immediateSend if immediate send data has been set for device
        // 6. writeSignalCharacteristic() will be called to perform immediate send to signalCharacteristic
        // 7. onCharacteristicWrite() will be triggered when signal data has been written
        // 8. Immediate send data for device will be set to null upon completion of write
        // 9. Connection is closed immediately
        device.immediateSendData(dataToSend);
        return taskConnectDevice(device);
    }

    @Override
    public boolean immediateSendAll(@NonNull final Data data) {
        logger.debug("immediateSendAll");
        // Encode data
        final Data dataToSend = SignalCharacteristicData.encodeImmediateSend(new ImmediateSendData(data));
        logger.debug("immediateSendAll (dataLength={})", dataToSend.value.length);

        // Order by descending time seen (most recent first)
        // Choose targets
        final SortedSet<BLEDevice> targets = new TreeSet<>(new BLEDeviceLastUpdatedComparator());
        // Fetch targets seen (for RSSI via advert) in the last minute
        for (final BLEDevice device : database.devices()) {
            if (!device.ignore() && null != device.signalCharacteristic() && device.timeIntervalSinceLastUpdate().value < 60) {
                targets.add(device);
            }
        }
        // Send messages
        // Connect and immediate send to each
        // NOTE: This separate loop doesn't order interactions yet. Once working, refactor so this has an effect.
        for (final BLEDevice target : targets) {
            target.immediateSendData(dataToSend);
        }
        // Now force an out of sequence connection (and thus immediate send as the next action)
        for (final BLEDevice target : targets) {
            taskConnectDevice(target);
        }
        return true; // fire and forget
    }

    // MARK:- Scan loop for startScan-wait-stopScan-processScanResults-wait-repeat

    private enum ScanLoopState {
        scanStarting, scanStarted, scanStopping, scanStopped, processing, processed
    }

    private class ScanLoopTask implements BLETimerDelegate {
        private ScanLoopState scanLoopState = ScanLoopState.processed;
        private long lastStateChangeAt = System.currentTimeMillis();

        private void state(final long now, @NonNull final ScanLoopState state) {
            final long elapsed = now - lastStateChangeAt;
            logger.debug("scanLoopTask, state change (from={},to={},elapsed={}ms)", scanLoopState, state, elapsed);
            this.scanLoopState = state;
            lastStateChangeAt = now;
        }

        private long timeSincelastStateChange(final long now) {
            return now - lastStateChangeAt;
        }

        @Nullable
        private BluetoothLeScanner bluetoothLeScanner() {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (null == bluetoothAdapter) {
                logger.fault("ScanLoop denied, Bluetooth adapter unavailable");
                return null;
            }
            final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (null == bluetoothLeScanner) {
                logger.fault("ScanLoop denied, Bluetooth LE scanner unavailable");
                return null;
            }
            return bluetoothLeScanner;
        }

        @Override
        public void bleTimer(final long now) {
            logger.debug("scanLoopTask, bleTimer");
            switch (scanLoopState) {
                case processed: {
                    if (receiverEnabled.get() && bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= scanOffDurationMillis) {
                            logger.debug("scanLoopTask, start scan (process={}ms)", period);
                            final BluetoothLeScanner bluetoothLeScanner = bluetoothLeScanner();
                            if (null == bluetoothLeScanner) {
                                logger.fault("scanLoopTask, start scan denied, Bluetooth LE scanner unavailable");
                                return;
                            }
                            state(now, ScanLoopState.scanStarting);
                            startScan(bluetoothLeScanner, new Callback<Boolean>() {
                                @Override
                                public void accept(@NonNull final Boolean value) {
                                    state(now, value ? ScanLoopState.scanStarted : ScanLoopState.scanStopped);
                                }
                            });
                        }
                    }
                    break;
                }
                case scanStarted: {
                    final long period = timeSincelastStateChange(now);
                    if (period >= scanOnDurationMillis) {
                        logger.debug("scanLoopTask, stop scan (scan={}ms)", period);
                        final BluetoothLeScanner bluetoothLeScanner = bluetoothLeScanner();
                        if (null == bluetoothLeScanner) {
                            logger.fault("scanLoopTask, stop scan denied, Bluetooth LE scanner unavailable");
                            return;
                        }
                        state(now, ScanLoopState.scanStopping);
                        stopScan(bluetoothLeScanner, new Callback<Boolean>() {
                            @Override
                            public void accept(@NonNull final Boolean value) {
                                state(now, ScanLoopState.scanStopped);
                            }
                        });
                    }
                    break;
                }
                case scanStopped: {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= scanRestDurationMillis) {
                            logger.debug("scanLoopTask, start processing (stop={}ms)", period);
                            state(now, ScanLoopState.processing);
                            processScanResults(new Callback<Boolean>() {
                                @Override
                                public void accept(@NonNull final Boolean value) {
                                    state(now, ScanLoopState.processed);
                                    if (!receiverEnabled.get()) {
                                        logger.debug("scanLoopTask, stopped because receiver is disabled");
                                    }
                                }
                            });
                        }
                    }
                    break;
                }
            }
        }
    }


    /**
     * Start scan.
     * @param bluetoothLeScanner BLE scanner
     * @param callback Callback for start scan result
     */
    private void startScan(@NonNull final BluetoothLeScanner bluetoothLeScanner, @Nullable final Callback<Boolean> callback) {
        logger.debug("startScan");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    scanForPeripherals(bluetoothLeScanner);
                    logger.debug("startScan successful");
                    if (null != callback) {
                        callback.accept(true);
                    }
                } catch (Throwable e) {
                    logger.fault("startScan failed", e);
                    if (null != callback) {
                        callback.accept(false);
                    }
                }
            }
        });
    }

    /**
     * Scan for devices advertising sensor service and all Apple devices as
     * iOS background advert does not include service UUID. There is a risk
     * that the sensor will spend time communicating with Apple devices that
     * are not running the sensor code repeatedly, but there is no reliable
     * way of filtering this as the service may be absent only because of
     * transient issues. This will be handled in taskConnect.
     * @param bluetoothLeScanner BLE scanner
     */
    private void scanForPeripherals(@NonNull final BluetoothLeScanner bluetoothLeScanner) {
        logger.debug("scanForPeripherals");
        final List<ScanFilter> filter = new ArrayList<>(4);
        // Scan for HERALD protocol service on iOS (background) devices
        filter.add(new ScanFilter.Builder().setManufacturerData(
                BLESensorConfiguration.manufacturerIdForApple, new byte[0], new byte[0]).build());

        // This logic added in v2.2 to enable disabling of standard herald device detection and
        // detection of custom application IDs
        if (BLESensorConfiguration.customServiceDetectionEnabled) {
            if (null != BLESensorConfiguration.customServiceUUID) {
                filter.add(new ScanFilter.Builder().setServiceUuid(
                                new ParcelUuid(BLESensorConfiguration.customServiceUUID),
                                new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                        .build());
            }
            if (null != BLESensorConfiguration.customAdditionalServiceUUIDs) {
                for (int idx = 0;idx < BLESensorConfiguration.customAdditionalServiceUUIDs.length;idx++) {
                    filter.add(new ScanFilter.Builder().setServiceUuid(
                                    new ParcelUuid(BLESensorConfiguration.customAdditionalServiceUUIDs[idx]),
                                    new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                            .build());
                }
            }
        }
        // This is useful for a custom application.
        if (BLESensorConfiguration.standardHeraldServiceDetectionEnabled) {
            // Scan for HERALD protocol service on Android or iOS (foreground) devices
            filter.add(new ScanFilter.Builder().setServiceUuid(
                            new ParcelUuid(BLESensorConfiguration.linuxFoundationServiceUUID),
                            new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                    .build());
        }
        // Scan for OpenTrace protocol service on iOS and Android devices
        if (BLESensorConfiguration.interopOpenTraceEnabled) {
            filter.add(new ScanFilter.Builder().setServiceUuid(
                            new ParcelUuid(BLESensorConfiguration.interopOpenTraceServiceUUID),
                            new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                    .build());
        }
        if (BLESensorConfiguration.legacyHeraldServiceDetectionEnabled) {
            filter.add(new ScanFilter.Builder().setServiceUuid(
                            new ParcelUuid(BLESensorConfiguration.legacyHeraldServiceUUID),
                            new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                    .build());
        }
        // Scan for legacy advert only protocol service
        if (BLESensorConfiguration.interopAdvertBasedProtocolEnabled) {
            filter.add(new ScanFilter.Builder().setServiceUuid(
                            new ParcelUuid(BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID),
                            new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                    .build());
        }
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("scanForPeripherals, no BLUETOOTH_SCAN permission");
                return;
            }
        }
        bluetoothLeScanner.startScan(filter, settings, scanCallback);
    }

    private void processScanResults(@NonNull final Callback<Boolean> callback) {
        logger.debug("processScanResults");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    processScanResults();
                    logger.debug("processScanResults, processed scan results");
                } catch (Throwable e) {
                    logger.fault("processScanResults warning, processScanResults error", e);
                    callback.accept(false);
                }
                logger.debug("processScanResults successful");
                callback.accept(true);
            }
        });
    }

    /**
     * Stop scan.
     * @param bluetoothLeScanner BLE scanner
     * @param callback Callback for stop scan result
     */
    private void stopScan(@NonNull final BluetoothLeScanner bluetoothLeScanner, @NonNull final Callback<Boolean> callback) {
        logger.debug("stopScan");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            logger.fault("stopScan, no BLUETOOTH_SCAN permission");
                            return;
                        }
                    }
                    bluetoothLeScanner.stopScan(scanCallback);
                    logger.debug("stopScan, stopped scanner");
                } catch (Throwable e) {
                    logger.fault("stopScan warning, bluetoothLeScanner.stopScan error", e);
                }
                try {
                    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (null != bluetoothAdapter) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                    logger.debug("stopScan, cancelled discovery");
                } catch (Throwable e) {
                    logger.fault("stopScan warning, bluetoothAdapter.cancelDiscovery error", e);
                }
                logger.debug("stopScan successful");
                callback.accept(true);
            }
        });
    }

    // MARK:- Process scan results

    /**
     * Process all recent scan results.
     */
    private void processScanResults() {
        final long t0 = System.currentTimeMillis();
        logger.debug("processScanResults (results={})", scanResults.size());
        // Identify devices discovered in last scan
        final List<BLEDevice> didDiscover = didDiscover();
        taskRemoveExpiredDevices();
        taskCorrectConnectionStatus();
        taskConnect(didDiscover);
        taskLegacyAdvertOnlyProtocolService(didDiscover);
        final long t1 = System.currentTimeMillis();
        logger.debug("processScanResults (results={},devices={},elapsed={}ms)", scanResults.size(), didDiscover.size(), (t1 - t0));
    }

    // MARK:- didDiscover

    /**
     * Process scan results to ...
     * <br>1. Create BLEDevice from scan result for new devices
     * <br>2. Read RSSI
     * <br>3. Identify operating system where possible
     */
    @NonNull
    private List<BLEDevice> didDiscover() {
        // Take current copy of concurrently modifiable scan results
        final List<ScanResult> scanResultList = new ArrayList<>(scanResults.size());
        while (scanResults.size() > 0) {
            scanResultList.add(scanResults.poll());
        }

        // Process scan results and return devices created/updated in scan results
        logger.debug("didDiscover (scanResults={})", scanResultList.size());
        final Set<BLEDevice> deviceSet = new HashSet<>();
        final List<BLEDevice> devices = new ArrayList<>();
        for (final ScanResult scanResult : scanResultList) {
            final BLEDevice device = database.device(scanResult);
            if (deviceSet.add(device)) {
                logger.debug("didDiscover (device={})", device);
                devices.add(device);
            }
            // Set scan record
            device.scanRecord(scanResult.getScanRecord());
            // Set TX power level
            if (null != device.scanRecord()) {
                //noinspection ConstantConditions
                int txPowerLevel = device.scanRecord().getTxPowerLevel();
                if (txPowerLevel != Integer.MIN_VALUE) {
                    device.txPower(new BLE_TxPower(txPowerLevel));
                }
            }
            // Identify operating system from scan record where possible
            // - Sensor service found + Manufacturer is Apple -> iOS (Foreground)
            // - Sensor service found + Manufacturer not Apple -> Android
            // - Sensor service not found + Manufacturer is Apple -> iOS (Background) or Apple device not advertising sensor service, to be resolved later
            // - Sensor service not found + Manufacturer not Apple -> Ignore (shouldn't be possible as we are scanning for Apple or with service)
            // - OpenTrace service found + Manufacturer is Apple -> iOS (Foreground)
            // - OpenTrace service found + Manufactuerr not Apple -> Android
            final boolean hasSensorService = hasSensorService(scanResult);
            final boolean hasOpenTraceService = hasOpenTraceService(scanResult);
            final boolean isAppleDevice = isAppleDevice(scanResult);
            if (hasOpenTraceService) {
                device.operatingSystem(isOpenTraceAndroidDevice(scanResult) ? BLEDeviceOperatingSystem.android : BLEDeviceOperatingSystem.ios);
            } else if (hasSensorService && isAppleDevice) {
                // Definitely iOS device offering sensor service in foreground mode
                device.operatingSystem(BLEDeviceOperatingSystem.ios);
            } else if (hasSensorService) { // !isAppleDevice implied
                // Definitely Android device offering sensor service
                if (device.operatingSystem() != BLEDeviceOperatingSystem.android) {
                    device.operatingSystem(BLEDeviceOperatingSystem.android_tbc);
                }
            } else if (isAppleDevice) { // !hasSensorService implied
                // Filter device by advert messages unless it is already confirmed ios device
                final BLEDeviceFilter.MatchingPattern matchingPattern = deviceFilter.match(device);
                if (device.operatingSystem() != BLEDeviceOperatingSystem.ios && null != matchingPattern) {
                    logger.debug("didDiscover, ignoring filtered device (device={},pattern={},message={})", device, matchingPattern.filterPattern.regularExpression, matchingPattern.message);
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore);
                }
                // Possibly an iOS device offering sensor service in background mode,
                // can't be sure without additional checks after connection, so
                // only set operating system if it is unknown to offer a guess.
                if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ios_tbc);
                }
            } else if (BLESensorConfiguration.interopAdvertBasedProtocolEnabled) {
                // Sensor service not found + Manufacturer not Apple should be impossible
                // as we are scanning for devices with sensor service or Apple device.
                logger.fault("didDiscover, invalid non-Apple device without sensor service (device={})", device);
                if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore);
                }
            } else {
                // hasLegacyAdvertOnlyProtocolServiceService implied
                // Legacy advertising only protocol service found, set to ignore
                // as this is handled by taskLegacyAdvertOnlyProtocolService without connection
                if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore);
                }
            }
        }
        return devices;
    }

    /**
     * Does scan result include advert for sensor service?
     * @param scanResult Scan result
     * @return True if result includes advert for sensor service, false otherwise
     */
    private static boolean hasSensorService(@NonNull final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (null == scanRecord) {
            return false;
        }
        final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (null == serviceUuids || 0 == serviceUuids.size()) {
            return false;
        }
        for (final ParcelUuid serviceUuid : serviceUuids) {
            // Since v2.2 try custom service UUID(s) first
            if (BLESensorConfiguration.customServiceDetectionEnabled) {
                if (null != BLESensorConfiguration.customServiceUUID &&
                    serviceUuid.getUuid().equals(BLESensorConfiguration.customServiceUUID)) {
                    return true;
                }
                if (null != BLESensorConfiguration.customAdditionalServiceUUIDs) {
                    for (int idx = 0; idx < BLESensorConfiguration.customAdditionalServiceUUIDs.length;idx++) {
                        if (serviceUuid.getUuid().equals(BLESensorConfiguration.customAdditionalServiceUUIDs[idx])) {
                            return true;
                        }
                    }
                }
            }
            // Extra if term added in v2.2 so a custom app doesn't accidentally detect normal standard Herald devices
            if (BLESensorConfiguration.standardHeraldServiceDetectionEnabled &&
                serviceUuid.getUuid().equals(BLESensorConfiguration.linuxFoundationServiceUUID)) {
                return true;
            }
            if (BLESensorConfiguration.legacyHeraldServiceDetectionEnabled &&
                serviceUuid.getUuid().equals(BLESensorConfiguration.legacyHeraldServiceUUID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does scan result indicate device was manufactured by Apple?
     * @param scanResult Scan result
     * @return True if Apple device, false otherwise.
     */
    private static boolean isAppleDevice(@NonNull final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (null == scanRecord) {
            return false;
        }
        final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.manufacturerIdForApple);
        return null != data;
    }

    /**
     * Does scan result indicate device is OpenTrace Android (true) or iOS (false) device?
     * @param scanResult Scan result
     * @return True for OpenTrace Android devices, false otherwise
     */
    private static boolean isOpenTraceAndroidDevice(@NonNull final ScanResult scanResult) {
        if (!BLESensorConfiguration.interopOpenTraceEnabled) {
            return false;
        }
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (null == scanRecord) {
            return false;
        }
        if (0 == scanRecord.getManufacturerSpecificData().size()) {
            return false;
        }
        final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.interopOpenTraceManufacturerId);
        return null != data;
    }

    /**
     * Does scan result include advert for OpenTrace service?
     * @param scanResult Scan result
     * @return True for adverts containing OpenTrace service, false otherwise
     */
    private static boolean hasOpenTraceService(@NonNull final ScanResult scanResult) {
        if (!BLESensorConfiguration.interopOpenTraceEnabled) {
            return false;
        }
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (null == scanRecord) {
            return false;
        }
        final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (null == serviceUuids || 0 == serviceUuids.size()) {
            return false;
        }
        for (final ParcelUuid serviceUuid : serviceUuids) {
            if (serviceUuid.getUuid().equals(BLESensorConfiguration.interopOpenTraceServiceUUID)) {
                return true;
            }
        }
        return false;
    }

    // MARK:- Legacy advertising only protocol service

    private void taskLegacyAdvertOnlyProtocolService(@NonNull final List<BLEDevice> discovered) {
        if (!BLESensorConfiguration.interopAdvertBasedProtocolEnabled) {
            // Not searching for legacy advertising only protocol service or service data
            return;
        }
        final long timeStart = System.currentTimeMillis();
        for (final BLEDevice device : discovered) {
            // Stop process if exceeded time limit
            final long elapsedTime = System.currentTimeMillis() - timeStart;
            if (elapsedTime >= scanProcessDurationMillis) {
                logger.debug("taskLegacyAdvertOnlyProtocolService, reached time limit (elapsed={}ms,limit={}ms)", elapsedTime, scanProcessDurationMillis);
                break;
            }
            processLegacyAdvertOnlyProtocolServiceData(device);
        }
    }

    /**
     * Extract messages from manufacturer specific data.
     * @param device BLE device
     */
    private void processLegacyAdvertOnlyProtocolServiceData(@NonNull final BLEDevice device) {
        // Test if device has legacy advert only protocol service
        if (!hasLegacyAdvertOnlyProtocolServiceService(device)) {
            return;
        }
        try {
            final ScanRecord scanRecord = device.scanRecord();
            if (null == scanRecord) {
                return;
            }
            final BLEScanResponseData bleScanResponseData = BLEAdvertParser.parseScanResponse(scanRecord.getBytes(), 0);
            final List<BLEAdvertServiceData> bleAdvertServiceDataList = BLEAdvertParser.extractServiceUUID16Data(bleScanResponseData.segments);
            for (final BLEAdvertServiceData bleAdvertServiceData : bleAdvertServiceDataList) {
                try {
                    // Test if service data area contains expected service data key
                    if (!BLESensorConfiguration.interopAdvertBasedProtocolServiceDataKey.equals(new Data(bleAdvertServiceData.service))) {
                        continue;
                    }
                    //noinspection ConstantConditions
                    if (null != bleAdvertServiceData.data && bleAdvertServiceData.data.length > 0) {
                        final LegacyPayloadData payloadData = new LegacyPayloadData(BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID, bleAdvertServiceData.data);
                        device.payloadData(payloadData);
                        logger.debug("processLegacyAdvertOnlyProtocolServiceData, found service (device={},payload={})", device, payloadData.shortName());
                    }
                } catch (Throwable e) {
                    // Errors are expected due to corrupt data
                }
            }
        } catch (Throwable e) {
            // Errors are expected due to corrupt data
        }
    }

    /**
     * Does device include advert for legacy advertising only protocol service?
     * @param device BLE device
     * @return True for devices with advert for legacy advertising only protocol service, false otherwise
     */
    private static boolean hasLegacyAdvertOnlyProtocolServiceService(@Nullable final BLEDevice device) {
        if (null == device) {
            return false;
        }
        final ScanRecord scanRecord = device.scanRecord();
        if (null == scanRecord) {
            return false;
        }
        final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (null == serviceUuids || 0 == serviceUuids.size()) {
            return false;
        }
        for (final ParcelUuid serviceUuid : serviceUuids) {
            if (serviceUuid.getUuid().equals(BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID)) {
                return true;
            }
        }
        return false;
    }


    // MARK:- House keeping tasks

    /**
     * Remove devices that have not been updated for over 15 minutes, as the UUID is likely
     * to have changed after being out of range for over 20 minutes, so it will require
     * discovery. Discovery is fast and cheap on Android.
     */
    private void taskRemoveExpiredDevices() {
        final List<BLEDevice> devicesToRemove = new ArrayList<>();
        for (final BLEDevice device : database.devices()) {
            if (device.timeIntervalSinceLastUpdate().value > TimeInterval.minutes(15).value) {
                devicesToRemove.add(device);
            }
        }
        for (final BLEDevice device : devicesToRemove) {
            logger.debug("taskRemoveExpiredDevices (remove={})", device);
            database.delete(device);
        }
    }

    /**
     * Connections should not be held for more than 1 minute, likely to have not received
     * onConnectionStateChange callback.
     */
    private void taskCorrectConnectionStatus() {
        for (final BLEDevice device : database.devices()) {
            if (device.state() == BLEDeviceState.connected && device.timeIntervalSinceConnected().value > TimeInterval.minute.value) {
                logger.debug("taskCorrectConnectionStatus (device={})", device);
                device.state(BLEDeviceState.disconnected);
            }
        }
    }


    // MARK:- Connect task

    private void taskConnect(@NonNull final List<BLEDevice> discovered) {
        // Clever connection prioritisation is pointless here as devices
        // like the Samsung A10 and A20 changes mac address on every scan
        // call, so optimising new device handling is more effective.
        final long timeStart = System.currentTimeMillis();
        int devicesProcessed = 0;
        for (final BLEDevice device : discovered) {
            // Stop process if exceeded time limit
            final long elapsedTime = System.currentTimeMillis() - timeStart;
            if (elapsedTime >= scanProcessDurationMillis) {
                logger.debug("taskConnect, reached time limit (elapsed={}ms,limit={}ms)", elapsedTime, scanProcessDurationMillis);
                break;
            }
            if (devicesProcessed > 0) {
                final long predictedElapsedTime = Math.round((elapsedTime / (double) devicesProcessed) * (devicesProcessed + 1));
                if (predictedElapsedTime > scanProcessDurationMillis) {
                    logger.debug("taskConnect, likely to exceed time limit soon (elapsed={}ms,devicesProcessed={},predicted={}ms,limit={}ms)", elapsedTime, devicesProcessed, predictedElapsedTime, scanProcessDurationMillis);
                    break;
                }
            }
            if (nextTaskForDevice(device) == NextTask.nothing) {
                logger.debug("taskConnect, no pending action (device={})", device);
                continue;
            }
            taskConnectDevice(device);
            devicesProcessed++;
        }
    }

    private boolean taskConnectDevice(@NonNull final BLEDevice device) {
        if (device.state() == BLEDeviceState.connected) {
            logger.debug("taskConnectDevice, already connected to transmitter (device={})", device);
            return true;
        }
        final long timeConnect = System.currentTimeMillis();
        logger.debug("taskConnectDevice, connect (device={})", device);
        device.state(BLEDeviceState.connecting);
        final BluetoothDevice peripheral = device.peripheral();
        BluetoothGatt gatt = null;
        if (null != peripheral) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    logger.fault("taskConnectDevice, no BLUETOOTH_CONNECT permission");
                    return false;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23 and above - force Low Energy only
                gatt = peripheral.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE);
            } else {
                // support back to API 21
                gatt = peripheral.connectGatt(context, false, this);
            }
        }
        if (null == gatt) {
            logger.fault("taskConnectDevice, connect failed (device={})", device);
            device.state(BLEDeviceState.disconnected);
            return false;
        }
        // Wait for connection
        // A connect request should normally result in .connected or .disconnected state which is
        // set asynchronously by the callback function onConnectionStateChange(). However, some
        // connections may get stuck in a .connecting state indefinitely due to BLE issues, and
        // therefore the callback function is never called, leaving the device in a limbo state.
        // As such, the follow loop runs for a fixed duration (established through experimentation)
        // to check if connection was successful, else abort the connection to put the device in
        // a consistent default .disconnected state.
        while (device.state() != BLEDeviceState.connected && device.state() != BLEDeviceState.disconnected && (System.currentTimeMillis() - timeConnect) < timeToConnectDeviceLimitMillis) {
            try {
                Thread.sleep(200);
            } catch (Throwable e) {
                logger.fault("Timer interrupted", e);
            }
        }
        if (device.state() != BLEDeviceState.connected) {
            // Failed to establish connection within time limit, assume connection failure
            // and disconnect device to put it in a consistent default .disconnected state
            logger.fault("taskConnectDevice, connect timeout (device={})", device);
            try {
                gatt.close();
            } catch (Throwable e) {
                logger.fault("taskConnectDevice, close failed (device={})", device, e);
            }
            return false;
        } else {
            // Connection was successful, make note of time to establish connection to
            // inform setting of timeToConnectDeviceLimitMillis. A previous implementation
            // used an adaptive algorithm to adjust this parameter according to device
            // capability, but that was deemed too unreliable for minimal gain in
            // performance, as the target device plays a big part in determining the
            // connection time, and that can be unpredictable due to environment factors.
            final long timeToConnectMillis = System.currentTimeMillis() - timeConnect;
            // Add sample to adaptive connection timeout
            if (null != timeToConnectDevice) {
                timeToConnectDevice.add((int) (timeToConnectMillis / 1000));
            }
            logger.debug("taskConnectDevice, connected (device={},elapsed={}ms)", device, timeToConnectMillis);
        }
        // Wait for disconnection
        // Device is connected at this point, and all the actual work is being
        // performed asynchronously by callback methods outside of this function.
        // As such, the only work required here at this point is to keep track
        // of connection time, to ensure a connection is not held for too long
        // e.g. due to BLE issues. The following code waits for device state change
        // from .connected to .disconnected, which is normally set asynchronously
        // by the callback function onConnectionStateChange(), once all the tasks
        // for the device have been completed. If the connection has been held
        // too long, then this function will force a disconnection by calling
        // gatt.close() to disconnect device to put it in a consistent default
        // .disconnected state.
        while (device.state() != BLEDeviceState.disconnected && (System.currentTimeMillis() - timeConnect) < scanProcessDurationMillis) {
            try {
                Thread.sleep(500);
            } catch (Throwable e) {
                logger.fault("Timer interrupted", e);
            }
        }
        boolean success = true;
        // Timeout connection if required, and always set state to disconnected
        if (device.state() != BLEDeviceState.disconnected) {
            // Failed to complete tasks and disconnect within time limit, assume failure
            // and disconnect device to put it in a consistent default .disconnected state
            logger.fault("taskConnectDevice, disconnect timeout (device={})", device);
            try {
                gatt.close();
            } catch (Throwable e) {
                logger.fault("taskConnectDevice, close failed (device={})", device, e);
            }
            success = false;
        }
        // Always set state to .disconnected at the end
        device.state(BLEDeviceState.disconnected);
        final long timeToProcessMillis = (System.currentTimeMillis() - timeConnect);
        if (success) {
            if (null != timeToProcessDevice) {
                timeToProcessDevice.add((int) (timeToProcessMillis / 1000));
            }
            logger.debug("taskConnectDevice, complete (success=true,device={},elapsed={}ms)", device, timeToProcessMillis);
        } else {
            logger.fault("taskConnectDevice, complete (success=false,device={},elapsed={}ms)", device, timeToProcessMillis);
        }
        // Train device filter
        if (BLESensorConfiguration.deviceFilterTrainingEnabled) {
            deviceFilter.train(device, null == device.payloadCharacteristic());
        }
        return success;
    }

    // MARK:- BluetoothStateManagerDelegate

    @Override
    public void onConnectionStateChange(@NonNull final BluetoothGatt gatt, final int status, final int newState) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState));
        if (BluetoothProfile.STATE_CONNECTED == newState) {
            device.state(BLEDeviceState.connected);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    logger.fault("onConnectionStateChange, no BLUETOOTH_CONNECT permission");
                    return;
                }
            }
            gatt.discoverServices();
        } else if (BluetoothProfile.STATE_DISCONNECTED == newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    logger.fault("onConnectionStateChange, no BLUETOOTH_CONNECT permission");
                    return;
                }
            }
            gatt.close();
            device.state(BLEDeviceState.disconnected);
            if (0 != status) {
                logger.fault("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState));
            }
         } else {
            logger.debug("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState));
        }
    }

    @Override
    public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, final int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onServicesDiscovered (device={},status={})", device, bleStatus(status));

        // Sensor characteristics
        BluetoothGattService service = gatt.getService(BLESensorConfiguration.linuxFoundationServiceUUID);
        // Since v2.2 override if a custom UUID is specified
        if (BLESensorConfiguration.customServiceDetectionEnabled) {
            service = null; // Ensure we don't accidentally re-enable Herald detection
            // Try the main custom one first (if not null)
            if (null != BLESensorConfiguration.customServiceUUID) {
                service = gatt.getService(BLESensorConfiguration.customServiceUUID);
            }
            if (null == service && null != BLESensorConfiguration.customAdditionalServiceUUIDs) {
                // Now try any additional custom service UUIDs
                for (int idx = 0;null == service && idx < BLESensorConfiguration.customAdditionalServiceUUIDs.length; idx++) {
                    service = gatt.getService(BLESensorConfiguration.customAdditionalServiceUUIDs[idx]);
                }
            }
        }
        if (null == service && BLESensorConfiguration.interopOpenTraceEnabled) {
            service = gatt.getService(BLESensorConfiguration.interopOpenTraceServiceUUID);
        }
        if (null == service && BLESensorConfiguration.legacyHeraldServiceDetectionEnabled) {
            service = gatt.getService(BLESensorConfiguration.legacyHeraldServiceUUID);
        }
        if (null == service) {
            logger.fault("onServicesDiscovered, missing sensor service (device={})", device);
            //noinspection StatementWithEmptyBody
            if (!BLESensorConfiguration.deviceFilterTrainingEnabled) {
                // Ignore device for a while unless it is a confirmed iOS or Android device,
                // where the sensor service has been found before, so ignore for a limited
                // time and try again in the near future.
                if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        logger.fault("onServicesDiscovered, no BLUETOOTH_CONNECT permission");
                        return;
                    }
                }
                gatt.disconnect();
                return;
            } else {
                // Device filter training enabled, maintain connection to obtain device
                // name and model for all devices, regardless of whether the device is
                // offering sensor services.
            }
        } else {
            logger.debug("onServicesDiscovered, found sensor service (device={},service={})", device,service.getUuid());
            device.invalidateCharacteristics();
            for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                // Confirm operating system with signal characteristic
                if (characteristic.getUuid().equals(BLESensorConfiguration.androidSignalCharacteristicUUID)) {
                    logger.debug("onServicesDiscovered, found Android signal characteristic (device={})", device);
                    device.operatingSystem(BLEDeviceOperatingSystem.android);
                    device.signalCharacteristic(characteristic);
                } else if (characteristic.getUuid().equals(BLESensorConfiguration.iosSignalCharacteristicUUID)) {
                    logger.debug("onServicesDiscovered, found iOS signal characteristic (device={})", device);
                    device.operatingSystem(BLEDeviceOperatingSystem.ios);
                    device.signalCharacteristic(characteristic);
                } else if (characteristic.getUuid().equals(BLESensorConfiguration.payloadCharacteristicUUID)) {
                    logger.debug("onServicesDiscovered, found payload characteristic (device={})", device);
                    device.payloadCharacteristic(characteristic);
                } else if (characteristic.getUuid().equals(BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID)) {
                    logger.debug("onServicesDiscovered, found legacy payload characteristic (device={})", device);
                    device.legacyPayloadCharacteristic(characteristic);
                    // If they have the legacy characteristic we know it a COVID app and can set the OS to be confirmed
	                if (device.operatingSystem() == BLEDeviceOperatingSystem.android_tbc) {
	                    device.operatingSystem(BLEDeviceOperatingSystem.android);
	                } else if (device.operatingSystem() == BLEDeviceOperatingSystem.ios_tbc) {
	                    device.operatingSystem(BLEDeviceOperatingSystem.ios);
	                }
				}
            }
            // Copy legacy payload characteristic to payload characteristic if null
            if (null == device.payloadCharacteristic() && null != device.legacyPayloadCharacteristic()) {
                device.payloadCharacteristic(device.legacyPayloadCharacteristic());
            }
        }

        // Device characteristics : Enabled if either device introspection or device filter training is enabled
        if (BLESensorConfiguration.deviceIntrospectionEnabled) {
            // Generic access : Device name
            if (null == device.deviceName()) {
                device.deviceNameCharacteristic(serviceCharacteristic(gatt, BLESensorConfiguration.bluetoothGenericAccessServiceUUID, BLESensorConfiguration.bluetoothGenericAccessServiceDeviceNameCharacteristicUUID));
                if (device.supportsDeviceNameCharacteristic()) {
                    logger.debug("onServicesDiscovered, found other service (device={},service=GenericAccess,characteristic=DeviceName)", device);
                }
            }
            // Device information : Model
            if (null == device.model()) {
                device.modelCharacteristic(serviceCharacteristic(gatt, BLESensorConfiguration.bluetoothDeviceInformationServiceUUID, BLESensorConfiguration.bluetoothDeviceInformationServiceModelCharacteristicUUID));
                if (device.supportsModelCharacteristic()) {
                    logger.debug("onServicesDiscovered, found other service (device={},service=DeviceInformation,characteristic=Model)", device);
                }
            }
        }

        nextTask(gatt);
    }

    /**
     * Get Bluetooth service characteristic, or null if not found.
     * @param gatt GATT
     * @param service Service UUID
     * @param characteristic Characteristic UUID
     * @return Characteristic, or null if not found
     */
    @Nullable
    private BluetoothGattCharacteristic serviceCharacteristic(@NonNull final BluetoothGatt gatt, @NonNull final UUID service, @NonNull final UUID characteristic) {
        try {
            final BluetoothGattService bluetoothGattService = gatt.getService(service);
            if (null == bluetoothGattService) {
                return null;
            }
            //noinspection UnnecessaryLocalVariable
            final BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic);
            return bluetoothGattCharacteristic;
        } catch (Throwable e) {
            logger.fault("serviceCharacteristic, failure (service={},characteristic={})", service, characteristic, e);
            return null;
        }
    }

    /**
     * Establish the next task for a device, given its current state.
     * This is necessary because all BLE activities are asynchronous,
     * thus the BLEDevice object acts as a repository for collating all
     * device state and information updates from the asynchronous calls.
     * This function inspects the device state and information to
     * determine the next task to perform, if any, for the device
     * while it is connected. Please note, service and characteristic
     * discovery must be performed (cannot be cached) on the device
     * on each connection, thus it makes sense to do as much as possible
     * once a connection has been established with the target device.
     * @param device BLE device
     * @return Next task for the device
     */
    @NonNull
    private NextTask nextTaskForDevice(@NonNull final BLEDevice device) {
        // No task for devices marked as .ignore
        if (device.ignore()) {
            logger.debug("nextTaskForDevice, ignore (device={},ignoreExpiresIn={})", device, device.timeIntervalUntilIgnoreExpires());
            return NextTask.nothing;
        }
        // If marked as ignore but ignore has expired, change to unknown
        if (device.operatingSystem() == BLEDeviceOperatingSystem.ignore) {
            logger.debug("nextTaskForDevice, switching ignore to unknown (device={},reason=ignoreExpired)", device);
            device.operatingSystem(BLEDeviceOperatingSystem.unknown);
        }
        // No task for devices marked as receive only (no advert to connect to)
        if (device.receiveOnly()) {
            return NextTask.nothing;
        }
        // Device introspection to resolve device model if enabled and possible
        if (BLESensorConfiguration.deviceIntrospectionEnabled && device.supportsModelCharacteristic() && null == device.model()) {
            logger.debug("nextTaskForDevice (device={},task=readModel)", device);
            return NextTask.readModel;
        }
        // Device introspection to resolve device name if enabled and possible
        if (BLESensorConfiguration.deviceIntrospectionEnabled && device.supportsDeviceNameCharacteristic() && null == device.deviceName()) {
            logger.debug("nextTaskForDevice (device={},task=readDeviceName)", device);
            return NextTask.readDeviceName;
        }
        // Resolve or confirm operating system by reading payload which
        // triggers characteristic discovery to confirm the operating system
        if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown ||
                device.operatingSystem() == BLEDeviceOperatingSystem.ios_tbc) {
            logger.debug("nextTaskForDevice (device={},task=readPayload|OS)", device);
            return NextTask.readPayload;
        }
        // Immediate send is supported only if service and characteristics
        // have been discovered, and operating system has been confirmed
        if (null != device.immediateSendData()) {
            return NextTask.immediateSend;
        }
        // Get payload as top priority
        if (null == device.payloadData()) {
            logger.debug("nextTaskForDevice (device={},task=readPayload)", device);
            return NextTask.readPayload;
        }
        // Get payload update if required
        if (device.timeIntervalSinceLastPayloadDataUpdate().value > BLESensorConfiguration.payloadDataUpdateTimeInterval.value) {
            logger.debug("nextTaskForDevice (device={},task=readPayloadUpdate,timeIntervalSinceLastUpdate={})", device, device.timeIntervalSinceLastPayloadDataUpdate());
            return NextTask.readPayload;
        }
        if (device.protocolIsOpenTrace() && device.timeIntervalSinceLastPayloadDataUpdate().value > BLESensorConfiguration.interopOpenTracePayloadDataUpdateTimeInterval.value) {
            logger.debug("nextTaskForDevice (device={},task=readPayloadUpdate|OpenTrace,timeIntervalSinceLastUpdate={})", device, device.timeIntervalSinceLastPayloadDataUpdate());
            return NextTask.readPayload;
        }
        // Write payload, rssi and payload sharing data if this device cannot transmit
        if (!transmitter.isSupported()) {
            // Write payload data as top priority
            if (device.timeIntervalSinceLastWritePayload().value > TimeInterval.minutes(5).value) {
                logger.debug("nextTaskForDevice (device={},task=writePayload,elapsed={})", device, device.timeIntervalSinceLastWritePayload());
                return NextTask.writePayload;
            }
            // Write payload sharing data to iOS device if there is data to be shared (alternate between payload sharing and write RSSI)
            final PayloadSharingData payloadSharingData = database.payloadSharingData(device);
            if (device.operatingSystem() == BLEDeviceOperatingSystem.ios
                    && payloadSharingData.data.value.length > 0
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= TimeInterval.seconds(15).value
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= device.timeIntervalSinceLastWriteRssi().value) {
                logger.debug("nextTaskForDevice (device={},task=writePayloadSharing,dataLength={},elapsed={})", device, payloadSharingData.data.value.length, device.timeIntervalSinceLastWritePayloadSharing());
                return NextTask.writePayloadSharing;
            }
            // Write RSSI as frequently as reasonable (alternate between write RSSI and write payload)
            if (null != device.rssi() &&
                    device.timeIntervalSinceLastWriteRssi().value >= TimeInterval.seconds(15).value &&
                    (device.timeIntervalSinceLastWritePayload().value < BLESensorConfiguration.payloadDataUpdateTimeInterval.value
                        || device.timeIntervalSinceLastWriteRssi().value >= device.timeIntervalSinceLastWritePayload().value)
            ) {
                logger.debug("nextTaskForDevice (device={},task=writeRSSI,elapsed={})", device, device.timeIntervalSinceLastWriteRssi());
                return NextTask.writeRSSI;
            }
            // Write payload update if required
            if (device.timeIntervalSinceLastWritePayload().value > BLESensorConfiguration.payloadDataUpdateTimeInterval.value) {
                logger.debug("nextTaskForDevice (device={},task=writePayloadUpdate,elapsed={})", device, device.timeIntervalSinceLastWritePayload());
                return NextTask.writePayload;
            }
        }
        // Write payload sharing data to iOS
        if (device.operatingSystem() == BLEDeviceOperatingSystem.ios && !device.protocolIsOpenTrace()) {
            // Write payload sharing data to iOS device if there is data to be shared
            final PayloadSharingData payloadSharingData = database.payloadSharingData(device);
            if (device.operatingSystem() == BLEDeviceOperatingSystem.ios
                    && payloadSharingData.data.value.length > 0
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= TimeInterval.seconds(15).value) {
                logger.debug("nextTaskForDevice (device={},task=writePayloadSharing,dataLength={},elapsed={})", device, payloadSharingData.data.value.length, device.timeIntervalSinceLastWritePayloadSharing());
                return NextTask.writePayloadSharing;
            }
        }
        return NextTask.nothing;
    }

    /**
     * Given an open connection, perform the next task for the device.
     * Use this function to define the actual code for implementing
     * a task on the device (e.g. readPayload). The actual priority
     * of tasks is defined in the function nextTaskForDevice().
     * See function nextTaskForDevice() for additional design details.
     * @param gatt GATT
     */
    private void nextTask(@NonNull final BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        final NextTask nextTask = nextTaskForDevice(device);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("nextTask, no BLUETOOTH_CONNECT permission");
                return;
            }
        }
        switch (nextTask) {
            case readModel: {
                final BluetoothGattCharacteristic modelCharacteristic = device.modelCharacteristic();
                if (null == modelCharacteristic) {
                    logger.fault("nextTask failed (task=readModel,device={},reason=missingModelCharacteristic)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                bluetoothGattProxy.proxy(gatt);
                if (!gatt.readCharacteristic(modelCharacteristic)) {
                    logger.fault("nextTask failed (task=readModel,device={},reason=readModelCharacteristicFailed)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                logger.debug("nextTask (task=readModel,device={})", device);
                return; // => onCharacteristicRead | timeout
            }
            case readDeviceName: {
                final BluetoothGattCharacteristic deviceNameCharacteristic = device.deviceNameCharacteristic();
                if (null == deviceNameCharacteristic) {
                    logger.fault("nextTask failed (task=readDeviceName,device={},reason=missingDeviceNameCharacteristic)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                bluetoothGattProxy.proxy(gatt);
                if (!gatt.readCharacteristic(deviceNameCharacteristic)) {
                    logger.fault("nextTask failed (task=readDeviceName,device={},reason=readDeviceNameCharacteristicFailed)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                logger.debug("nextTask (task=readDeviceName,device={})", device);
                return; // => onCharacteristicRead | timeout
            }
            case readPayload: {
                final BluetoothGattCharacteristic payloadCharacteristic = device.payloadCharacteristic();
                if (null == payloadCharacteristic) {
                    logger.fault("nextTask failed (task=readPayload,device={},reason=missingPayloadCharacteristic)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                bluetoothGattProxy.proxy(gatt);
                // OpenTrace relies on MTU change to 512 to enable exchange of large payloads
                if (device.protocolIsOpenTrace()) {
                    gatt.requestMtu(512);
                    // Request MTU -> readCharacteristic
                    logger.debug("nextTask (task=readPayload|openTrace|requestMTU,device={})", device);
                    return; // => onCharacteristicRead | timeout
                }
                // HERALD handles fragmentation internally
                else if (!gatt.readCharacteristic(payloadCharacteristic)) {
                    logger.fault("nextTask failed (task=readPayload,device={},reason=readCharacteristicFailed)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                // TODO incorporate Android non-auth security patch once license confirmed
                logger.debug("nextTask (task=readPayload,device={})", device);
                return; // => onCharacteristicRead | timeout
            }
            case writePayload: {
                final PayloadData payloadData = transmitter.payloadData();
                //noinspection ConstantConditions
                if (null == payloadData || null == payloadData.value || 0 == payloadData.value.length) {
                    logger.fault("nextTask failed (task=writePayload,device={},reason=missingPayloadData)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                final Data data = SignalCharacteristicData.encodeWritePayload(transmitter.payloadData());
                logger.debug("nextTask (task=writePayload,device={},dataLength={})", device, data.value.length);
                writeSignalCharacteristic(gatt, NextTask.writePayload, data.value);
                return;
            }
            case writePayloadSharing: {
                final PayloadSharingData payloadSharingData = database.payloadSharingData(device);
                //noinspection ConstantConditions
                if (null == payloadSharingData) {
                    logger.fault("nextTask failed (task=writePayloadSharing,device={},reason=missingPayloadSharingData)", device);
                    gatt.disconnect();
                    return;
                }
                final Data data = SignalCharacteristicData.encodeWritePayloadSharing(payloadSharingData);
                logger.debug("nextTask (task=writePayloadSharing,device={},dataLength={})", device, data.value.length);
                writeSignalCharacteristic(gatt, NextTask.writePayloadSharing, data.value);
                return;
            }
            case writeRSSI: {
                final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
                if (null == signalCharacteristic) {
                    logger.fault("nextTask failed (task=writeRSSI,device={},reason=missingSignalCharacteristic)", device);
                    gatt.disconnect();
                    return;
                }
                final RSSI rssi = device.rssi();
                if (null == rssi) {
                    logger.fault("nextTask failed (task=writeRSSI,device={},reason=missingRssiData)", device);
                    gatt.disconnect();
                    return;
                }
                final Data data = SignalCharacteristicData.encodeWriteRssi(rssi);
                logger.debug("nextTask (task=writeRSSI,device={},dataLength={})", device, data.value.length);
                writeSignalCharacteristic(gatt, NextTask.writeRSSI, data.value);
                return;
            }
            case immediateSend: {
                final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
                if (null == signalCharacteristic) {
                    logger.fault("nextTask failed (task=immediateSend,device={},reason=missingSignalCharacteristic)", device);
                    gatt.disconnect();
                    return;
                }
                final Data data = device.immediateSendData(); // already encoded (arbitrary data with header)
                if (null == data) {
                    logger.fault("nextTask failed (task=immediateSend,device={},reason=missingImmediateSendData)", device);
                    gatt.disconnect();
                    return;
                }
                logger.debug("nextTask (task=immediateSend,device={},dataLength={})", device, data.value.length);
                writeSignalCharacteristic(gatt, NextTask.immediateSend, data.value);
                device.immediateSendData(null); // remove data to ensure it gets sent
                return;
            }
        }
        logger.debug("nextTask (task=nothing,device={})", device);
        gatt.disconnect();
    }

    private void writeSignalCharacteristic(@NonNull final BluetoothGatt gatt, @NonNull final NextTask task, @NonNull final byte[] data) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("writeSignalCharacteristic, no BLUETOOTH_CONNECT permission");
                return;
            }
        }
        if (null == signalCharacteristic) {
            logger.fault("writeSignalCharacteristic failed (task={},device={},reason=missingSignalCharacteristic)", task, device);
            gatt.disconnect();
            return;
        }
        //noinspection ConstantConditions
        if (null == data || 0 == data.length) {
            logger.fault("writeSignalCharacteristic failed (task={},device={},reason=missingData)", task, device);
            gatt.disconnect();
            return;
        }
        if (signalCharacteristic.getUuid().equals(BLESensorConfiguration.iosSignalCharacteristicUUID)) {
            device.signalCharacteristicWriteValue = data;
            device.signalCharacteristicWriteQueue = null;
            signalCharacteristic.setValue(data);
            signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            bluetoothGattProxy.proxy(gatt);
            if (!gatt.writeCharacteristic(signalCharacteristic)) {
                logger.fault("writeSignalCharacteristic to iOS failed (task={},device={},reason=writeCharacteristicFailed)", task, device);
                gatt.disconnect();
            } else {
                logger.debug("writeSignalCharacteristic to iOS (task={},dataLength={},device={})", task, data.length, device);
                // => onCharacteristicWrite
            }
            return;
        }
        if (signalCharacteristic.getUuid().equals(BLESensorConfiguration.androidSignalCharacteristicUUID)) {
            device.signalCharacteristicWriteValue = data;
            device.signalCharacteristicWriteQueue = fragmentDataByMtu(data);
            if (writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.failed) {
                logger.fault("writeSignalCharacteristic to Android failed (task={},device={},reason=writeCharacteristicFailed)", task, device);
                gatt.disconnect();
            } else {
                logger.debug("writeSignalCharacteristic to Android (task={},dataLength={},device={})", task, data.length, device);
                // => onCharacteristicWrite
            }
        }
    }

    private enum WriteAndroidSignalCharacteristicResult {
        moreToWrite, complete, failed
    }

    @NonNull
    private WriteAndroidSignalCharacteristicResult writeAndroidSignalCharacteristic(@NonNull final BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (null == signalCharacteristic) {
            logger.fault("writeAndroidSignalCharacteristic failed (device={},reason=missingSignalCharacteristic)", device);
            return WriteAndroidSignalCharacteristicResult.failed;
        }
        if (null == device.signalCharacteristicWriteQueue || 0 == device.signalCharacteristicWriteQueue.size()) {
            logger.debug("writeAndroidSignalCharacteristic completed (device={})", device);
            return WriteAndroidSignalCharacteristicResult.complete;
        }
        logger.debug("writeAndroidSignalCharacteristic (device={},queue={})", device, device.signalCharacteristicWriteQueue.size());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("writeAndroidSignalCharacteristic, no BLUETOOTH_CONNECT permission");
                return WriteAndroidSignalCharacteristicResult.failed;
            }
        }
        final byte[] data = device.signalCharacteristicWriteQueue.poll();
        signalCharacteristic.setValue(data);
        signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        bluetoothGattProxy.proxy(gatt);
        if (!gatt.writeCharacteristic(signalCharacteristic)) {
            logger.fault("writeAndroidSignalCharacteristic failed (device={},reason=writeCharacteristicFailed)", device);
            return WriteAndroidSignalCharacteristicResult.failed;
        } else {
            logger.debug("writeAndroidSignalCharacteristic (device={},remaining={})", device, device.signalCharacteristicWriteQueue.size());
            return WriteAndroidSignalCharacteristicResult.moreToWrite;
        }
    }

    /**
     * Split data into fragments, where each fragment has length <= mtu.
     * @param data Data
     * @return Data fragments
     */
    @NonNull
    private Queue<byte[]> fragmentDataByMtu(@NonNull final byte[] data) {
        final Queue<byte[]> fragments = new ConcurrentLinkedQueue<>();
        byte[] fragment;
        for (int i = 0; i < data.length; i += ConcreteBLEReceiver.defaultMTU) {
            fragment = new byte[Math.min(ConcreteBLEReceiver.defaultMTU, data.length - i)];
            System.arraycopy(data, i, fragment, 0, fragment.length);
            fragments.add(fragment);
        }
        return fragments;
    }

    /**
     * Interoperability with OpenTrace.
     * If nextTask=readPayload, rather than calling readCharacteristic directly, OpenTrace requires
     * MTU to be set to 512, before reading the actual payload. While HERALD handles fragmentation
     * internally, OpenTrace relies on setting the MTU to support reading of large payloads.
     * @param gatt GATT
     * @param mtu Actual MTU following change
     * @param status Change status
     */
    @Override
    public void onMtuChanged(@NonNull final BluetoothGatt gatt, final int mtu, final int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onMtuChanged (device={},status={})", device, bleStatus(status));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("onMtuChanged, no BLUETOOTH_CONNECT permission");
                return;
            }
        }
        final BluetoothGattCharacteristic characteristic = device.legacyPayloadCharacteristic();
        bluetoothGattProxy.proxy(gatt);
        if (BluetoothGatt.GATT_SUCCESS == status && null != characteristic && gatt.readCharacteristic(characteristic)) {
            logger.debug("nextTask (task=readPayload|legacy,device={})", device);
            return; // => onCharacteristicRead | timeout
        }
        gatt.disconnect();
    }

    /**
     * Write payload to legacy OpenTrace device
     * <br>
     * OpenTrace protocol : read payload -> write payload -> disconnect
     * @param gatt GATT
     */
    private void writeLegacyPayload(@NonNull final BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        if (device.protocolIsOpenTrace()) {
            final BluetoothGattCharacteristic characteristic = device.legacyPayloadCharacteristic();
            final LegacyPayloadData legacyPayloadData = payloadDataSupplier.legacyPayload(new PayloadTimestamp(), device);
            if (null != characteristic && null != legacyPayloadData) {
                characteristic.setValue(legacyPayloadData.value);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGattProxy.proxy(gatt);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        logger.fault("writeLegacyPayload, no BLUETOOTH_CONNECT permission");
                        return;
                    }
                }
                if (gatt.writeCharacteristic(characteristic)) {
                    // onCharacteristicWrite
                    logger.debug("writeLegacyPayload requested (device={})", device);
                    return;
                } else {
                    logger.fault("writeLegacyPayload failed (device={},reason=writeCharacteristicFailed)", device);
                }
            }
        }
        gatt.disconnect();
    }

    @Override
    public void onCharacteristicRead(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattCharacteristic characteristic, final int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
        logger.debug("onCharacteristicRead (device={},status={},characteristic={})", device, bleStatus(status), characteristic.getUuid().toString());
        if (characteristic.getUuid().equals(BLESensorConfiguration.payloadCharacteristicUUID)) {
            final PayloadData payloadData = (null != characteristic.getValue() ? new PayloadData(characteristic.getValue()) : null);
            if (success) {
                if (null != payloadData) {
                    logger.debug("onCharacteristicRead, read payload data success (device={},payload={})", device, payloadData.shortName());
                    device.payloadData(payloadData);
                    // TODO incorporate Android non-auth security patch once license confirmed
                } else {
                    logger.fault("onCharacteristicRead, read payload data failed, no data (device={})", device);
                }
            } else {
                logger.fault("onCharacteristicRead, read payload data failed (device={})", device);
            }
        } else if (characteristic.getUuid().equals(BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID)) {
            final LegacyPayloadData payloadData = (null != characteristic.getValue() ? new LegacyPayloadData(BLESensorConfiguration.interopOpenTraceServiceUUID, characteristic.getValue()) : null);
            if (success) {
                if (null != payloadData) {
                    logger.debug("onCharacteristicRead, read legacy payload data success (device={},payload={})", device, payloadData.shortName());
                    device.payloadData(payloadData);
                    // TODO incorporate Android non-auth security patch once license confirmed
                    // Write legacy payload data after read
                    writeLegacyPayload(gatt);
                    return;
                } else {
                    logger.fault("onCharacteristicRead, read legacy payload data failed, no data (device={})", device);
                }
            } else {
                logger.fault("onCharacteristicRead, read legacy payload data failed (device={})", device);
            }
        } else if (characteristic.getUuid().equals(BLESensorConfiguration.bluetoothDeviceInformationServiceModelCharacteristicUUID)) {
            final String model = characteristic.getStringValue(0);
            if (success) {
                if (null != model) {
                    logger.debug("onCharacteristicRead, read model data success (device={},model={})", device, model);
                    device.model(model);
                } else {
                    logger.fault("onCharacteristicRead, read model data failed, no data (device={})", device);
                }
            } else {
                logger.fault("onCharacteristicRead, read model data failed (device={})", device);
            }
        } else if (characteristic.getUuid().equals(BLESensorConfiguration.bluetoothGenericAccessServiceDeviceNameCharacteristicUUID)) {
            final String deviceName = characteristic.getStringValue(0);
            if (success) {
                if (null != deviceName) {
                    logger.debug("onCharacteristicRead, read deviceName data success (device={},deviceName={})", device, deviceName);
                    device.deviceName(deviceName);
                } else {
                    logger.fault("onCharacteristicRead, read deviceName data failed, no data (device={})", device);
                }
            } else {
                logger.fault("onCharacteristicRead, read deviceName data failed (device={})", device);
            }
        } else {
            logger.debug("onCharacteristicRead (device={},status={},characteristic={},value={})", device, bleStatus(status), characteristic.getUuid().toString(), characteristic.getStringValue(0));
        }
        nextTask(gatt);
    }

    @Override
    public void onCharacteristicWrite(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattCharacteristic characteristic, final int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onCharacteristicWrite (device={},status={})", device, bleStatus(status));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                logger.fault("onCharacteristicWrite, no BLUETOOTH_CONNECT permission");
                return;
            }
        }
        final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
        // OpenTrace payload characteristic write support
        if (characteristic.getUuid().equals(BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID)) {
            if (success) {
                logger.debug("onCharacteristicWrite, write OpenTrace payload success (device={})", device);
                device.registerWritePayload();
            } else {
                logger.fault("onCharacteristicWrite, write OpenTrace payload failed (device={})", device);
            }
            gatt.disconnect();
            return;
        }
        // Herald signal characteristic write support
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (signalCharacteristic != null && signalCharacteristic.getUuid().equals(BLESensorConfiguration.androidSignalCharacteristicUUID)) {
            if (success && writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.moreToWrite) {
                return;
            }
        }
        final SignalCharacteristicDataType signalCharacteristicDataType =
                (device.signalCharacteristicWriteValue == null ?
                        SignalCharacteristicDataType.unknown :
                        SignalCharacteristicData.detect(new Data(device.signalCharacteristicWriteValue)));
        if (signalCharacteristic != null) {
            signalCharacteristic.setValue(new byte[0]);
        }
        device.signalCharacteristicWriteValue = null;
        device.signalCharacteristicWriteQueue = null;
        switch (signalCharacteristicDataType) {
            case payload:
                if (success) {
                    logger.debug("onCharacteristicWrite, write payload success (device={})", device);
                    device.registerWritePayload();
                } else {
                    logger.fault("onCharacteristicWrite, write payload failed (device={})", device);
                }
                break;
            case rssi:
                if (success) {
                    logger.debug("onCharacteristicWrite, write RSSI success (device={})", device);
                    device.registerWriteRssi();
                } else {
                    logger.fault("onCharacteristicWrite, write RSSI failed (device={})", device);
                }
                break;
            case payloadSharing:
                if (success) {
                    logger.debug("onCharacteristicWrite, write payload sharing success (device={})", device);
                    device.registerWritePayloadSharing();
                } else {
                    logger.fault("onCharacteristicWrite, write payload sharing failed (device={})", device);
                }
                break;
            case immediateSend:
                //noinspection IfStatementWithIdenticalBranches
                if (success) {
                    logger.debug("onCharacteristicWrite, write immediate send data success (device={})", device);
                    device.immediateSendData(null);
                } else {
                    logger.fault("onCharacteristicWrite, write immediate send data failed (device={})", device);
                    // No retry for immediate send
                    device.immediateSendData(null);
                }
                // Close connection immediately upon completion of immediate send
                gatt.disconnect();
                // Do not perform any other tasks
                return;
            default:
                logger.fault("onCharacteristicWrite, write unknown data (device={},success={})", device, success);
                break;
        }
        nextTask(gatt);
    }

    // MARK:- Bluetooth code transformers

    @NonNull
    private static String bleStatus(final int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            return "GATT_SUCCESS";
        } else {
            return "GATT_FAILURE";
        }
    }

    @NonNull
    private static String bleState(final int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "STATE_CONNECTED";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "STATE_DISCONNECTED";
            default:
                return "UNKNOWN_STATE_" + state;
        }
    }

    @NonNull
    private static String onScanFailedErrorCodeToString(final int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "SCAN_FAILED_ALREADY_STARTED";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "SCAN_FAILED_INTERNAL_ERROR";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "SCAN_FAILED_FEATURE_UNSUPPORTED";
            default:
                return "UNKNOWN_ERROR_CODE_" + errorCode;
        }
    }
}
