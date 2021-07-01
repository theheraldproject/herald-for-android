//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * IEE 754 binary16 format 16-bit float.
 */
public class Float16 implements DoubleValue {
    public final float value;
    @NonNull
    public final Data bigEndian;

    public Float16(final float value) {
        this.value = value;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) float16(value));
        this.bigEndian = new Data(byteBuffer.array());
    }

    public Float16(@NonNull final Data bigEndian) {
        this.bigEndian = bigEndian;
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bigEndian.value);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        final int value = byteBuffer.getShort(0);
        this.value = valueOf(value);
    }

    /**
     * Float value to Float16 where the last 16 bits of integer value contains Float16
     * @param values Float value
     * @return Last 16 bits of integer value contains Float16
     */
    private static int float16(final float values) {
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

    private static float valueOf(final int hbits) {
        int mant = hbits & 0x03ff;
        int exp = hbits & 0x7c00;
        if (0x7c00 == exp) {
            exp = 0x3fc00;
        } else if (exp != 0) {
            exp += 0x1c000;
            if (0 == mant && exp > 0x1c400) {
                return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
            }
        } else if (mant != 0) {
            exp = 0x1c400;
            do {
                mant <<= 1;
                exp -= 0x400;
            } while (0 == (mant & 0x400));
            mant &= 0x3ff;
        }
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | (exp | mant) << 13);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
