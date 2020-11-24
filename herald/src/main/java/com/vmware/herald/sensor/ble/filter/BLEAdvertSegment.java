//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble.filter;

public class BLEAdvertSegment {
    public BLEAdvertSegmentType type;
    public int dataLength;
    public byte[] data; // BIG ENDIAN (network order) AT THIS POINT

    public BLEAdvertSegment(BLEAdvertSegmentType type, int dataLength, byte[] data) {
        this.type = type;
        this.dataLength = dataLength;
        this.data = data;
    }
}
