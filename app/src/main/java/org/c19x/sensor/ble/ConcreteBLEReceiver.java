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
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import org.c19x.sensor.PayloadDataSupplier;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.BluetoothState;
import org.c19x.sensor.datatype.Callback;
import org.c19x.sensor.datatype.Data;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadTimestamp;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.TimeInterval;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcreteBLEReceiver implements BLEReceiver, BluetoothStateManagerDelegate {
    private final static int scanOnDurationMillis = 8000, scanOffDurationMillis = 4000;
    // Define fixed concurrent connection quota
    private final static int concurrentConnectionQuota = 5;
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEReceiver");
    private final ConcreteBLEReceiver self = this;
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final PayloadDataSupplier payloadDataSupplier;
    private final BLEDatabase database;
    private final BLETransmitter transmitter;
    private final Handler handler;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final Queue<ScanResult> scanResults = new ConcurrentLinkedQueue<>();
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private boolean enabled = false;

    /**
     * Receiver starts automatically when Bluetooth is enabled.
     */
    public ConcreteBLEReceiver(Context context, BluetoothStateManager bluetoothStateManager, PayloadDataSupplier payloadDataSupplier, BLEDatabase database, BLETransmitter transmitter) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.payloadDataSupplier = payloadDataSupplier;
        this.database = database;
        this.transmitter = transmitter;
        this.handler = new Handler(Looper.getMainLooper());
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        if (bluetoothLeScanner == null) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
        if (bluetoothLeScanner == null) {
            logger.fault("start denied, Bluetooth LE scanner unsupported");
            return;
        }
        if (bluetoothStateManager.state() != BluetoothState.poweredOn) {
            logger.fault("start denied, Bluetooth is not powered on");
            return;
        }
        if (scanCallback != null) {
            logger.fault("start denied, already started");
            return;
        }
        logger.debug("start");
        enabled = true;
        scan("start");
    }

    @Override
    public void stop() {
        logger.debug("stop");
        enabled = false;
        stopScan(new Callback<Boolean>() {
            @Override
            public void accept(Boolean success) {
                logger.debug("stopScan (success={})", success);
            }
        });
    }


    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {
        logger.debug("didUpdateState (state={})", didUpdateState);
        if (didUpdateState == BluetoothState.poweredOn) {
            start();
        } else if (didUpdateState == BluetoothState.poweredOff) {
            stop();
        }
    }

    public void scan(String source) {
        logger.debug("scan (source={},enabled={},on={}ms,off={}ms)", source, enabled, scanOnDurationMillis, scanOffDurationMillis);
        if (!enabled) {
            return;
        }
        startScan();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan(new Callback<Boolean>() {
                    @Override
                    public void accept(Boolean value) {
                        if (enabled) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    scan("onOffLoop");
                                }
                            }, scanOffDurationMillis);
                        }
                    }
                });
            }
        }, scanOnDurationMillis);
    }


    private void startScan() {
        if (bluetoothLeScanner == null) {
            logger.fault("startScan denied, Bluetooth LE scanner unsupported");
            return;
        }
        if (scanCallback != null) {
            logger.fault("startScan denied, already started");
            return;
        }
        if (!enabled) {
            logger.fault("startScan denied, not enabled");
            return;
        }
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    scanCallback = startScan(logger, bluetoothLeScanner, scanResults);
                } catch (Throwable e) {
                    logger.fault("startScan failed", e);
                }
            }
        });
    }

    private void stopScan(final Callback<Boolean> callback) {
        if (bluetoothLeScanner == null) {
            logger.fault("stopScan denied, Bluetooth LE scanner unsupported");
            return;
        }
        if (scanCallback == null) {
            logger.fault("stopScan denied, already stopped");
            return;
        }
        if (bluetoothStateManager.state() == BluetoothState.poweredOff) {
            logger.fault("stopScan denied, Bluetooth is powered off");
            return;
        }
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothLeScanner.stopScan(scanCallback);
                    scanCallback = null;
                    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    bluetoothAdapter.cancelDiscovery();
                    logger.debug("stopScan");
                    handleScanResults();
                    callback.accept(true);
                } catch (Throwable e) {
                    logger.fault("stopScan failed", e);
                    callback.accept(false);
                }
            }
        });
    }

    private static ScanCallback startScan(final SensorLogger logger, final BluetoothLeScanner bluetoothLeScanner, final Collection<ScanResult> scanResults) {
        final List<ScanFilter> filter = new ArrayList<>(2);
        filter.add(new ScanFilter.Builder().setManufacturerData(
                BLESensorConfiguration.manufacturerIdForApple, new byte[0], new byte[0]).build());
        filter.add(new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(BLESensorConfiguration.serviceUUID),
                new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                .build());
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(0)
                .build();
        final ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
//                logger.debug("onScanResult (result={})", result);
                scanResults.add(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                logger.fault("onScanFailed (error={})", onScanFailedErrorCodeToString(errorCode));
            }
        };
        bluetoothLeScanner.startScan(filter, settings, callback);
        logger.debug("Scan started (filter={},settings={})", filter, settings);
        return callback;
    }

    /**
     * Remove devices that have not been updated for over an hour, as the UUID is likely to have changed after being out of range for over 20 minutes, so it will require discovery.
     */
    private void taskRemoveExpiredDevices() {
        final List<BLEDevice> devicesToRemove = new ArrayList<>();
        for (BLEDevice device : database.devices()) {
            if (device.timeIntervalSinceLastUpdate().value > TimeInterval.hour.value) {
                devicesToRemove.add(device);
            }
        }
        for (BLEDevice device : devicesToRemove) {
            logger.debug("taskRemoveExpiredDevices (removed={})", device.identifier);
            database.delete(device.identifier);
            if (device.bluetoothGatt != null) {
                logger.debug("disconnect (source=taskRemoveExpiredDevices,identifier={})", device.identifier);
                device.bluetoothGatt.disconnect();
            }
        }
    }

    /**
     * Remove devices with the same payload data but different peripherals.
     */
    private void taskRemoveDuplicatePeripherals() {
        final Map<PayloadData, BLEDevice> index = new HashMap<>();
        for (BLEDevice device : database.devices()) {
            if (device.payloadData() == null) {
                continue;
            }
            final BLEDevice duplicate = index.get(device.payloadData());
            if (duplicate == null) {
                continue;
            }
            BLEDevice keeping = device;
            if (device.peripheral() != null && duplicate.peripheral() == null) {
                keeping = device;
            } else if (duplicate.peripheral() != null && device.peripheral() == null) {
                keeping = duplicate;
            } else if (device.lastUpdatedAt.getTime() > duplicate.lastUpdatedAt.getTime()) {
                keeping = device;
            } else {
                keeping = duplicate;
            }
            final BLEDevice discarding = (keeping == device ? duplicate : device);
            index.put(keeping.payloadData(), keeping);
            database.delete(discarding.identifier);
            logger.debug("taskRemoveDuplicatePeripherals (payload={},device={},duplicate={},keeping={}",
                    keeping.payloadData().description(),
                    device.identifier,
                    duplicate.identifier,
                    (keeping == device ? "former" : "latter"));
        }
    }


    /**
     * Process scan results to
     * 1. Create BLEDevice from scan result for new devices
     * 2. Read RSSI for the device
     * 3. Identify operating system for the device, if it is currently .unknown or .ignore
     */
    private Set<BLEDevice> taskProcessScanResults() {

        final List<ScanResult> scanResultList = new ArrayList<>(scanResults);
        final Set<BLEDevice> devices = new HashSet<>();
        final Set<String> processed = new HashSet<>();
        for (ScanResult scanResult : scanResultList) {
            // Device
            final BLEDevice device = database.device(scanResult.getDevice());
            devices.add(device);
            // RSSI
            device.rssi(new RSSI(scanResult.getRssi()));
            // Operating system
            final ScanRecord scanRecord = scanResult.getScanRecord();
            if ((device.operatingSystem() == BLEDeviceOperatingSystem.unknown || device.operatingSystem() == BLEDeviceOperatingSystem.ignore) && scanRecord != null) {
                // Test if service is being advertised
                boolean hasService = false;
                if (scanRecord.getServiceUuids() != null) {
                    for (ParcelUuid parcelUuid : scanRecord.getServiceUuids()) {
                        if (parcelUuid.getUuid().equals(BLESensorConfiguration.serviceUUID)) {
                            hasService = true;
                            break;
                        }
                    }
                }
                if (scanRecord.getManufacturerSpecificData(BLESensorConfiguration.manufacturerIdForApple) != null) {
                    // Apple device
                    if (!hasService) {
                        // Apple device in background mode
                        device.operatingSystem(BLEDeviceOperatingSystem.ios);
                    } else {
                        // Apple device in foreground mode advertising BLESensor service
                        device.operatingSystem(BLEDeviceOperatingSystem.ios);
                    }
                } else if (hasService) {
                    // Android device advertising C19X service
                    device.operatingSystem(BLEDeviceOperatingSystem.android);
                } else {
                    // Ignore other devices
                    device.operatingSystem(BLEDeviceOperatingSystem.ignore);
                }
            }
        }
        scanResults.clear();
        return devices;
    }

    /**
     * Manage connections to meet concurrent connection quota
     * 1. Discard long running connections
     * 2. Connect to readPayload or readPayloadSharing using surplus quota
     */
    private void taskConnect() {
        // Get connection status
        final List<BLEDevice> connected = new ArrayList<>();
        final List<BLEDevice> connecting = new ArrayList<>();
        final List<BLEDevice> disconnected = new ArrayList<>();
        for (BLEDevice device : database.devices()) {
            if (device.peripheral() == null) {
                continue;
            }
            switch (device.state()) {
                case connected: {
                    connected.add(device);
                    break;
                }
                case connecting: {
                    connecting.add(device);
                    break;
                }
                default: {
                    disconnected.add(device);
                    break;
                }
            }
        }
        logger.debug("taskConnect status (connected={},connecting={},disconnected={})", connected.size(), connecting.size(), disconnected.size());
        for (BLEDevice device : connected) {
            logger.debug("taskConnect connected (device={},operatingSystem={})", device.identifier, device.operatingSystem());
        }

        // Establish connections to keep
        // - Android connections are short lived and should be left to complete
        // - iOS connections for getting the payload data should be left to complete
        final List<BLEDevice> keep = new ArrayList<>();
        final List<BLEDevice> keepAndroid = new ArrayList<>();
        final List<BLEDevice> keepIos = new ArrayList<>();
        for (BLEDevice device : connected) {
            if (device.operatingSystem() == BLEDeviceOperatingSystem.ios) {
                keepIos.add(device);
            } else if (device.operatingSystem() == BLEDeviceOperatingSystem.android) {
                keepAndroid.add(device);
            }
        }
        keep.addAll(keepAndroid);
        keep.addAll(keepIos);
        logger.debug("taskConnect keep (android={},ios={})", keepAndroid.size(), keepIos.size());

        // Establish connections to discard
        // - All connections that are not disconnected within 10 seconds
        final List<BLEDevice> discard = new ArrayList<>();
        for (BLEDevice device : database.devices()) {
            // Has connection handle
            if (device.gatt == null) {
                continue;
            }
            // Not disconnected
            if (device.state() == BLEDeviceState.disconnected) {
                continue;
            }
            // Is connected for too long
            if (device.timeIntervalSinceLastStateUpdate().value < (new TimeInterval(10)).value) {
                continue;
            }
            discard.add(device);
        }
        // Sort by last updated time stamp (most recent first)
        Collections.sort(discard, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });

        // Discard connections to meet quota
        final int capacity = concurrentConnectionQuota - connected.size();
        if (capacity <= 0) {
            logger.fault("taskConnect quota exceeded, suspending new connections (connected={},keep={},quota={})", connected.size(), keep.size(), concurrentConnectionQuota);
            // Keep most recently updated devices first as devices that haven't been updated for a while may be going out of range
            final int surplusCapacity = concurrentConnectionQuota - keep.size();
            if (surplusCapacity > 0) {
                for (int i = surplusCapacity; i-- > 0 && discard.size() > 0; ) {
                    discard.remove(0);
                }
            }
            for (BLEDevice device : discard) {
                logger.debug("taskConnect|discard (device={},operatingSystem={})", device.identifier, device.operatingSystem());
                if (device.gatt != null) {
                    try {
                        device.state(BLEDeviceState.disconnecting);
                        device.gatt.disconnect();
                    } catch (Throwable e) {
                        logger.fault("taskConnect|discard, disconnect failed (device={},error={})", device.identifier, e);
                    }
                    try {
                        device.gatt.close();
                    } catch (Throwable e) {
                        logger.fault("taskConnect|discard, close failed (device={},error={})", device.identifier, e);
                    }
                    device.gatt = null;
                }
                device.state(BLEDeviceState.disconnected);
            }
        }

        // Establish pending connections
        // - New iOS or Android devices without payload data
        // - iOS and Android devices sorted by last payload shared at timestamp (least recent first)
        final List<BLEDevice> pending = new ArrayList<>();
        final List<BLEDevice> pendingNew = new ArrayList<>();
        final List<BLEDevice> pendingShare = new ArrayList<>();
        for (BLEDevice device : disconnected) {
            // iOS and Android devices only (unknown or ignore devices are resolved by taskProcessScanResults)
            if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android)) {
                continue;
            }
            // Seek payload as top priority
            if (device.payloadData() == null) {
                pendingNew.add(device);
            }
            // Invoke payload sharing if payloads have not been shared for a while
            else if (device.timeIntervalSinceLastPayloadShared().value > BLESensorConfiguration.payloadSharingTimeInterval.value) {
                pendingShare.add(device);
            }
        }
        Collections.sort(pendingShare, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d0.lastUpdatedAt.getTime(), d1.lastUpdatedAt.getTime());
            }
        });
        pending.addAll(pendingNew);
        pending.addAll(pendingShare);
        logger.debug("taskConnect pending (new={},share={},total={})", pendingNew.size(), pendingShare.size(), pending.size());
        final List<String> pendingQueue = new ArrayList<>();
        for (BLEDevice device : pending) {
            pendingQueue.add(device.operatingSystem() + ":" + device.timeIntervalSinceLastUpdate().value);
        }
        logger.debug("taskConnect pending (queue={})", pendingQueue);
        for (int i = 0; i < capacity && i < pending.size(); i++) {
            final BLEDevice device = pending.get(i);
            if (device.payloadData() == null) {
                logger.debug("taskConnect connect (goal=readPayload,device={})", device);
                readPayload(device);
            } else {
                logger.debug("taskConnect connect (goal=readPayloadSharing,device={})", device);
                readPayloadSharing(device);
            }
        }
    }

    /**
     * Write RSSI and payload data to central via signal characteristic if this device cannot transmit.
     */
    private void taskWriteBack(final Set<BLEDevice> devices) {
        logger.debug("taskWriteBack (transmitter={})", transmitter.isSupported());
        if (transmitter.isSupported()) {
            return;
        }
        // Establish surplus concurrent connection capacity
        final List<BLEDevice> connections = new ArrayList<>();
        for (BLEDevice device : database.devices()) {
            if (device.state() == BLEDeviceState.connected || device.state() == BLEDeviceState.connecting) {
                connections.add(device);
            }
        }
        final int capacity = concurrentConnectionQuota - connections.size();
        if (capacity <= 0) {
            return;
        }
        // Prioritise devices to write payload (least recent first)
        // - RSSI data is always fresh enough given the devices are passed in from taskProcessScanResults
        final List<BLEDevice> pending = new ArrayList<>();
        for (BLEDevice device : devices) {
            if (device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.operatingSystem() == BLEDeviceOperatingSystem.android) {
                pending.add(device);
            }
        }
        Collections.sort(pending, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.timeIntervalSinceLastWriteBack().value, d0.timeIntervalSinceLastWriteBack().value);
            }
        });
        // Initiate write back
        final List<String> pendingQueue = new ArrayList<>();
        for (BLEDevice device : pending) {
            pendingQueue.add(device.operatingSystem() + ":" + device.timeIntervalSinceLastWriteBack().value);
        }
        logger.debug("taskWriteBack pending (capacity={},pending={},queue={})", capacity, pending.size(), pendingQueue);
        for (int i = 0; i < capacity && i < pending.size(); i++) {
            final BLEDevice device = pending.get(i);
            logger.debug("taskWriteBack connect (goal=writePayload,device={})", device);
            writePayload(device);
        }
    }

    private void handleScanResults() {
        taskRemoveExpiredDevices();
        taskRemoveDuplicatePeripherals();
        final Set<BLEDevice> devices = taskProcessScanResults();
        taskConnect();
        taskWriteBack(devices);
    }

    private void readPayload(final BLEDevice device) {
        logger.debug("readPayload (device={})", device);
        connect(device, "readPayload", BLESensorConfiguration.payloadCharacteristicUUID, new Callback<byte[]>() {
            @Override
            public void accept(byte[] value) {
                final PayloadData payloadData = new PayloadData(value);
                device.payloadData(payloadData);
            }
        }, null);
    }

    private void readPayloadSharing(final BLEDevice device) {
        logger.debug("readPayloadSharing (device={})", device);
        connect(device, "readPayloadSharing", BLESensorConfiguration.payloadSharingCharacteristicUUID, new Callback<byte[]>() {
            @Override
            public void accept(byte[] value) {
                final List<PayloadData> payloadSharingData = payloadDataSupplier.payload(new Data(value));
                device.payloadSharingData(payloadSharingData);
            }
        }, null);
    }

    private void writePayload(final BLEDevice device) {
        logger.debug("writePayload (device={})", device);
        // Establish OS and characteristic
        UUID characteristicUUID = null;
        if (device.operatingSystem() == BLEDeviceOperatingSystem.ios) {
            characteristicUUID = BLESensorConfiguration.iosSignalCharacteristicUUID;
        } else if (device.operatingSystem() == BLEDeviceOperatingSystem.android) {
            characteristicUUID = BLESensorConfiguration.androidSignalCharacteristicUUID;
        }
        if (characteristicUUID == null) {
            logger.fault("writePayload denied, operating system must be iOS or Android (device={},operatingSystem={})", device, device.operatingSystem());
            return;
        }
        // Establish RSSI
        final RSSI rssi = device.rssi();
        if (rssi == null) {
            logger.fault("writePayload denied, missing RSSI (device={})", device);
            return;
        }
        // Establish payload
        final PayloadData payloadData = payloadDataSupplier.payload(new PayloadTimestamp());
        if (payloadData == null) {
            logger.fault("writePayload denied, missing payload data (device={})", device);
            return;
        }
        // Establish payload bundle
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4 + payloadData.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(rssi.value);
        byteBuffer.position(4);
        byteBuffer.put(payloadData.value);
        final byte[] writeData = byteBuffer.array();
        logger.fault("writePayload (device={},rssi={},payload={})", device, rssi, payloadData.description());
        connect(device, "writePayload", characteristicUUID, null, writeData);
    }

    // MARK:- BLEDevice connection

    /// Connect device and perform read/write operation on characteristic
    private void connect(final BLEDevice device, final String task, final UUID characteristicUUID, final Callback<byte[]> readData, final byte[] writeData) {
        if (device.peripheral() == null) {
            return;
        }
        if (readData != null && writeData != null) {
            logger.fault("task {} denied, cannot read and write at the same time (device={})", task, device.identifier);
            return;
        }
        final CountDownLatch blocking = new CountDownLatch(1);
        final Callback<String> disconnect = new Callback<String>() {
            @Override
            public void accept(String source) {
                logger.debug("task {}, disconnect (source={},device={})", task, source, device.identifier);
                if (device.gatt != null) {
                    try {
                        device.state(BLEDeviceState.disconnecting);
                        device.gatt.disconnect();
                    } catch (Throwable e) {
                        logger.fault("task {}, disconnect failed (device={},error={})", task, device, e);
                    }
                    try {
                        device.gatt.close();
                    } catch (Throwable e) {
                        logger.fault("task {}, close failed (device={},error={})", task, device, e);
                    }
                    device.gatt = null;
                }
                device.state(BLEDeviceState.disconnected);
                logger.debug("task {}, disconnected (source={},device={})", task, source, device.identifier);
                blocking.countDown();
            }
        };
        final BluetoothGattCallback callback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                logger.debug("task {}, onConnectionStateChange (device={},status={},state={})", task, device.identifier, status, onConnectionStatusChangeStateToString(newState));
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTING: {
                        device.state(BLEDeviceState.connecting);
                        break;
                    }
                    case BluetoothProfile.STATE_CONNECTED: {
                        logger.debug("task {}, didConnect (device={})", device.identifier);
                        device.state(BLEDeviceState.connected);
                        device.gatt = gatt;
                        gatt.discoverServices();
                        break;
                    }
                    case BluetoothProfile.STATE_DISCONNECTING: {
                        device.state(BLEDeviceState.disconnecting);
                        break;
                    }
                    case BluetoothProfile.STATE_DISCONNECTED: {
                        disconnect.accept("onConnectionStateChange");
                        break;
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                final BluetoothGattService service = gatt.getService(BLESensorConfiguration.serviceUUID);
                if (service == null) {
                    logger.debug("task {}, onServicesDiscovered, BLESensor service not found (device={})", task, device.identifier);
                    if (device.operatingSystem() != BLEDeviceOperatingSystem.unknown) {
                        device.operatingSystem(BLEDeviceOperatingSystem.ignore);
                    }
                    disconnect.accept("onServicesDiscovered|serviceNotFound");
                    return;
                }
                final BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
                if (characteristic != null) {
                    if (readData != null) {
                        logger.debug("task {}, readCharacteristic (device={})", task, device.identifier);
                        if (!gatt.readCharacteristic(characteristic)) {
                            disconnect.accept("onServicesDiscovered|readCharacteristicFailed");
                        }
                    } else if (writeData != null) {
                        logger.debug("task {}, writeCharacteristic (device={})", task, device.identifier);
                        characteristic.setValue(writeData);
                        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        if (!gatt.writeCharacteristic(characteristic)) {
                            disconnect.accept("onServicesDiscovered|writeCharacteristicFailed");
                        }
                    } else {
                        // This should not be possible as read=null and write=null is check earlier
                        disconnect.accept("onServicesDiscovered|noReadNorWrite");
                    }
                } else {
                    disconnect.accept("onServicesDiscovered|characteristicNotFound");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                logger.debug("task {}, onCharacteristicRead (device={},success={},value={})",
                        task, device.identifier,
                        (status == BluetoothGatt.GATT_SUCCESS),
                        (characteristic.getValue() == null ? "NULL" : characteristic.getValue().length));
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getValue() != null) {
                    readData.accept(characteristic.getValue());
                }
                disconnect.accept("onCharacteristicRead");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                logger.debug("task {}, onCharacteristicWrite (device={},success={})", task, device.identifier, (status == BluetoothGatt.GATT_SUCCESS));
                disconnect.accept("onCharacteristicWrite");
            }
        };
        logger.debug("task {}, connect (device={})", device);
        device.state(BLEDeviceState.connecting);
        try {
            device.gatt = device.peripheral().connectGatt(context, false, callback);
            if (device.gatt == null) {
                disconnect.accept("connect|noGatt");
            } else {
                try {
                    blocking.await(10, TimeUnit.SECONDS);
                } catch (Throwable e) {
                    logger.debug("task {}, timeout (device={})", task, device.identifier);
                    disconnect.accept("connect|timeout");
                }
            }
        } catch (Throwable e) {
            logger.fault("task {}, connect failed (device={},error={})", device, e);
            disconnect.accept("connect|noGatt");
        }
    }

    // MARK:- Bluetooth code transformers

    private static String onCharacteristicWriteStatusToString(final int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "GATT_SUCCESS";
            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                return "GATT_CONNECTION_CONGESTED";
            case BluetoothGatt.GATT_FAILURE:
                return "GATT_FAILURE";
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                return "GATT_INSUFFICIENT_AUTHENTICATION";
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                return "GATT_INSUFFICIENT_ENCRYPTION";
            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                return "GATT_INVALID_ATTRIBUTE_LENGTH";
            case BluetoothGatt.GATT_INVALID_OFFSET:
                return "GATT_INVALID_OFFSET";
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                return "GATT_READ_NOT_PERMITTED";
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                return "GATT_REQUEST_NOT_SUPPORTED";
            case BluetoothGatt.GATT_SERVER:
                return "GATT_SERVER";
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                return "GATT_WRITE_NOT_PERMITTED";
            default:
                return "UNKNOWN_STATUS_" + status;
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

    private static String onConnectionStatusChangeStateToString(final int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "STATE_CONNECTED";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "STATE_DISCONNECTED";
            default:
                return "UNKNOWN_STATE_" + state;
        }
    }
//
//    private final void taskProcessDevice(final BLEDevice device) {
//        logger.debug("taskProcessDevice (device={})", device);
//
//        if (device.operatingSystem() == BLEDeviceOperatingSystem.ignore) {
//            logger.debug("taskProcessDevice, ignore (device={})", device);
//            return;
//        }
//
//        if (device.payloadData() != null) {
//            logger.debug("taskProcessDevice, has payload, skipping (device={})", device);
//            return;
//        }
//
//        // Beacon missing beacon code or peripheral needs to transmit beacon code to central
//        final CompletableFuture<PayloadData> future = new CompletableFuture<>();
//        final AtomicBoolean gattOpen = new AtomicBoolean(true);
//        final BluetoothGattCallback callback = new BluetoothGattCallback() {
//            private PayloadData payloadData = null;
//
//            @Override
//            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                logger.debug("GATT client connection state change (device={},status={},newState={})",
//                        gatt.getDevice(), status, onConnectionStatusChangeStateToString(newState));
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    gatt.discoverServices();
//                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    if (gattOpen.compareAndSet(true, false)) {
//                        gatt.close();
//                        logger.debug("GATT client closed");
//                    }
//                    future.complete(payloadData);
//                }
//            }
//
//            @Override
//            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                logger.debug("GATT client discovered services (device={},status={})", device, status);
////                for (BluetoothGattService service : gatt.getServices()) {
////                    logger.debug("GATT client discovered service (device={},status={},uuid={})", device, status, service.getUuid());
////                }
//                final BluetoothGattService service = gatt.getService(BLESensorConfiguration.serviceUUID);
//                if (service == null) {
//                    logger.debug("GATT client C19X service not found (device={})", device);
//                    device.operatingSystem(BLEDeviceOperatingSystem.ignore);
//                    gatt.close();
//                    future.complete(payloadData);
//                    return;
//                }
//                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
//                    logger.debug("GATT client discovered characteristic (device={},characteristic={})", device, characteristic.getUuid());
//                    if (characteristic.getUuid().equals(BLESensorConfiguration.payloadCharacteristicUUID) && device.payloadData() == null) {
//                        logger.debug("GATT client discovered payload characteristic (device={})", gatt.getDevice());
//                        gatt.readCharacteristic(characteristic);
//                        return;
////                        if (!transmitter.isSupported()) {
////                            Logger.debug(tag, "GATT client not associated with transmitter, sending data instead");
////                            final BeaconCode beaconCode = transmitter.beaconCode();
////                            final ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
////                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
////                            byteBuffer.putLong(0, beaconCode.value);
////                            byteBuffer.putInt(Long.BYTES, beacon.getRssi().value);
////                            characteristic.setValue(byteBuffer.array());
////                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
////                            final boolean writeInitSuccess = gatt.writeCharacteristic(characteristic);
////                            Logger.debug(tag, "GATT client initiated write (device={},receiverBeaconCode={},rssi={},success={})", gatt.getDevice(), beaconCode, beacon.getRssi().value, writeInitSuccess);
////                            // GATT close on write complete
////                            return;
////                        } else {
////                            gatt.disconnect();
////                            future.complete(centralBeaconCode);
////                        }
//                    }
//                }
//                gatt.disconnect();
//                future.complete(payloadData);
//            }
//
//            @Override
//            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                logger.debug("GATT client read characteristic (device={},success={},status={})", gatt.getDevice(), (status == BluetoothGatt.GATT_SUCCESS), onCharacteristicWriteStatusToString(status));
//                payloadData = new PayloadData(characteristic.getValue());
//                gatt.disconnect();
//                future.complete(payloadData);
//            }
//
//            @Override
//            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                logger.debug("GATT client wrote characteristic (device={},success={},status={})", gatt.getDevice(), (status == BluetoothGatt.GATT_SUCCESS), onCharacteristicWriteStatusToString(status));
//                gatt.disconnect();
//                future.complete(payloadData);
//            }
//        };
//        final BluetoothGatt gatt = device.peripheral().connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
//        final String gattDevice = (gatt != null && gatt.getDevice() != null ? gatt.getDevice().toString() : "null");
//        try {
//            final PayloadData payloadData = future.get(5, TimeUnit.SECONDS);
//            if (payloadData != null) {
//                logger.debug("GATT client read payload (payload={})", payloadData.description());
//                device.payloadData(payloadData);
//            }
//        } catch (TimeoutException e) {
//            logger.fault("GATT client timeout (device={})", gattDevice);
//        } catch (Throwable e) {
//            logger.fault("GATT client exception (device={})", gattDevice, e);
//        }
//        if (gattOpen.compareAndSet(true, false)) {
//            try {
//                gatt.disconnect();
//            } catch (Throwable e) {
//                logger.fault("GATT client disconnect exception (device={})", gattDevice, e);
//            }
//            gatt.close();
//            logger.fault("GATT client closed (device={})", gattDevice);
//        }
//    }
//
}
