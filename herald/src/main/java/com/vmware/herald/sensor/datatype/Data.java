//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/// Raw byte array data
public class Data {
    private final static char[] hexChars = "0123456789ABCDEF".toCharArray();
    public byte[] value = null;

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

    public Data(byte repeating, int count) {
        this.value = new byte[count];
        for (int i=count; i-->0;) {
            this.value[i] = repeating;
        }
    }

    public Data(String base64EncodedString) {
        this.value = Base64.decode(base64EncodedString);
    }

    public String base64EncodedString() {
        return Base64.encode(value);
    }

    public String hexEncodedString() {
        if (value == null) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder(value.length * 2);
        for (int i = 0; i < value.length; i++) {
            final int v = value[i] & 0xFF;
            stringBuilder.append(hexChars[v >>> 4]);
            stringBuilder.append(hexChars[v & 0x0F]);
        }
        return stringBuilder.toString();
    }

    public final static Data fromHexEncodedString(String hexEncodedString) {
        final int length = hexEncodedString.length();
        final byte[] value = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            value[i / 2] = (byte) ((Character.digit(hexEncodedString.charAt(i), 16) << 4) +
                    Character.digit(hexEncodedString.charAt(i+1), 16));
        }
        return new Data(value);
    }

    public String description() {
        return base64EncodedString();
    }

    /// Get subdata from offset to end
    public Data subdata(int offset) {
        if (offset >=0 && offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Get subdata from offset to offset + length
    public Data subdata(int offset, int length) {
        if (offset >= 0 && offset < value.length && offset + length <= value.length) {
            final byte[] offsetValue = new byte[length];
            System.arraycopy(value, offset, offsetValue, 0, length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Append data to end of this data.
    public void append(Data data) {
        append(data.value);
    }

    private void append(byte[] data) {
        final byte[] concatenated = new byte[value.length + data.length];
        System.arraycopy(value, 0, concatenated, 0, value.length);
        System.arraycopy(data, 0, concatenated, value.length, data.length);
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

    @Override
    public String toString() {
        return hexEncodedString();
    }

    // MARK:- Conversion from intrinsic types to Data

    public void append(final UInt8 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF)
        });
    }

    public UInt8 uint8(final int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return new UInt8(value[index] & 0xFF);
    }

    public void append(final Int8 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF)
        });
    }

    public Int8 int8(final int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return new Int8(value[index]);
    }

    public void append(final UInt16 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >>> 8) & 0xFF) // MSB
        });
    }

    public UInt16 uint16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        final int v =
                value[index] & 0xFF |
                ((value[index + 1] & 0xFF) << 8);
        return new UInt16(v);
    }


    public void append(final Int16 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF), // LSB
                (byte) (value.value >> 8)    // MSB
        });
    }

    public Int16 int16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        final int v =
                value[index] & 0xFF |
                ((value[index + 1]) << 8);
        return new Int16(v);
    }

    public void append(final UInt32 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >>> 8) & 0xFF),
                (byte) ((value.value >>> 16) & 0xFF),
                (byte) ((value.value >>> 24) & 0xFF) // MSB
        });
    }

    public UInt32 uint32(final int index) {
        if (index < 0 || index + 3 >= value.length) {
            return null;
        }
        final long v =
                (long) (value[index] & 0xFF) |
                ((long) (value[index + 1] & 0xFF) << 8) |
                ((long) (value[index + 2] & 0xFF) << 16) |
                ((long) (value[index + 3] & 0xFF) << 24);
        return new UInt32(v);
    }

    public void append(final Int32 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >> 8) & 0xFF),
                (byte) ((value.value >> 16) & 0xFF),
                (byte) (value.value >> 24)          // MSB
        });
    }

    public Int32 int32(final int index) {
        if (index < 0 || index + 3 >= value.length) {
            return null;
        }
        final int v =
                (value[index] & 0xFF) |
                ((value[index + 1] & 0xFF) << 8) |
                ((value[index + 2] & 0xFF) << 16) |
                ((value[index + 3]) << 24);
        return new Int32(v);
    }

    public void append(final UInt64 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >>> 8) & 0xFF),
                (byte) ((value.value >>> 16) & 0xFF),
                (byte) ((value.value >>> 24) & 0xFF),
                (byte) ((value.value >>> 32) & 0xFF),
                (byte) ((value.value >>> 40) & 0xFF),
                (byte) ((value.value >>> 48) & 0xFF),
                (byte) ((value.value >>> 56) & 0xFF) // MSB
        });
    }

    public UInt64 uint64(final int index) {
        if (index < 0 || index + 7 >= value.length) {
            return null;
        }
        final long v =
                (long) value[index] & 0xFF |
                ((long) (value[index + 1] & 0xFF) << 8) |
                ((long) (value[index + 2] & 0xFF) << 16) |
                ((long) (value[index + 3] & 0xFF) << 24) |
                ((long) (value[index + 4] & 0xFF) << 32) |
                ((long) (value[index + 5] & 0xFF) << 40) |
                ((long) (value[index + 6] & 0xFF) << 48) |
                ((long) (value[index + 7] & 0xFF) << 56);
        return new UInt64(v);
    }

    public void append(final Int64 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >> 8) & 0xFF),
                (byte) ((value.value >> 16) & 0xFF),
                (byte) ((value.value >> 24) & 0xFF),
                (byte) ((value.value >> 32) & 0xFF),
                (byte) ((value.value >> 40) & 0xFF),
                (byte) ((value.value >> 48) & 0xFF),
                (byte) ((value.value >> 56)) // MSB
        });
    }

    public Int64 int64(final int index) {
        if (index < 0 || index + 7 >= value.length) {
            return null;
        }
        final long v =
                (long) value[index] & 0xFF |
                ((long) (value[index + 1] & 0xFF) << 8) |
                ((long) (value[index + 2] & 0xFF) << 16) |
                ((long) (value[index + 3] & 0xFF) << 24) |
                ((long) (value[index + 4] & 0xFF) << 32) |
                ((long) (value[index + 5] & 0xFF) << 40) |
                ((long) (value[index + 6] & 0xFF) << 48) |
                ((long) (value[index + 7]) << 56);
        return new Int64(v);
    }
}
