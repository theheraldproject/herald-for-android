//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.datatype;

import java.util.Arrays;

/// Raw byte array data
public class Data {
    public byte[] value;

    public Data() {
        this(new byte[0]);
    }

    public Data(byte[] value) {
        this.value = value;
    }

    public Data(final Data data) {
        final byte[] value = new byte[data.value.length];
        System.arraycopy(data.value, 0, value, 0, data.value.length);
        this.value = value;
    }

    public String base64EncodedString() {
        return Base64.encode(value);
    }

    public String description() {
        return base64EncodedString();
    }

    /// Get subdata from offset to end
    public Data subdata(int offset) {
        if (offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Get subdata from offset to offset + length
    public Data subdata(int offset, int length) {
        if (offset + length < value.length) {
            final byte[] offsetValue = new byte[length];
            System.arraycopy(value, offset, offsetValue, 0, length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Append data to end of this data.
    public void append(Data data) {
        final byte[] concatenated = new byte[value.length + data.value.length];
        System.arraycopy(value, 0, concatenated, 0, value.length);
        System.arraycopy(data.value, 0, concatenated, value.length, data.value.length);
        value = concatenated;
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
