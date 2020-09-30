package com.vmware.herald.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// IEE 754 binary16 format 16-bit float
public class Float16 {
    public final float value;
    public final Data bigEndian;

    public Float16(float value) {
        this.value = value;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) float16(value));
        this.bigEndian = new Data(byteBuffer.array());
    }

    /// Float value to Float16 where the last 16 bits of integer value contains Float16
    private static int float16(float values) {
        final int bits = Float.floatToIntBits(values);
        final int sign = bits >>> 16 & 0x8000;
        int val = (bits & 0x7fffffff) + 0x1000;
        if (val >= 0x47800000) {
            if ((bits & 0x7fffffff) >= 0x47800000) {
                if (val < 0x7f800000) {
                    return sign | 0x7c00;
                }
                return sign | 0x7c00 | (bits & 0x007fffff) >>> 13;
            }
            return sign | 0x7bff;
        }
        if (val >= 0x38800000) {
            return sign | val - 0x38000000 >>> 13;
        }
        if (val < 0x33000000) {
            return sign;
        }
        val = (bits & 0x7fffffff) >>> 23;
        return sign | ((bits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
    }
}
