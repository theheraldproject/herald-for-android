package org.c19x.sensor.ble;

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
import android.os.ParcelUuid;

import org.c19x.sensor.PayloadDataSupplier;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.BluetoothState;
import org.c19x.sensor.datatype.Callback;
import org.c19x.sensor.datatype.Data;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadSharingData;
import org.c19x.sensor.datatype.PayloadTimestamp;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.SignalCharacteristicData;
import org.c19x.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;

public class ConcreteBLETransmitter implements BLETransmitter, BluetoothStateManagerDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLETransmitter");
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final PayloadDataSupplier payloadDataSupplier;
    private final BLEDatabase database;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer bluetoothGattServer;
    private AdvertiseCallback advertiseCallback;
    private boolean enabled = false;

    /**
     * Transmitter starts automatically when Bluetooth is enabled.
     */
    public ConcreteBLETransmitter(Context context, BluetoothStateManager bluetoothStateManager, PayloadDataSupplier payloadDataSupplier, BLEDatabase database) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.payloadDataSupplier = payloadDataSupplier;
        this.database = database;
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        if (bluetoothLeAdvertiser == null) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported()) {
                bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            }
        }
        logger.debug("start");
        enabled = true;
        advertise("start");
    }

    private void advertise(String source) {
        logger.debug("advertise (source={},enabled={})", source, enabled);
        if (!enabled) {
            return;
        }
        startAdvertising();
    }

    @Override
    public void stop() {
        logger.debug("stop");
        enabled = false;
        stopAdvertising(new Callback<Boolean>() {
            @Override
            public void accept(Boolean success) {
                logger.debug("stopAdvertising (success={})", success);
            }
        });
    }

    private void stopAdvertising(final Callback<Boolean> callback) {
        if (bluetoothLeAdvertiser == null) {
            logger.fault("stopAdvertising denied, Bluetooth LE advertising unsupported");
            return;
        }
        if (advertiseCallback == null) {
            logger.fault("Already stopped");
            return;
        }
        if (bluetoothStateManager.state() == BluetoothState.poweredOff) {
            logger.fault("stopAdvertising denied, Bluetooth is powered off");
            return;
        }
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                    advertiseCallback = null;
                    if (bluetoothGattServer != null) {
                        bluetoothGattServer.clearServices();
                        bluetoothGattServer.close();
                        bluetoothGattServer = null;
                    }
                    logger.debug("stopAdvertising");
                    callback.accept(true);
                } catch (Throwable e) {
                    logger.fault("stopAdvertising failed", e);
                    callback.accept(false);
                }
            }
        });
    }

    private void startAdvertising() {
        logger.debug("startAdvertising");
        if (bluetoothLeAdvertiser == null) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported()) {
                bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            }
        }
        if (bluetoothLeAdvertiser == null) {
            logger.fault("Bluetooth LE advertiser unsupported");
            return;
        }
        if (bluetoothStateManager.state() != BluetoothState.poweredOn) {
            logger.fault("Bluetooth is not powered on");
            return;
        }
        if (advertiseCallback != null) {
            operationQueue.execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                    advertiseCallback = null;
                }
            });
        }
        if (bluetoothGattServer != null) {
            operationQueue.execute(new Runnable() {
                @Override
                public void run() {
                    bluetoothGattServer.clearServices();
                    bluetoothGattServer.close();
                    bluetoothGattServer = null;
                }
            });
        }
        if (!enabled) {
            return;
        }
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothGattServer = startGattServer(logger, context, payloadDataSupplier, database);
                setGattService(logger, context, bluetoothGattServer);
                advertiseCallback = startAdvertising(logger, bluetoothLeAdvertiser);
            }
        });
    }

    @Override
    public PayloadData payloadData() {
        return payloadDataSupplier.payload(new PayloadTimestamp(new Date()));
    }

    @Override
    public boolean isSupported() {
        return bluetoothLeAdvertiser != null;
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

    private static AdvertiseCallback startAdvertising(final SensorLogger logger, final BluetoothLeAdvertiser bluetoothLeAdvertiser) {
        final AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        final AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(BLESensorConfiguration.serviceUUID))
                .build();

        final AdvertiseCallback callback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                logger.debug("advertising (settingsInEffect={})", settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                logger.fault("advertising failed to start (error={})", onStartFailureErrorCodeToString(errorCode));
            }
        };
        bluetoothLeAdvertiser.startAdvertising(settings, data, callback);
        return callback;
    }

    private static BluetoothGattServer startGattServer(final SensorLogger logger, final Context context, final PayloadDataSupplier payloadDataSupplier, final BLEDatabase database) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            logger.fault("Bluetooth unsupported");
            return null;
        }
        // Data = rssi (4 bytes int) + payload (remaining bytes)
        final AtomicReference<BluetoothGattServer> server = new AtomicReference<>(null);
        final BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
            private final Map<String, PayloadData> onCharacteristicReadPayloadData = new ConcurrentHashMap<>();
            private final Map<String, byte[]> onCharacteristicWriteSignalData = new ConcurrentHashMap<>();

            private PayloadData onCharacteristicReadPayloadData(BluetoothDevice device) {
                final String key = device.getAddress();
                if (onCharacteristicReadPayloadData.containsKey(key)) {
                    return onCharacteristicReadPayloadData.get(key);
                }
                final PayloadData payloadData = payloadDataSupplier.payload(new PayloadTimestamp());
                onCharacteristicReadPayloadData.put(key, payloadData);
                return payloadData;
            }

            private byte[] onCharacteristicWriteSignalData(BluetoothDevice device, byte[] value) {
                final String key = device.getAddress();
                byte[] partialData = onCharacteristicWriteSignalData.get(key);
                if (partialData == null) {
                    partialData = new byte[0];
                }
                byte[] data = new byte[partialData.length + (value == null ? 0 : value.length)];
                System.arraycopy(partialData, 0, data, 0, partialData.length);
                if (value != null) {
                    System.arraycopy(value, 0, data, partialData.length, value.length);
                }
                onCharacteristicWriteSignalData.put(key, data);
                return data;
            }

            private void removeData(BluetoothDevice device) {
                final String deviceAddress = device.getAddress();
                for (String deviceRequestId : new ArrayList<>(onCharacteristicReadPayloadData.keySet())) {
                    if (deviceRequestId.startsWith(deviceAddress)) {
                        onCharacteristicReadPayloadData.remove(deviceRequestId);
                    }
                }
                for (String deviceRequestId : new ArrayList<>(onCharacteristicWriteSignalData.keySet())) {
                    if (deviceRequestId.startsWith(deviceAddress)) {
                        onCharacteristicWriteSignalData.remove(deviceRequestId);
                    }
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothDevice bluetoothDevice, int status, int newState) {
                final BLEDevice device = database.device(bluetoothDevice);
                logger.debug("onConnectionStateChange (device={},status={},newState={})",
                        device, status, onConnectionStateChangeStatusToString(newState));
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    device.state(BLEDeviceState.connected);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    device.state(BLEDeviceState.disconnected);
                    removeData(bluetoothDevice);
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                final BLEDevice targetDevice = database.device(device);
                final TargetIdentifier targetIdentifier = targetDevice.identifier;
                logger.debug("didReceiveWrite (central={},requestId={},offset={},characteristic={},value={})",
                        targetDevice, requestId, offset,
                        (characteristic.getUuid().equals(BLESensorConfiguration.androidSignalCharacteristicUUID) ? "signal" : "unknown"),
                        (value != null ? value.length : "null")
                );
                if (characteristic.getUuid() != BLESensorConfiguration.androidSignalCharacteristicUUID) {
                    if (responseNeeded) {
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, value);
                    }
                    return;
                }
                final Data data = new Data(onCharacteristicWriteSignalData(device, value));
                switch (SignalCharacteristicData.detect(data)) {
                    case rssi: {
                        final RSSI rssi = SignalCharacteristicData.decodeWriteRSSI(data);
                        if (rssi == null) {
                            logger.fault("didReceiveWrite, invalid request (central={},action=writeRSSI)", targetDevice);
                            break;
                        }
                        logger.debug("didReceiveWrite (dataType=rssi,central={},rssi={})", targetDevice, rssi);
                        // Only receive-only Android devices write RSSI
                        targetDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                        targetDevice.receiveOnly(true);
                        targetDevice.rssi(rssi);
                        break;
                    }
                    case payload: {
                        final PayloadData payloadData = SignalCharacteristicData.decodeWritePayload(data);
                        if (payloadData == null) {
                            // Fragmented payload data may be incomplete
                            break;
                        }
                        logger.debug("didReceiveWrite (dataType=payload,central={},payload={})", targetDevice, payloadData);
                        // Only Android devices write payload
                        targetDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                        targetDevice.payloadData(payloadData);
                        onCharacteristicWriteSignalData.remove(device.getAddress());
                        break;
                    }
                    case payloadSharing: {
                        final PayloadSharingData payloadSharingData = SignalCharacteristicData.decodeWritePayloadSharing(data);
                        if (payloadSharingData == null) {
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
                        logger.debug("didReceiveWrite (dataType=payloadSharing,central={},payloadSharingData={})", targetDevice, didSharePayloadData);
                        for (final PayloadData payloadData : didSharePayloadData) {
                            final BLEDevice sharedDevice = database.device(payloadData);
                            sharedDevice.operatingSystem(BLEDeviceOperatingSystem.shared);
                            sharedDevice.rssi(payloadSharingData.rssi);
                        }
                        break;
                    }
                }
                if (responseNeeded) {
                    server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                final BLEDevice targetDevice = database.device(device);
                if (characteristic.getUuid() == BLESensorConfiguration.payloadCharacteristicUUID) {
                    final PayloadData payloadData = onCharacteristicReadPayloadData(device);
                    if (offset > payloadData.value.length) {
                        logger.fault("didReceiveRead, invalid offset (central={},requestId={},offset={},characteristic=payload,dataLength={})", targetDevice, requestId, offset, payloadData.value.length);
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                    } else {
                        final byte[] value = Arrays.copyOfRange(payloadData.value, offset, payloadData.value.length);
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                        logger.debug("didReceiveRead (central={},requestId={},offset={},characteristic=payload)", targetDevice, requestId, offset);
                    }
                } else {
                    logger.fault("didReceiveRead (central={},characteristic=unknown)", targetDevice);
                    server.get().sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null);
                }
            }
        };
        server.set(bluetoothManager.openGattServer(context, callback));
        logger.debug("GATT server started");
        return server.get();
    }

    private static void setGattService(final SensorLogger logger, final Context context, final BluetoothGattServer bluetoothGattServer) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            logger.fault("Bluetooth unsupported");
            return;
        }
        if (bluetoothGattServer == null) {
            logger.fault("Bluetooth LE advertiser unsupported");
            return;
        }
        for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            bluetoothGattServer.cancelConnection(device);
        }
        for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
            bluetoothGattServer.cancelConnection(device);
        }
        bluetoothGattServer.clearServices();
        final BluetoothGattService service = new BluetoothGattService(BLESensorConfiguration.serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
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
        service.addCharacteristic(payloadCharacteristic);
        bluetoothGattServer.addService(service);
        logger.debug("setGattService (service={},signalCharacteristic={},payloadCharacteristic={})",
                service.getUuid(), signalCharacteristic.getUuid(), payloadCharacteristic.getUuid());
    }

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
