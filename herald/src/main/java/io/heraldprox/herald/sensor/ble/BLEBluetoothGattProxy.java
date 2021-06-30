//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.bluetooth.BluetoothGatt;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;

/**
 * Proxy of BluetoothGatt for fixing CVE-2020-12856.
 */
public class BLEBluetoothGattProxy {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEBluetoothGattProxy");
    private Boolean reflectionSuccessful = null;
    private Field mService;
    private Class<?> iBluetoothGatt;

    /**
     * Invocation handler for intercepting calls to BluetoothGatt.readCharacteristic and
     * BluetoothGatt.writeCharacteristic, and replacing authReq parameter with 0 (NONE)
     * to fix CVE-2020-12856 vulnerability.
     */
    private final class ProxyInvocationHandler implements InvocationHandler {
        private final Object mServiceInstance;

        public ProxyInvocationHandler(final Object mServiceInstance) {
            this.mServiceInstance = mServiceInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                switch (method.getName()) {
                    case "readCharacteristic": {
                        if (args.length >= 4) {
                            logger.debug("invoke, patched authReq (method=readCharacteristic,fromArg={})", args[3]);
                            args[3] = 0;
                        }
                        break;
                    }
                    case "writeCharacteristic": {
                        if (args.length >= 5) {
                            logger.debug("invoke, patched authReq (method=writeCharacteristic,fromArg={})", args[4]);
                            args[4] = 0;
                        }
                        break;
                    }
                }
                logger.debug("invoke (method={})", method.getName());
            } catch (Throwable e) {
                logger.fault("invoke, bypass (reason=exception)", e);
            }
            return method.invoke(mServiceInstance, args);
        }
    }

    /**
     * Using Java reflection to obtain references to mService and IBluetoothGatt for proxy.
     * @return True on success, false otherwise.
     */
    @SuppressWarnings("ConstantConditions")
    private synchronized boolean reflection() {
        if (null != reflectionSuccessful) {
            return reflectionSuccessful;
        }
        try {
            // Get reference to BluetoothGatt.mService
            //noinspection JavaReflectionMemberAccess
            final Field mService = BluetoothGatt.class.getDeclaredField("mService");
            if (null == mService) {
                logger.fault("reflection failed (field=mService)");
                reflectionSuccessful = false;
                return false;
            }
            // Get reference to IBluetoothGatt
            final Class<?> iBluetoothGatt = mService.getType();
            if (null == iBluetoothGatt) {
                logger.fault("reflection failed (class=iBluetoothGatt)");
                reflectionSuccessful = false;
                return false;
            }
            // Set references
            this.mService = mService;
            this.iBluetoothGatt = iBluetoothGatt;
            this.reflectionSuccessful = true;
            logger.debug("reflection, successfully referenced mService and IBluetoothGatt");
            return true;
        } catch (Throwable e) {
            logger.fault("Reflection failed, exception", e);
            this.reflectionSuccessful = false;
            return false;
        }
    }

    /**
     * Replace mService field value in BluetoothGatt instance with proxy to enable interception
     * of calls to BluetoothGatt.readCharacteristic and BluetoothGatt.writeCharacteristic for
     * fixing CVE-2020-12856 vulnerability.
     * @param bluetoothGatt Bluetooth GATT for wrapping as proxy.
     */
    public synchronized void proxy(final BluetoothGatt bluetoothGatt) {
        if (null == bluetoothGatt) {
            logger.debug("proxy, bypassing (reason=bluetoothGattIsNull)");
            return;
        }
        if (!reflection()) {
            logger.debug("proxy, bypassing (reason=reflectionFailed)");
            return;
        }
        // Enable access to mService temporarily to facilitate proxy
        Boolean mServiceIsAccessible = null;
        try {
            mServiceIsAccessible = mService.isAccessible();
            logger.debug("proxy, mService accessibility at start (isAccessible={})", mService.isAccessible());
            if (!mServiceIsAccessible) {
                mService.setAccessible(true);
                logger.debug("proxy, mService accessibility enabled (isAccessible={})", mService.isAccessible());
            }
            // Get IBluetoothGatt instance
            final Object mServiceInstance = mService.get(bluetoothGatt);
            if (null == mServiceInstance) {
                logger.debug("proxy, bypassing (reason=mServiceInstanceIsNull)");
            } else {
                // Proxy IBluetoothGatt instance
                if (!Proxy.isProxyClass(mServiceInstance.getClass())) {
                    final Object mServiceInstanceProxy = Proxy.newProxyInstance(
                            bluetoothGatt.getClass().getClassLoader(),
                            new Class<?>[]{iBluetoothGatt},
                            new ProxyInvocationHandler(mServiceInstance));
                    mService.set(bluetoothGatt, mServiceInstanceProxy);
                    logger.debug("proxy, successfully wrapped mServiceInstance");
                } else {
                    logger.debug("proxy, bypassing (reason=mServiceInstanceIsProxy)");
                }
            }
        } catch (Throwable e) {
            logger.fault("proxy, bypassing (reason=exception)", e);
        }
        // Set access to mService back to original state
        if (null == mServiceIsAccessible) {
            return;
        }
        try {
            mService.setAccessible(mServiceIsAccessible);
            logger.debug("proxy, mService accessibility on completion (isAccessible={})", mService.isAccessible());
        } catch (Throwable e) {
            logger.fault("proxy, failed to reset mService accessibility", e);
        }
    }
}
