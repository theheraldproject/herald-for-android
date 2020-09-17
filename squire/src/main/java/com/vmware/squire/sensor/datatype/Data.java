//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor.datatype;

import java.util.Arrays;

/// Raw byte array data
public class Data {
    public final byte[] value;

    public Data(byte[] value) {
        this.value = value;
    }

//    public String base64EncodedString() {
//        return Base64.encodeToString(value, Base64.DEFAULT | Base64.NO_WRAP);
//    }
//
//    public String description() {
//        return base64EncodedString();
//    }

    public Data subdata(int offset) {
        if (offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
