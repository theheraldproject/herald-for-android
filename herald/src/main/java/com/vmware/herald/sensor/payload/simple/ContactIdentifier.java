//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.simple;

import com.vmware.herald.sensor.datatype.Data;

/// Contact identifier
public class ContactIdentifier extends Data {

    public ContactIdentifier(Data value) {
        super(value);
    }

    public ContactIdentifier(byte repeating, int count) {
        super(repeating, count);
    }
}
