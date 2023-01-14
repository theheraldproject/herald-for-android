//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Raw byte array data
 */
public class Data {
    private final static char[] hexChars = "0123456789ABCDEF".toCharArray();
    @NonNull
    public byte[] value;

    public Data() {
        this(new byte[0]);
    }

    public Data(@NonNull final byte[] value) {
        //noinspection ConstantConditions
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

    public Data(final byte repeating, final int count) {
        if (count < 0) {
            this.value = new byte[0];
            return;
        }
        this.value = new byte[count];
        for (int i=count; i-->0;) {
            this.value[i] = repeating;
        }
    }

    public Data(@Nullable final String base64EncodedString) {
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
        //noinspection ConstantConditions
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
    public static Data fromHexEncodedString(@Nullable final String hexEncodedString) {
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

    /**
     * Get subdata from offset to end
     * @param offset Offset
     * @return Suffix of data
     */
    @Nullable
    public Data subdata(final int offset) {
        if (offset >= 0 && offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /**
     * Get subdata from offset to offset + length
     * @param offset Offset
     * @param length Length from offset
     * @return Data fragment
     */
    @Nullable
    public Data subdata(final int offset, final int length) {
        if (offset >= 0 && offset < value.length && offset + length <= value.length) {
            final byte[] offsetValue = new byte[length];
            System.arraycopy(value, offset, offsetValue, 0, length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /**
     * Get prefix of data, equivalent to subdata(0, length)
     * @param length
     * @return
     */
    @Nullable
    public Data prefix(final int length) {
        return subdata(0, length);
    }

    @Nullable
    public Data suffix(final int from) {
        final int length = value.length - from;
        if (length < 0) {
            return null;
        }
        return subdata(from, length);
    }

    /**
     * Append data to end of this data.
     * @param data Data
     */
    public void append(@Nullable final Data data) {
        if (null == data) {
            return;
        }
        append(data.value);
    }

    private void append(@Nullable final byte[] data) {
        if (null == data) {
            return;
        }
        final byte[] concatenated = new byte[value.length + data.length];
        System.arraycopy(value, 0, concatenated, 0, value.length);
        System.arraycopy(data, 0, concatenated, value.length, data.length);
        value = concatenated;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
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
                (byte) ((value.value >> 24) & 0xFF)         // MSB
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
                ((value[index + 3] & 0xFF) << 24);
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
                (byte) ((value.value >> 56) & 0xFF) // MSB
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
                ((long) (value[index + 7] & 0xFF) << 56);
        return new Int64(v);
    }

    public void append(@Nullable final UUID value) {
        Data temp = new Data();
        temp.append(new Int64(value.getLeastSignificantBits()));
        temp.append(new Int64(value.getMostSignificantBits()));
        temp = temp.reversed();
        append(temp);
    }

    public UUID uuid(final int index) {
        if (index < 0 || index + 15 >= value.length) {
            return null;
        }
        Data temp = subdata(index,16);
        temp = temp.reversed();

        return new UUID(temp.int64(index + 8).value,temp.int64(index).value);
    }

    public void append(@Nullable final UIntBig value) {
        if (null == value) {
            return;
        }
        final short[] magnitude = value.magnitude();
        // Magnitude length
        append(new UInt32(magnitude.length));
        // Magnitude values
        for (int i=0; i<magnitude.length; i++) {
            append(new UInt16((int) magnitude[i] & 0xFFFF));
        }
    }

    public UIntBig uintBig(final int index) {
        if (index < 0) {
            return null;
        }
        final UInt32 length = uint32(index);
        if (length == null || length.value > Integer.MAX_VALUE) {
            return null;
        }
        final short[] magnitude = new short[(int) length.value];
        for (int i=0, j=index+4; i<magnitude.length; i++) {
            final UInt16 value = uint16(j);
            if (value == null) {
                return null;
            }
            magnitude[i] = (short) (value.value & 0xFFFF);
            j += 2;
        }
        return new UIntBig(magnitude);
    }

    public void append(@Nullable Float16 value) {
        if (null == value) {
            return;
        }
        append(value.bigEndian);
    }

    @Nullable
    public Float16 float16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        return new Float16(new Data(new byte[] {
                value[index], value[index + 1]
        }));
    }

    // MARK:- String to/from Data functions

    /**
     * Encoding option for data length data as prefix
     */
    public enum DataLengthEncodingOption {
        UINT8, UINT16, UINT32, UINT64
    }

    /**
     * Encode data, inserting length as prefix using UInt8,...,64.
     * @param value Data
     * @param encoding Encoding for length of data
     * @return True if successful, false otherwise.
     */
    @SuppressWarnings({"UnusedReturnValue", "ConstantConditions"})
    public boolean append(@Nullable final Data value, @NonNull final DataLengthEncodingOption encoding) {
        //noinspection ConstantConditions
        if (null == value || null == encoding) {
            return false;
        }
        final byte[] data = value.value;
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
            // Note: While Java byte array cannot exceed MAX_INT length, a developer is free to use
            // UINT64 (long) to represent byte array length. This is a deliberate design decision.
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

    /**
     * Encode string as data, inserting length as prefix using UInt8.
     * @param value String
     * @return True if successful, false otherwise
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean append(@Nullable final String value) {
        if (null == value) {
            return false;
        }
        return append(value, DataLengthEncodingOption.UINT8);
    }

    /**
     * Encode string as data, inserting length as prefix using UInt8,...,64.
     * @param value Data
     * @param encoding Encoding for length of data
     * @return True if successful, false otherwise.
     */
    public boolean append(@Nullable final String value, @NonNull final DataLengthEncodingOption encoding) {
        //noinspection ConstantConditions
        if (null == value || null == encoding) {
            return false;
        }
        byte[] data;
        try {
            data = value.getBytes(StandardCharsets.UTF_8);
        } catch (Throwable e) {
            return false;
        }
        if (null == data) {
            return false;
        }
        return append(new Data(data), encoding);
    }

    /**
     * Decoded data and start/end indices in data byte array
     */
    public final static class DecodedData {
        @NonNull
        public final Data value;
        public final int start;
        public final int end;

        public DecodedData(@Nullable final Data value, final int start, final int end) {
            if (null == value) {
                this.value = new Data();
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
    public DecodedData data(final int index, @NonNull final DataLengthEncodingOption encoding) {
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
            // Note: While Java byte array cannot exceed MAX_INT length, a developer is free to use
            // UINT64 (long) to represent byte array length. This is a deliberate design decision.
            // Bounds checks are included below to ensure byte array cannot exceed Java limits.
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
        //noinspection ConstantConditions
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
            final Data data = subdata((int) start, (int) (end - start));
            return new DecodedData(data == null ? new Data() : data, (int) start, (int) end);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Decoded string and start/end indices in data byte array.
     */
    public final static class DecodedString {
        @NonNull
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
        return string(index, DataLengthEncodingOption.UINT8);
    }

    @Nullable
    public DecodedString string(final int index, @NonNull final DataLengthEncodingOption encoding) {
        final DecodedData data = data(index, encoding);
        if (null == data) {
            return null;
        }
        try {
            final String string = new String(data.value.value, StandardCharsets.UTF_8);
            return new DecodedString(string, data.start, data.end);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Length of the byte array value.
     * @return Size of the value array in number of bytes. Returns 0 for null value array.
     */
    public int length() {
        if (null == value) {
            return 0;
        }
        return value.length;
    }

    /**
     * Alias for length(). They are identical.
     * @return Size of the value array in number of bytes.
     */
    public int size() {
        return length();
    }

    /**
     * Returns a new Data instance with the same data as this one, but in the reverse order
     * @return a new Data instance with the byte order reversed
     */
    public Data reversed() {
        Data reverseData = new Data();
        for (int i = length() - 1; i >= 0; --i) {
            reverseData.append(subdata(i,1));
        }
        return reverseData;
    }
}
