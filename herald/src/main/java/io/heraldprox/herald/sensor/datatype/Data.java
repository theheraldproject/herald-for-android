//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.Arrays;

/// Raw byte array data
public class Data {
    private final static char[] hexChars = "0123456789ABCDEF".toCharArray();
    @Nullable
    public byte[] value = null;

    public Data() {
        this(new byte[0]);
    }

    public Data(@Nullable byte[] value) {
        if (null == value) {
            this.value = new byte[0];
            return;
        }
        this.value = value;
    }

    public Data(@Nullable final Data data) {
        if (null == data) {
            this.value = new byte[0];
            return;
        }
        final byte[] value = new byte[data.value.length];
        System.arraycopy(data.value, 0, value, 0, data.value.length);
        this.value = value;
    }

    public Data(byte repeating, int count) {
        if (count < 0) {
            this.value = new byte[0];
            return;
        }
        this.value = new byte[count];
        for (int i=count; i-->0;) {
            this.value[i] = repeating;
        }
    }

    public Data(@Nullable String base64EncodedString) {
        if (null == base64EncodedString) {
            this.value = new byte[0];
            return;
        }
        this.value = Base64.decode(base64EncodedString);
    }

    @NonNull
    public String base64EncodedString() {
        return Base64.encode(value);
    }

    @NonNull
    public String hexEncodedString() {
        if (null == value || 0 == value.length) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder(value.length * 2);
        int v;
        for (int i = 0; i < value.length; i++) {
            v = value[i] & 0xFF;
            stringBuilder.append(hexChars[v >>> 4]);
            stringBuilder.append(hexChars[v & 0x0F]);
        }
        return stringBuilder.toString();
    }

    @NonNull
    public final static Data fromHexEncodedString(@Nullable String hexEncodedString) {
        if (null == hexEncodedString) {
            return new Data();
        }
        final int length = hexEncodedString.length();
        final byte[] value = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            value[i / 2] = (byte) ((Character.digit(hexEncodedString.charAt(i), 16) << 4) +
                    Character.digit(hexEncodedString.charAt(i+1), 16));
        }
        return new Data(value);
    }

    @NonNull
    public String description() {
        return base64EncodedString();
    }

    /// Get subdata from offset to end
    @Nullable
    public Data subdata(int offset) {
        if (offset >= 0 && offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// Get subdata from offset to offset + length
    @Nullable
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
    public void append(@Nullable Data data) {
        if (null == data) {
            return;
        }
        append(data.value);
    }

    private void append(@Nullable byte[] data) {
        if (null == data) {
            return;
        }
        final byte[] concatenated = new byte[value.length + data.length];
        System.arraycopy(value, 0, concatenated, 0, value.length);
        System.arraycopy(data, 0, concatenated, value.length, data.length);
        value = concatenated;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @NonNull
    @Override
    public String toString() {
        return hexEncodedString();
    }

    // MARK:- Conversion from intrinsic types to Data

    public void append(@Nullable final UInt8 value) {
        if (null == value) {
            return;
        }
        append(new byte[]{
                (byte) (value.value & 0xFF)
        });
    }

    @Nullable
    public UInt8 uint8(final int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return new UInt8(value[index] & 0xFF);
    }

    public void append(@Nullable final Int8 value) {
        if (null == value) {
            return;
        }
        append(new byte[]{
                (byte) (value.value & 0xFF)
        });
    }

    @Nullable
    public Int8 int8(final int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return new Int8(value[index]);
    }

    public void append(@Nullable final UInt16 value) {
        if (null == value) {
            return;
        }
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >>> 8) & 0xFF) // MSB
        });
    }

    @Nullable
    public UInt16 uint16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        final int v =
                value[index] & 0xFF |
                ((value[index + 1] & 0xFF) << 8);
        return new UInt16(v);
    }


    public void append(@Nullable final Int16 value) {
        if (null == value) {
            return;
        }
        append(new byte[]{
                (byte) (value.value & 0xFF), // LSB
                (byte) (value.value >> 8)    // MSB
        });
    }

    @Nullable
    public Int16 int16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        final int v =
                value[index] & 0xFF |
                ((value[index + 1]) << 8);
        return new Int16(v);
    }

    public void append(@Nullable final UInt32 value) {
        if (null == value) {
            return;
        }
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >>> 8) & 0xFF),
                (byte) ((value.value >>> 16) & 0xFF),
                (byte) ((value.value >>> 24) & 0xFF) // MSB
        });
    }

    @Nullable
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

    public void append(@Nullable final Int32 value) {
        if (null == value) {
            return;
        }
        append(new byte[]{
                (byte) (value.value & 0xFF),        // LSB
                (byte) ((value.value >> 8) & 0xFF),
                (byte) ((value.value >> 16) & 0xFF),
                (byte) (value.value >> 24)          // MSB
        });
    }

    @Nullable
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

    public void append(@Nullable final UInt64 value) {
        if (null == value) {
            return;
        }
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

    @Nullable
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

    public void append(@Nullable final Int64 value) {
        if (null == value) {
            return;
        }
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

    @Nullable
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

    public void append(@Nullable Float16 value) {
        if (null == value) {
            return;
        }
        append(value.bigEndian);
    }

    @Nullable
    public Float16 float16(int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        return new Float16(new Data(new byte[] {
                value[index], value[index + 1]
        }));
    }

    // MARK:- String to/from Data functions

    /// Encoding option for string length data as prefix
    public enum StringLengthEncodingOption {
        UINT8, UINT16, UINT32, UINT64
    }

    /// Encode string as data, inserting length as prefix using UInt8,...,64. Returns true if successful, false otherwise.
    public boolean append(@Nullable final String value) {
        if (null == value) {
            return false;
        }
        return append(value, StringLengthEncodingOption.UINT8);
    }

    public boolean append(@Nullable final String value, @Nullable final StringLengthEncodingOption encoding) {
        if (null == value || null == encoding) {
            return false;
        }
        byte[] data = null;
        try {
            data = value.getBytes("UTF-8");
        } catch (Throwable e) {
            return false;
        }
        if (null == data) {
            return false;
        }
        switch (encoding) {
            case UINT8:
            if (!(data.length <= UInt8.max.value)) {
                return false;
            }
            append(new UInt8(data.length));
            break;
        case UINT16:
            if (!(data.length <= UInt16.max.value))  {
                return false;
            }
            append(new UInt16(data.length));
            break;
        case UINT32:
            if (!(data.length <= UInt32.max.value)) {
                return false;
            }
            append(new UInt32(data.length));
            break;
        case UINT64:
            if (!(data.length <= UInt64.max.value)) {
                return false;
            }
            append(new UInt64(data.length));
            break;
        }
        append(data);
        return true;
    }

    /// Decoded string and start/end indices in data byte array
    public final static class DecodedString {
        @Nullable
        public final String value;
        public final int start;
        public final int end;

        public DecodedString(@Nullable final String value, final int start, final int end) {
            if (null == value) {
                this.value = "";
                this.start = 0;
                this.end = 0;
            } else {
                this.value = value;
                this.start = start;
                this.end = end;
            }
        }
    }

    @Nullable
    public DecodedString string(final int index) {
        return string(index, StringLengthEncodingOption.UINT8);
    }

    @Nullable
    public DecodedString string(final int index, @NonNull final StringLengthEncodingOption encoding) {
        // TODO Refactor Data for max size. Max Java byte length is MAX_INT. It cannot be LONG. Either change to int or refactor data class for array of array of bytes.
        long start = index;
        long end = index;
        switch (encoding) {
            case UINT8: {
                final UInt8 count = uint8(index);
                if (null == count) {
                    return null;
                }
                start = index + 1;
                end = start + count.value;
                break;
            }
            case UINT16: {
                final UInt16 count = uint16(index);
                if (null == count) {
                    return null;
                }
                start = index + 2;
                end = start + count.value;
                break;
            }
            case UINT32: {
                final UInt32 count = uint32(index);
                if (null == count) {
                    return null;
                }
                start = index + 4;
                end = start + count.value;
                break;
            }
            case UINT64: {
                final UInt64 count = uint64(index);
                if (null == count) {
                    return null;
                }
                start = index + 8;
                end = start + count.value;
                break;
            }
        }
        if (start > Integer.MAX_VALUE || end > Integer.MAX_VALUE) {
            return null;
        }
        if (start == index || start > value.length || end > value.length) {
            return null;
        }
        if (start > end) {
            return null;
        }
        try {
            final String string = new String(value, (int) start, (int) (end - start), Charset.forName("UTF-8"));
            return new DecodedString(string, (int) start, (int) end);
        } catch (Throwable e) {
            return null;
        }
    }

}
