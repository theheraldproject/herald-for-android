package org.c19x.sensor.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.BluetoothState;

/**
 * Monitors bluetooth state changes.
 */
public class ConcreteBluetoothStateManager implements BluetoothStateManager {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBluetoothStateManager");
    private BluetoothState state;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == BluetoothAdapter.ACTION_STATE_CHANGED) {
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
     */
    public ConcreteBluetoothStateManager(Context context) {
        state = state();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public BluetoothState state() {
        if (state == null) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                state = BluetoothState.unsupported;
                return state;
            }
            switch (BluetoothAdapter.getDefaultAdapter().getState()) {
                case BluetoothAdapter.STATE_ON:
                    state = BluetoothState.poweredOn;
                    break;
                case BluetoothAdapter.STATE_OFF:
                    state = BluetoothState.poweredOff;
                    break;
                default:
                    state = BluetoothState.resetting;
                    break;
            }
        }
        return state;
    }
}
