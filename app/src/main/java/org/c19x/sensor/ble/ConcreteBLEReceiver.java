package org.c19x.sensor.ble;

import android.bluetooth.BluetoothAdapter;
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
import android.os.ParcelUuid;

import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.BluetoothState;
import org.c19x.sensor.datatype.Callback;
import org.c19x.sensor.datatype.Data;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadSharingData;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.Sample;
import org.c19x.sensor.datatype.SensorError;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.SignalCharacteristicData;
import org.c19x.sensor.datatype.SignalCharacteristicDataType;
import org.c19x.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLEReceiver extends BluetoothGattCallback implements BLEReceiver, BluetoothStateManagerDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEReceiver");
    // Scan ON/OFF/PROCESS durations
    private final static long scanOnDurationMillis = TimeInterval.seconds(4).millis();
    private final static long scanRestDurationMillis = TimeInterval.seconds(1).millis();
    private final static long scanProcessDurationMillis = TimeInterval.seconds(60).millis();
    private final static long scanOffDurationMillis = TimeInterval.seconds(2).millis();
    private final static long timeToConnectDeviceLimitMillis = TimeInterval.seconds(12).millis();
    private final static Sample timeToConnectDevice = new Sample();
    private final static Sample timeToProcessDevice = new Sample();
    private final static int defaultMTU = 20;
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final BLEDatabase database;
    private final BLETransmitter transmitter;
    private final BLETimer timer;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final Queue<ScanResult> scanResults = new ConcurrentLinkedQueue<>();

    private enum NextTask {
        nothing, readPayload, writePayload, writeRSSI, writePayloadSharing
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //logger.debug("onScanResult (result={})", result);
            scanResults.add(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            //logger.debug("onBatchScanResults (results=)", results.size());
            for (ScanResult scanResult : results) {
                onScanResult(0, scanResult);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            logger.fault("onScanFailed (error={})", onScanFailedErrorCodeToString(errorCode));
            super.onScanFailed(errorCode);
            final SensorError sensorError = new SensorError("BLEReceiver.onScanFailed(" + onScanFailedErrorCodeToString(errorCode) + ")");
            for (SensorDelegate delegate : delegates) {
                delegate.sensor(SensorType.BLE, sensorError);
            }
        }
    };

    /**
     * Receiver starts automatically when Bluetooth is enabled.
     */
    public ConcreteBLEReceiver(Context context, BluetoothStateManager bluetoothStateManager, BLEDatabase database, BLETransmitter transmitter) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.database = database;
        this.transmitter = transmitter;
        this.timer = new BLETimer(context);
        timer.timerTask(new ScanLoopTask());
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
    }

    // MARK:- BLEReceiver

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        logger.debug("start");
        // startScanLoop is started by Bluetooth state
    }

    @Override
    public void stop() {
        logger.debug("stop");
        // startScanLoop is stopped by Bluetooth state
    }

    // MARK:- BluetoothStateManagerDelegate

    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {
        logger.debug("didUpdateState (state={})", didUpdateState);
    }

    // MARK:- Scan loop for startScan-wait-stopScan-processScanResults-wait-repeat

    private enum ScanLoopState {
        scanStarting, scanStarted, scanStopping, scanStopped, processing, processed
    }

    private class ScanLoopTask implements Callback<Long> {
        private ScanLoopState scanLoopState = ScanLoopState.processed;
        private long lastStateChangeAt = System.currentTimeMillis();

        private void state(final long now, ScanLoopState state) {
            final long elapsed = now - lastStateChangeAt;
            logger.debug("ScanLoop, state change (from={},to={},elapsed={}ms)", scanLoopState, state, elapsed);
            this.scanLoopState = state;
            lastStateChangeAt = now;
        }

        private long timeSincelastStateChange(final long now) {
            return now - lastStateChangeAt;
        }

        private BluetoothLeScanner bluetoothLeScanner() {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                logger.fault("ScanLoop denied, Bluetooth adapter unavailable");
                return null;
            }
            final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                logger.fault("ScanLoop denied, Bluetooth LE scanner unavailable");
                return null;
            }
            return bluetoothLeScanner;
        }

        @Override
        public void accept(final Long now) {
            switch (scanLoopState) {
                case processed: {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= scanOffDurationMillis) {
                            logger.debug("scanLoopTask, start scan (process={}ms)", period);
                            final BluetoothLeScanner bluetoothLeScanner = bluetoothLeScanner();
                            if (bluetoothLeScanner == null) {
                                logger.fault("scanLoopTask, start scan denied, Bluetooth LE scanner unavailable");
                                return;
                            }
                            state(now, ScanLoopState.scanStarting);
                            startScan(bluetoothLeScanner, new Callback<Boolean>() {
                                @Override
                                public void accept(Boolean value) {
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
                        if (bluetoothLeScanner == null) {
                            logger.fault("scanLoopTask, stop scan denied, Bluetooth LE scanner unavailable");
                            return;
                        }
                        state(now, ScanLoopState.scanStopping);
                        stopScan(bluetoothLeScanner, new Callback<Boolean>() {
                            @Override
                            public void accept(Boolean value) {
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
                                public void accept(Boolean value) {
                                    state(now, ScanLoopState.processed);
                                }
                            });
                        }
                    }
                    break;
                }
            }
        }
    }


    /// Get BLE scanner and start scan
    private void startScan(final BluetoothLeScanner bluetoothLeScanner, final Callback<Boolean> callback) {
        logger.debug("startScan");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    scanForPeripherals(bluetoothLeScanner);
                    logger.debug("startScan successful");
                    if (callback != null) {
                        callback.accept(true);
                    }
                } catch (Throwable e) {
                    logger.fault("startScan failed", e);
                    if (callback != null) {
                        callback.accept(false);
                    }
                }
            }
        });
    }


    /// Scan for devices advertising sensor service and all Apple devices as
    // iOS background advert does not include service UUID. There is a risk
    // that the sensor will spend time communicating with Apple devices that
    // are not running the sensor code repeatedly, but there is no reliable
    // way of filtering this as the service may be absent only because of
    // transient issues. This will be handled in taskConnect.
    private void scanForPeripherals(final BluetoothLeScanner bluetoothLeScanner) {
        logger.debug("scanForPeripherals");
        final List<ScanFilter> filter = new ArrayList<>(2);
        filter.add(new ScanFilter.Builder().setManufacturerData(
                BLESensorConfiguration.manufacturerIdForApple, new byte[0], new byte[0]).build());
        filter.add(new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(BLESensorConfiguration.serviceUUID),
                new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                .build());
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        bluetoothLeScanner.startScan(filter, settings, scanCallback);
    }

    private void processScanResults(final Callback<Boolean> callback) {
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

    /// Get BLE scanner and stop scan
    private void stopScan(final BluetoothLeScanner bluetoothLeScanner, final Callback<Boolean> callback) {
        logger.debug("stopScan");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothLeScanner.stopScan(scanCallback);
                    logger.debug("stopScan, stopped scanner");
                } catch (Throwable e) {
                    logger.fault("stopScan warning, bluetoothLeScanner.stopScan error", e);
                }
                try {
                    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null) {
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

    /// Process scan results.
    private void processScanResults() {
        final long t0 = System.currentTimeMillis();
        logger.debug("processScanResults (results={})", scanResults.size());
        // Identify devices discovered in last scan
        final List<BLEDevice> didDiscover = didDiscover();
        taskRemoveExpiredDevices();
        taskCorrectConnectionStatus();
        taskConnect(didDiscover);
        final long t1 = System.currentTimeMillis();
        logger.debug("processScanResults (results={},devices={},elapsed={}ms)", scanResults.size(), didDiscover.size(), (t1 - t0));
    }

    // MARK:- didDiscover

    /**
     * Process scan results to ...
     * 1. Create BLEDevice from scan result for new devices
     * 2. Read RSSI
     * 3. Identify operating system where possible
     */
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
        for (ScanResult scanResult : scanResultList) {
            final BLEDevice device = database.device(scanResult.getDevice());
            device.registerDiscovery();
            if (deviceSet.add(device)) {
                logger.debug("didDiscover (device={})", device);
                devices.add(device);
            }
            // Read RSSI from scan result
            device.rssi(new RSSI(scanResult.getRssi()));
            // Identify operating system from scan record where possible
            // - Sensor service found + Manufacturer is Apple -> iOS (Foreground)
            // - Sensor service found + Manufacturer not Apple -> Android
            // - Sensor service not found + Manufacturer is Apple -> iOS (Background) or Apple device not advertising sensor service, to be resolved later
            // - Sensor service not found + Manufacturer not Apple -> Ignore (shouldn't be possible as we are scanning for Apple or with service)
            final boolean hasSensorService = hasSensorService(scanResult);
            final boolean isAppleDevice = isAppleDevice(scanResult);
            if (hasSensorService && isAppleDevice) {
                // Definitely iOS device offering sensor service in foreground mode
                device.operatingSystem(BLEDeviceOperatingSystem.ios);
            } else if (hasSensorService && !isAppleDevice) {
                // Definitely Android device offering sensor service
                device.operatingSystem(BLEDeviceOperatingSystem.android_tbc);
            } else if (!hasSensorService && isAppleDevice) {
                // Possibly an iOS device offering sensor service in background mode,
                // can't be sure without additional checks after connection, so
                // only set operating system if it is unknown to offer a guess.
                if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown) {
                    device.operatingSystem(BLEDeviceOperatingSystem.ios_tbc);
                }
            } else {
                // Sensor service not found + Manufacturer not Apple should be impossible
                // as we are scanning for devices with sensor service or Apple device.
                logger.fault("didDiscover, invalid non-Apple device without sensor service (device={})", device);
                device.operatingSystem(BLEDeviceOperatingSystem.ignore);
            }
        }
        return devices;
    }

    /// Does scan result include advert for sensor service?
    private static boolean hasSensorService(final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            return false;
        }
        final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (serviceUuids == null || serviceUuids.size() == 0) {
            return false;
        }
        for (ParcelUuid serviceUuid : serviceUuids) {
            if (serviceUuid.getUuid().equals(BLESensorConfiguration.serviceUUID)) {
                return true;
            }
        }
        return false;
    }

    /// Does scan result indicate device was manufactured by Apple?
    private static boolean isAppleDevice(final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            return false;
        }
        final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.manufacturerIdForApple);
        return data != null;
    }

    // MARK:- House keeping tasks

    /// Remove devices that have not been updated for over 15 minutes, as the UUID
    // is likely to have changed after being out of range for over 20 minutes,
    // so it will require discovery. Discovery is fast and cheap on Android.
    private void taskRemoveExpiredDevices() {
        final List<BLEDevice> devicesToRemove = new ArrayList<>();
        for (BLEDevice device : database.devices()) {
            if (device.timeIntervalSinceLastUpdate().value > TimeInterval.minutes(15).value) {
                devicesToRemove.add(device);
            }
        }
        for (BLEDevice device : devicesToRemove) {
            logger.debug("taskRemoveExpiredDevices (remove={})", device);
            database.delete(device.identifier);
        }
    }

    /// Connections should not be held for more than 1 minute, likely to have not received onConnectionStateChange callback.
    private void taskCorrectConnectionStatus() {
        for (BLEDevice device : database.devices()) {
            if (device.state() == BLEDeviceState.connected && device.timeIntervalSinceConnected().value > TimeInterval.minute.value) {
                logger.debug("taskCorrectConnectionStatus (device={})", device);
                device.state(BLEDeviceState.disconnected);
            }
        }
    }


    // MARK:- Connect task

    private void taskConnect(final List<BLEDevice> discovered) {
        // Clever connection prioritisation is pointless here as devices
        // like the Samsung A10 and A20 changes mac address on every scan
        // call, so optimising new device handling is more effective.
        final long timeStart = System.currentTimeMillis();
        int devicesProcessed = 0;
        for (BLEDevice device : discovered) {
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

    private void taskConnectDevice(final BLEDevice device) {
        if (device.state() == BLEDeviceState.connected) {
            logger.debug("taskConnectDevice, already connected to transmitter (device={})", device);
            return;
        }
        // Connect (timeout at 95% = 2 SD)
        final long timeConnect = System.currentTimeMillis();
        logger.debug("taskConnectDevice, connect (device={})", device);
        device.state(BLEDeviceState.connecting);
        final BluetoothGatt gatt = device.peripheral().connectGatt(context, false, this);
        if (gatt == null) {
            logger.fault("taskConnectDevice, connect failed (device={})", device);
            device.state(BLEDeviceState.disconnected);
            return;
        }
        // Wait for connection
        while (device.state() != BLEDeviceState.connected && device.state() != BLEDeviceState.disconnected && (System.currentTimeMillis() - timeConnect) < timeToConnectDeviceLimitMillis) {
            try {
                Thread.sleep(200);
            } catch (Throwable e) {
            }
        }
        if (device.state() != BLEDeviceState.connected) {
            logger.fault("taskConnectDevice, connect timeout (device={})", device);
            try {
                gatt.close();
            } catch (Throwable e) {
                logger.fault("taskConnectDevice, close failed (device={})", device, e);
            }
            return;
        } else {
            final long connectElapsed = System.currentTimeMillis() - timeConnect;
            // Add sample to adaptive connection timeout
            timeToConnectDevice.add(connectElapsed);
            logger.debug("taskConnectDevice, connected (device={},elapsed={}ms,statistics={})", device, connectElapsed, timeToConnectDevice);
        }
        // Wait for disconnection
        while (device.state() != BLEDeviceState.disconnected && (System.currentTimeMillis() - timeConnect) < scanProcessDurationMillis) {
            try {
                Thread.sleep(500);
            } catch (Throwable e) {
            }
        }
        boolean success = true;
        // Timeout connection if required, and always set state to disconnected
        if (device.state() != BLEDeviceState.disconnected) {
            logger.fault("taskConnectDevice, disconnect timeout (device={})", device);
            try {
                gatt.close();
            } catch (Throwable e) {
                logger.fault("taskConnectDevice, close failed (device={})", device, e);
            }
            success = false;
        }
        device.state(BLEDeviceState.disconnected);
        final long timeDisconnect = System.currentTimeMillis();
        final long timeElapsed = (timeDisconnect - timeConnect);
        if (success) {
            timeToProcessDevice.add(timeElapsed);
            logger.debug("taskConnectDevice, complete (success=true,device={},elapsed={}ms,statistics={})", device, timeElapsed, timeToProcessDevice);
        } else {
            logger.fault("taskConnectDevice, complete (success=false,device={},elapsed={}ms)", device, timeElapsed);
        }

    }

    // MARK:- BluetoothStateManagerDelegate

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState));
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            device.state(BLEDeviceState.connected);
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt.close();
            device.state(BLEDeviceState.disconnected);
            if (status != 0) {
                device.operatingSystem(BLEDeviceOperatingSystem.ignore);
            }
        } else {
            logger.debug("onConnectionStateChange (device={},status={},state={})", device, bleStatus(status), bleState(newState));
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onServicesDiscovered (device={},status={})", device, bleStatus(status));

        final BluetoothGattService service = gatt.getService(BLESensorConfiguration.serviceUUID);
        if (service == null) {
            logger.fault("onServicesDiscovered, missing sensor service (device={})", device);
            // Ignore device for a while unless it is a confirmed iOS or Android device
            if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                device.operatingSystem(BLEDeviceOperatingSystem.ignore);
            }
            gatt.disconnect();
            return;
        }

        logger.debug("onServicesDiscovered, found sensor service (device={})", device);

        device.invalidateCharacteristics();
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
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
            }
        }
        nextTask(gatt);
    }


    private NextTask nextTaskForDevice(final BLEDevice device) {
        // No task for devices marked as .ignore
        if (device.ignore()) {
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
        // Resolve or confirm operating system by reading payload which
        // triggers characteristic discovery to confirm the operating system
        if (device.operatingSystem() == BLEDeviceOperatingSystem.unknown ||
                device.operatingSystem() == BLEDeviceOperatingSystem.ios_tbc) {
            logger.debug("nextTaskForDevice (device={},task=readPayload|OS)", device);
            return NextTask.readPayload;
        }
        // Get payload as top priority
        if (device.payloadData() == null) {
            logger.debug("nextTaskForDevice (device={},task=readPayload)", device);
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
            // Write RSSI as frequently as reasonable
            if (device.rssi() != null && device.timeIntervalSinceLastWriteRssi().value >= TimeInterval.seconds(15).value) {
                logger.debug("nextTaskForDevice (device={},task=writeRSSI,elapsed={})", device, device.timeIntervalSinceLastWriteRssi());
                return NextTask.writeRSSI;
            }
        }
        // Write payload sharing data to iOS
        if (device.operatingSystem() == BLEDeviceOperatingSystem.ios) {
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

    private void nextTask(BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        final NextTask nextTask = nextTaskForDevice(device);
        switch (nextTask) {
            case readPayload: {
                final BluetoothGattCharacteristic payloadCharacteristic = device.payloadCharacteristic();
                if (payloadCharacteristic == null) {
                    logger.fault("nextTask failed (task=readPayload,device={},reason=missingPayloadCharacteristic)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                if (!gatt.readCharacteristic(payloadCharacteristic)) {
                    logger.fault("nextTask failed (task=readPayload,device={},reason=readCharacteristicFailed)", device);
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                logger.debug("nextTask (task=readPayload,device={})", device);
                return; // => onCharacteristicRead | timeout
            }
            case writePayload: {
                final PayloadData payloadData = transmitter.payloadData();
                if (payloadData == null || payloadData.value == null || payloadData.value.length == 0) {
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
                if (payloadSharingData == null) {
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
                if (signalCharacteristic == null) {
                    logger.fault("nextTask failed (task=writeRSSI,device={},reason=missingSignalCharacteristic)", device);
                    gatt.disconnect();
                    return;
                }
                final RSSI rssi = device.rssi();
                if (rssi == null) {
                    logger.fault("nextTask failed (task=writeRSSI,device={},reason=missingRssiData)", device);
                    gatt.disconnect();
                    return;
                }
                final Data data = SignalCharacteristicData.encodeWriteRssi(rssi);
                logger.debug("nextTask (task=writeRSSI,device={},dataLength={})", device, data.value.length);
                writeSignalCharacteristic(gatt, NextTask.writeRSSI, data.value);
                return;
            }
        }
        logger.debug("nextTask (task=nothing,device={})", device);
        gatt.disconnect();
    }

    private void writeSignalCharacteristic(BluetoothGatt gatt, NextTask task, byte[] data) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (signalCharacteristic == null) {
            logger.fault("writeSignalCharacteristic failed (task={},device={},reason=missingSignalCharacteristic)", task, device);
            gatt.disconnect();
            return;
        }
        if (data == null || data.length == 0) {
            logger.fault("writeSignalCharacteristic failed (task={},device={},reason=missingData)", task, device);
            gatt.disconnect();
            return;
        }
        if (signalCharacteristic.getUuid().equals(BLESensorConfiguration.iosSignalCharacteristicUUID)) {
            device.signalCharacteristicWriteValue = data;
            device.signalCharacteristicWriteQueue = null;
            signalCharacteristic.setValue(data);
            signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            if (!gatt.writeCharacteristic(signalCharacteristic)) {
                logger.fault("writeSignalCharacteristic to iOS failed (task={}},device={},reason=writeCharacteristicFailed)", task, device);
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
                logger.fault("writeSignalCharacteristic to Android failed (task={}},device={},reason=writeCharacteristicFailed)", task, device);
                gatt.disconnect();
            } else {
                logger.debug("writeSignalCharacteristic to Android (task={},dataLength={},device={})", task, data.length, device);
                // => onCharacteristicWrite
            }
            return;
        }
    }

    private enum WriteAndroidSignalCharacteristicResult {
        moreToWrite, complete, failed
    }

    private WriteAndroidSignalCharacteristicResult writeAndroidSignalCharacteristic(BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (signalCharacteristic == null) {
            logger.fault("writeAndroidSignalCharacteristic failed (device={},reason=missingSignalCharacteristic)", device);
            return WriteAndroidSignalCharacteristicResult.failed;
        }
        if (device.signalCharacteristicWriteQueue == null || device.signalCharacteristicWriteQueue.size() == 0) {
            logger.debug("writeAndroidSignalCharacteristic completed (device={})", device);
            return WriteAndroidSignalCharacteristicResult.complete;
        }
        logger.debug("writeAndroidSignalCharacteristic (device={},queue={})", device, device.signalCharacteristicWriteQueue.size());
        final byte[] data = device.signalCharacteristicWriteQueue.poll();
        signalCharacteristic.setValue(data);
        signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (!gatt.writeCharacteristic(signalCharacteristic)) {
            logger.fault("writeAndroidSignalCharacteristic failed (device={},reason=writeCharacteristicFailed)", device);
            return WriteAndroidSignalCharacteristicResult.failed;
        } else {
            logger.debug("writeAndroidSignalCharacteristic (device={},remaining={})", device, device.signalCharacteristicWriteQueue.size());
            return WriteAndroidSignalCharacteristicResult.moreToWrite;
        }
    }

    /// Split data into fragments, where each fragment has length <= mtu
    private Queue<byte[]> fragmentDataByMtu(byte[] data) {
        final Queue<byte[]> fragments = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < data.length; i += ConcreteBLEReceiver.defaultMTU) {
            final byte[] fragment = new byte[Math.min(ConcreteBLEReceiver.defaultMTU, data.length - i)];
            System.arraycopy(data, i, fragment, 0, fragment.length);
            fragments.add(fragment);
        }
        return fragments;
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
        logger.debug("onCharacteristicRead (device={},status={})", device, bleStatus(status));
        if (characteristic.getUuid().equals(BLESensorConfiguration.payloadCharacteristicUUID)) {
            final PayloadData payloadData = (characteristic.getValue() != null ? new PayloadData(characteristic.getValue()) : null);
            if (success) {
                if (payloadData != null) {
                    logger.debug("onCharacteristicRead, read payload data success (device={},payload={})", device, payloadData.shortName());
                    device.payloadData(payloadData);
                } else {
                    logger.fault("onCharacteristicRead, read payload data failed, no data (device={})", device);
                }
            } else {
                logger.fault("onCharacteristicRead, read payload data failed (device={})", device);
            }
        }
        nextTask(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        logger.debug("onCharacteristicWrite (device={},status={})", device, bleStatus(status));
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
        if (signalCharacteristic.getUuid().equals(BLESensorConfiguration.androidSignalCharacteristicUUID)) {
            if (success && writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.moreToWrite) {
                return;
            }
        }
        final SignalCharacteristicDataType signalCharacteristicDataType = SignalCharacteristicData.detect(new Data(device.signalCharacteristicWriteValue));
        signalCharacteristic.setValue(new byte[0]);
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
            default:
                logger.fault("onCharacteristicWrite, write unknown data (device={},success={})", device, success);
                break;
        }
        nextTask(gatt);
    }

    // MARK:- Bluetooth code transformers

    private static String bleStatus(final int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            return "GATT_SUCCESS";
        } else {
            return "GATT_FAILURE";
        }
    }

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
