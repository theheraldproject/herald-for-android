//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.BluetoothState;

/**
 * Monitors bluetooth state changes.
 */
public class ConcreteBluetoothStateManager implements BluetoothStateManager {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBluetoothStateManager");
    @NonNull
    private BluetoothState state;
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                try {
                    final int nativeState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    logger.debug("Bluetooth state changed (nativeState={})", nativeState);
                    switch (nativeState) {
                        case BluetoothAdapter.STATE_ON:
                            logger.debug("Power ON");
                            state = BluetoothState.poweredOn;
                            for (BluetoothStateManagerDelegate delegate : delegates) {
                                delegate.bluetoothStateManager(BluetoothState.poweredOn);
                            }
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            logger.debug("Power OFF");
                            state = BluetoothState.poweredOff;
                            for (BluetoothStateManagerDelegate delegate : delegates) {
                                delegate.bluetoothStateManager(BluetoothState.poweredOff);
                            }
                            break;
                    }
                } catch (Throwable e) {
                    logger.fault("Bluetooth state change exception", e);
                }
            }
        }
    };

    /**
     * Monitors bluetooth state changes.
     * 
     * @param context The Herald execution Context. Used to register the BLE Receiver against.
     */
    public ConcreteBluetoothStateManager(@NonNull final Context context) {
        state = state();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    @NonNull
    @Override
    public BluetoothState state() {
        //noinspection ConstantConditions
        if (null == state) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (null == bluetoothAdapter) {
                state = BluetoothState.unsupported;
                return state;
            }
            switch (BluetoothAdapter.getDefaultAdapter().getState()) {
                case BluetoothAdapter.STATE_ON:
                    state = BluetoothState.poweredOn;
                    break;
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_TURNING_ON:
                default:
                    state = BluetoothState.poweredOff;
                    break;
            }
        }
        return state;
    }
}
