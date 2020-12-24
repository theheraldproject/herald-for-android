//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

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

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.BluetoothState;
import com.vmware.herald.sensor.datatype.Callback;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.ImmediateSendData;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadSharingData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.PseudoDeviceAddress;
import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.SignalCharacteristicData;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;
import com.vmware.herald.sensor.datatype.Triple;
import com.vmware.herald.sensor.PayloadDataSupplier;
import com.vmware.herald.sensor.SensorDelegate;

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
    private final static long advertOffDurationMillis = TimeInterval.seconds(4).millis();
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final PayloadDataSupplier payloadDataSupplier;
    private final BLEDatabase database;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();

    // Referenced by startAdvert and stopExistingGattServer ONLY
    private BluetoothGattServer bluetoothGattServer = null;

    /**
     * Transmitter starts automatically when Bluetooth is enabled.
     */
    public ConcreteBLETransmitter(Context context, BluetoothStateManager bluetoothStateManager, BLETimer timer, PayloadDataSupplier payloadDataSupplier, BLEDatabase database) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.payloadDataSupplier = payloadDataSupplier;
        this.database = database;
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
        timer.add(new AdvertLoopTask());
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        logger.debug("start (supported={})", isSupported());
        // advertLoop is started by Bluetooth state
    }

    @Override
    public void stop() {
        logger.debug("stop");
        // advertLoop is stopped by Bluetooth state
    }

    // MARK:- Advert loop

    private enum AdvertLoopState {
        starting, started, stopping, stopped
    }

    /// Get Bluetooth LE advertiser
    private BluetoothLeAdvertiser bluetoothLeAdvertiser() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            logger.debug("bluetoothLeAdvertiser, no Bluetooth Adapter available");
            return null;
        }
        boolean supported = bluetoothAdapter.isMultipleAdvertisementSupported();
        try {
            final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (bluetoothLeAdvertiser == null) {
                logger.debug("bluetoothLeAdvertiser, no LE advertiser present (multiSupported={}, exception=no)", supported);
                return null;
            }
            // log this, as this will allow us to identify handsets with a different API implementation
            logger.debug("bluetoothLeAdvertiser, LE advertiser present (multiSupported={})", supported);
            return bluetoothLeAdvertiser;
        } catch (Exception e) {
            // log it, as this will allow us to identify handsets with the expected API implementation (from Android API source code)
            logger.debug("bluetoothLeAdvertiser, no LE advertiser present (multiSupported={}, exception={})", supported, e.getMessage());
            return null;
        }
    }

    private class AdvertLoopTask implements BLETimerDelegate {
        private AdvertLoopState advertLoopState = AdvertLoopState.stopped;
        private long lastStateChangeAt = System.currentTimeMillis();
        private BluetoothGattServer bluetoothGattServer;
        private AdvertiseCallback advertiseCallback;

        private void state(final long now, AdvertLoopState state) {
            final long elapsed = now - lastStateChangeAt;
            logger.debug("advertLoopTask, state change (from={},to={},elapsed={}ms)", advertLoopState, state, elapsed);
            this.advertLoopState = state;
            lastStateChangeAt = now;
        }

        private long timeSincelastStateChange(final long now) {
            return now - lastStateChangeAt;
        }

        @Override
        public void bleTimer(final long now) {
            if (!isSupported() || bluetoothStateManager.state() == BluetoothState.poweredOff) {
                if (advertLoopState != AdvertLoopState.stopped) {
                    logger.debug("advertLoopTask, stopping advert following bluetooth state change (isSupported={},bluetoothPowerOff={})", isSupported(), bluetoothStateManager.state() == BluetoothState.poweredOff);
                    stopAdvert(bluetoothLeAdvertiser(), advertiseCallback, bluetoothGattServer, new Callback<Boolean>() {
                        @Override
                        public void accept(Boolean value) {
                            advertiseCallback = null;
                            bluetoothGattServer = null;
                            state(now, AdvertLoopState.stopped);
                            logger.debug("advertLoopTask, stop advert (advert={}ms)", timeSincelastStateChange(now));
                        }
                    });
                }
                return;
            }
            switch (advertLoopState) {
                case stopped: {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= advertOffDurationMillis) {
                            logger.debug("advertLoopTask, start advert (stop={}ms)", period);
                            final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothLeAdvertiser();
                            if (bluetoothLeAdvertiser == null) {
                                logger.fault("advertLoopTask, start advert denied, Bluetooth LE advertiser unavailable");
                                return;
                            }
                            state(now, AdvertLoopState.starting);
                            startAdvert(bluetoothLeAdvertiser, new Callback<Triple<Boolean, AdvertiseCallback, BluetoothGattServer>>() {
                                @Override
                                public void accept(Triple<Boolean, AdvertiseCallback, BluetoothGattServer> value) {
                                    advertiseCallback = value.b;
                                    bluetoothGattServer = value.c;
                                    state(now, value.a ? AdvertLoopState.started : AdvertLoopState.stopped);
                                }
                            });
                        }
                    }
                    break;
                }
                case started: {
                    final long period = timeSincelastStateChange(now);
                    if (period >= BLESensorConfiguration.advertRefreshTimeInterval.millis()) {
                        logger.debug("advertLoopTask, stop advert (advert={}ms)", period);
                        final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothLeAdvertiser();
                        if (bluetoothLeAdvertiser == null) {
                            logger.fault("advertLoopTask, stop advert denied, Bluetooth LE advertiser unavailable");
                            return;
                        }
                        state(now, AdvertLoopState.stopping);
                        stopAdvert(bluetoothLeAdvertiser, advertiseCallback, bluetoothGattServer, new Callback<Boolean>() {
                            @Override
                            public void accept(Boolean value) {
                                advertiseCallback = null;
                                bluetoothGattServer = null;
                                state(now, AdvertLoopState.stopped);
                            }
                        });
                    }
                    break;
                }
            }
        }
    }

    // MARK:- Start and stop advert

    private void startAdvert(final BluetoothLeAdvertiser bluetoothLeAdvertiser, final Callback<Triple<Boolean, AdvertiseCallback, BluetoothGattServer>> callback) {
        logger.debug("startAdvert");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                // Stop existing advert if there is already a proxy reference.
                // This should never happen because only the AdvertLoopTask calls
                // startAdvert and it should only call startAdvert after stopAdvert
                // has been called previously. Logging this condition to verify if
                // this condition can ever occur to support investigation.
                if (bluetoothGattServer != null) {
                    logger.fault("startAdvert found existing GATT server");
                    try {
                        bluetoothGattServer.clearServices();
                        bluetoothGattServer.close();
                    } catch (Throwable e) {
                        logger.fault("startAdvert found existing GATT server but failed to stop the server", e);
                    }
                    bluetoothGattServer = null;
                }
                // Start new GATT server
                try {
                    bluetoothGattServer = startGattServer(logger, context, payloadDataSupplier, database);
                } catch (Throwable e) {
                    logger.fault("startAdvert failed to start GATT server", e);
                    result = false;
                }
                if (bluetoothGattServer == null) {
                    result = false;
                } else {
                    try {
                        setGattService(logger, context, bluetoothGattServer);
                    } catch (Throwable e) {
                        logger.fault("startAdvert failed to set GATT service", e);
                        try {
                            bluetoothGattServer.clearServices();
                            bluetoothGattServer.close();
                            bluetoothGattServer = null;
                        } catch (Throwable e2) {
                            logger.fault("startAdvert failed to stop GATT server", e2);
                            bluetoothGattServer = null;
                        }
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
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                            logger.debug("startAdvert successful");
                            callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(true, this, bluetoothGattServerConfirmed));
                        }

                        @Override
                        public void onStartFailure(int errorCode) {
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

    private void stopAdvert(final BluetoothLeAdvertiser bluetoothLeAdvertiser, final AdvertiseCallback advertiseCallback, final BluetoothGattServer bluetoothGattServer, final Callback<Boolean> callback) {
        logger.debug("stopAdvert");
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                try {
                    if (bluetoothLeAdvertiser != null && advertiseCallback != null) {
                        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                    }
                } catch (Throwable e) {
                    logger.fault("stopAdvert failed to stop advertising", e);
                    result = false;
                }
                try {
                    if (bluetoothGattServer != null) {
                        bluetoothGattServer.clearServices();
                        bluetoothGattServer.close();
                    }
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


    @Override
    public PayloadData payloadData() {
        return payloadDataSupplier.payload(new PayloadTimestamp(new Date()));
    }

    @Override
    public boolean isSupported() {
        return bluetoothLeAdvertiser() != null;
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

    private void startAdvertising(final BluetoothLeAdvertiser bluetoothLeAdvertiser, final AdvertiseCallback advertiseCallback) {
        logger.debug("startAdvertising");
        final AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();

        final PseudoDeviceAddress pseudoDeviceAddress = new PseudoDeviceAddress();
        final AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(BLESensorConfiguration.serviceUUID))
                .addManufacturerData(BLESensorConfiguration.manufacturerIdForSensor, pseudoDeviceAddress.data)
                .build();
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
        logger.debug("startAdvertising successful (pseudoDeviceAddress={},settings={})", pseudoDeviceAddress, settings);
    }

    private static BluetoothGattServer startGattServer(final SensorLogger logger, final Context context, final PayloadDataSupplier payloadDataSupplier, final BLEDatabase database) {
        logger.debug("startGattServer");
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
				if (characteristic.getUuid().equals(BLESensorConfiguration.legacyPayloadCharacteristicUUID)) {
				    if (null == data.value) {
				        return;
                    }
                    final PayloadData payloadData = new PayloadData(data.value);
                    logger.debug("didReceiveWrite (dataType=payload,central={},payload={})", targetDevice, payloadData);
                    targetDevice.payloadData(payloadData);
                    onCharacteristicWriteSignalData.remove(device.getAddress());
                    if (responseNeeded) {
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    }
                    return;
                }
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
                        // Only receive-only Android devices write payload
                        targetDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                        targetDevice.receiveOnly(true);
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
                    case immediateSend: {
                        final ImmediateSendData immediateSendData = SignalCharacteristicData.decodeImmediateSend(data);
                        if (immediateSendData == null) {
                            // Fragmented immediate send data may be incomplete
                            break;
                        }
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, immediateSendData, targetIdentifier);
                        }
                        logger.debug("didReceiveWrite (dataType=immediateSend,central={},immediateSendData={})", targetDevice, immediateSendData.data);
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
                if (characteristic.getUuid() == BLESensorConfiguration.payloadCharacteristicUUID || characteristic.getUuid().equals(BLESensorConfiguration.legacyPayloadCharacteristicUUID)) {
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
        logger.debug("startGattServer successful");
        return server.get();
    }

    private static void setGattService(final SensorLogger logger, final Context context, final BluetoothGattServer bluetoothGattServer) {
        logger.debug("setGattService");
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

        // Logic check - ensure there are now no Gatt Services
        List<BluetoothGattService> services = bluetoothGattServer.getServices();
        for (BluetoothGattService svc : services) {
            logger.fault("setGattService device clearServices() call did not correctly clear service (service={})",svc.getUuid());
        }

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
		if (null != BLESensorConfiguration.legacyPayloadCharacteristic) {
			final BluetoothGattCharacteristic legacyPayloadCharacteristic = 
			BLESensorConfiguration.legacyPayloadCharacteristic;
        	service.addCharacteristic(legacyPayloadCharacteristic);
		}
        service.addCharacteristic(payloadCharacteristic);
        bluetoothGattServer.addService(service);

        // Logic check - ensure there can be only one Herald service
        services = bluetoothGattServer.getServices();
        int count = 0;
        for (BluetoothGattService svc : services) {
            if (svc.getUuid().equals(BLESensorConfiguration.serviceUUID)) {
                count++;
            }
        }
        if (count > 1) {
            logger.fault("setGattService device incorrectly sharing multiple Herald services (count={})", count);
        }

        logger.debug("setGattService successful (service={},signalCharacteristic={},payloadCharacteristic={})",
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
