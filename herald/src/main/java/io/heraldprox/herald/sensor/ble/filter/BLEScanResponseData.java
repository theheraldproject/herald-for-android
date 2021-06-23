//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import androidx.annotation.NonNull;

import java.util.List;

public class BLEScanResponseData {
    public final int dataLength;
    @NonNull
    public final List<BLEAdvertSegment> segments;

    public BLEScanResponseData(final int dataLength, @NonNull final List<BLEAdvertSegment> segments) {
        this.dataLength = dataLength;
        this.segments = segments;
    }

    @NonNull
    @Override
    public String toString() {
        return segments.toString();
    }
}
