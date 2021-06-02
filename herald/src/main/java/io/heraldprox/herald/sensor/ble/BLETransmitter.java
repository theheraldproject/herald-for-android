//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.Sensor;
import io.heraldprox.herald.sensor.SensorDelegate;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Beacon transmitter broadcasts a fixed service UUID to enable background scan by iOS. When iOS
 * enters background mode, the UUID will disappear from the broadcast, so Android devices need to
 * search for Apple devices and then connect and discover services to read the UUID.
 */
public interface BLETransmitter extends Sensor {
    /**
     * Delegates for receiving beacon detection events. This is necessary because some Android devices (Samsung J6)
     * does not support BLE transmit, thus making the beacon characteristic writable offers a mechanism for such devices
     * to detect a beacon transmitter and make their own presence known by sending its own beacon code and RSSI as
     * data to the transmitter.
     */
    @NonNull
    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();

    /**
     * Get current payload.
     * 
     * @return The current PayloadData for this device.
     */
    @NonNull
    PayloadData payloadData();

    /**
     * Is transmitter supported.
     *
     * @return True if BLE advertising is supported.
     */
    boolean isSupported();
}
