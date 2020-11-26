//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.simple;

import com.vmware.herald.sensor.datatype.Data;

/// Matching key
public class MatchingKey extends Data {

    public MatchingKey(Data value) {
        super(value);
    }

    public MatchingKey(byte repeating, int count) {
        super(repeating, count);
    }
}
