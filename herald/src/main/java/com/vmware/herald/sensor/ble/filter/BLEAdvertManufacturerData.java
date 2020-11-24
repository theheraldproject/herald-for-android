//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble.filter;

public class BLEAdvertManufacturerData {
    public int manufacturer;
    public byte[] data; // BIG ENDIAN (network order) AT THIS POINT

    public BLEAdvertManufacturerData(int manufacturer, byte[] dataBigEndian) {
        this.manufacturer = manufacturer;
        this.data = dataBigEndian;
    }
}
