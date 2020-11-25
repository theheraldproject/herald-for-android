//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble.filter;

public class BLEAdvertAppleManufacturerSegment {
    public int type;
    public int reportedLength;
    public byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public byte[] raw;

    public BLEAdvertAppleManufacturerSegment(int type, int reportedLength, byte[] dataBigEndian, byte[] rawBigEndian) {
        this.type = type;
        this.reportedLength = reportedLength;
        this.data = dataBigEndian;
        this.raw = rawBigEndian;
    }
}
