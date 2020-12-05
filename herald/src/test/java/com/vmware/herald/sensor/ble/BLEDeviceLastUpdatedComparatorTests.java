//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.TargetIdentifier;

import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class BLEDeviceLastUpdatedComparatorTests {

    @Test
    public void testCorrectOrder() throws Exception {
        BLEDeviceDelegate dummyDelegate = new BLEDeviceDelegate() {
            @Override
            public void device(BLEDevice device, BLEDeviceAttribute didUpdate) {
                // do nothing
            }
        };

        BLEDevice earlierDevice = new BLEDevice(new TargetIdentifier(),dummyDelegate);
        earlierDevice.rssi(new RSSI(12));

        Thread.sleep(20); // a bit naughty, but only a short delay

        BLEDevice laterDevice = new BLEDevice(new TargetIdentifier(),dummyDelegate);
        laterDevice.rssi(new RSSI(12));

        // Assumption check
        assertNotEquals(earlierDevice.lastUpdatedAt.getTime(),laterDevice.lastUpdatedAt.getTime());

        SortedSet<BLEDevice> devices1 = new TreeSet<>(new BLEDeviceLastUpdatedComparator());
        devices1.add(earlierDevice);
        devices1.add(laterDevice);

        assertEquals(laterDevice,devices1.first());

        // Ensure this order of add works too on the same data
        SortedSet<BLEDevice> devices2 = new TreeSet<>(new BLEDeviceLastUpdatedComparator());
        devices2.add(laterDevice);
        devices2.add(earlierDevice);

        assertEquals(laterDevice,devices2.first());
    }
}
