//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.datatype;

import android.util.Base64;

/// Encrypted payload data received from target. This is likely to be an encrypted datagram of the target's actual permanent identifier.
public class PayloadData extends Data {

    public PayloadData(byte[] value) {
        super(value);
    }

    public PayloadData(String base64EncodedString) {
        super(base64EncodedString);
    }

    public PayloadData(byte repeating, int count) {
        super(repeating, count);
    }

    public PayloadData() {
        this(new byte[0]);
    }

    public String shortName() {
        try {
            return Base64.encodeToString(value, 3, value.length - 3, Base64.DEFAULT | Base64.NO_WRAP).substring(0, 6);
        } catch (Throwable e) {
            return Base64.encodeToString(value, 0, value.length, Base64.DEFAULT | Base64.NO_WRAP);
        }
    }

    public String toString() {
        return shortName();
    }
}
