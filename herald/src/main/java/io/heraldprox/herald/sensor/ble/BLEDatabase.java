//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadSharingData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.List;

/**
 * Registry for collating fragments of information from asynchronous BLE operations.
 */
public interface BLEDatabase {
    /**
     * Add delegate for handling database events
     * @param delegate
     */
    void add(@NonNull final BLEDatabaseDelegate delegate);

    /**
     * Get or create device for collating information from asynchronous BLE operations.
     * @param scanResult
     * @return
     */
    @NonNull
    BLEDevice device(@NonNull final ScanResult scanResult);

    /**
     * Get or create device for collating information from asynchronous BLE operations.
     * @param bluetoothDevice
     * @return
     */
    @NonNull
    BLEDevice device(@NonNull final BluetoothDevice bluetoothDevice);

    /**
     * Get or create device for collating information from asynchronous BLE operations.
     * @param payloadData
     * @return
     */
    @NonNull
    BLEDevice device(@NonNull final PayloadData payloadData);

    /**
     * Get a device from a TargetIdentifier.
     * @param targetIdentifier
     * @return
     */
    @Nullable
    BLEDevice device(@NonNull final TargetIdentifier targetIdentifier);

    /**
     * Get all devices.
     * @return
     */
    @NonNull
    List<BLEDevice> devices();

    /**
     * Delete device from database.
     * @param device
     */
    void delete(@NonNull final BLEDevice device);

    /**
     * Get payload sharing data for a peer.
     * @param peer
     * @return
     */
    @NonNull
    PayloadSharingData payloadSharingData(@NonNull final BLEDevice peer);
}
