//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import java.util.List;

public class BLEScanResponseData {
    public int dataLength;
    public List<BLEAdvertSegment> segments;

    public BLEScanResponseData(int dataLength, List<BLEAdvertSegment> segments) {
        this.dataLength = dataLength;
        this.segments = segments;
    }
}
