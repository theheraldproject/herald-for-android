//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.util.Arrays;
import java.util.Random;

/// Mutable unsigned integer of unlimited size (for 32-bit architectures)
public class UIntBig {
    // Unsigned value (LSB ... MSB)
    private short[] magnitude;
    // Common values
    private final static short[] magnitudeZero = new short[0];
    private final static short zero = (short) 0;
    private final static short one = (short) 1;

    /// From raw data
    public UIntBig(final short[] magnitude) {
        this.magnitude = magnitude;
    }

    /// Zero
    public UIntBig() {
        this(magnitudeZero);
    }

    /// Deep copy of given value
    private UIntBig(final UIntBig value) {
        this(new short[value.magnitude.length]);
        System.arraycopy(value.magnitude, 0, magnitude, 0, magnitude.length);
    }

    /// UInt64 value as unlimited value
    public UIntBig(final long uint64) {
        final short[] value = new short[]{
                (short) (uint64 & 0xFFFF),          // LSB
                (short) ((uint64 >>> 16) & 0xFFFF),
                (short) ((uint64 >>> 32) & 0xFFFF),
                (short) ((uint64 >>> 48) & 0xFFFF)  // MSB
        };
        magnitude = trimZeroMSBs(value);
    }

    /// Hex encoded string with format MSB...LSB
    public UIntBig(final String hexEncodedString) {
        // Pad MSB with zeros until length % 4 == 0
        final StringBuilder hex = new StringBuilder(hexEncodedString.length() + 4);
        while ((hex.length() + hexEncodedString.length()) % 4 != 0) {
            hex.append('0');
        }
        hex.append(hexEncodedString);
        // Convert to bytes MSB...LSB
        final Data data = Data.fromHexEncodedString(hex.toString());
        // Convert to short LSB...MSB
        magnitude = new short[data.value.length / 2];
        for (int i=0, j=data.value.length, n; i<magnitude.length; i++) {
            n = (int) data.value[--j] & 0xFF;
            n |= ((int) (data.value[--j] & 0xFF) << 8);
            n &= 0xFFFF;
            magnitude[i] = (short) n;
        }
    }

    public UIntBig(final int bitLength, final Random random) {
        magnitude = new short[(bitLength + 15) / 16];
        final byte[] bytes = new byte[2];
        int value, remaining=bitLength;
        for (int i=0; i<magnitude.length && remaining>0; i++) {
            random.nextBytes(bytes);
            value = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
            if (remaining < 16) {
                value = value >>> (16 - remaining);
                remaining = 0;
            } else {
                remaining -= 16;
            }
            magnitude[i] = (short) (value & 0xFFFF);
        }
    }

    public String hexEncodedString() {
        final Data data = new Data();
        for (int i=magnitude.length; i-->0;) {
            data.append(new UInt8((magnitude[i] >>> 8) & 0xFF));
            data.append(new UInt8(magnitude[i] & 0xFF));
        }
        // Strip leading zeros
        int offset = 0;
        while (offset<data.value.length && data.value[offset] == 0) {
            offset++;
        }
        final Data unpadded = (offset == 0 ? data : data.subdata(offset));
        return unpadded.hexEncodedString();
    }

    public short[] magnitude() {
        return magnitude;
    }

    /// Get unsigned long value
    public long uint64() {
        long value = 0;
        if (magnitude.length >= 1) {
            value = (long) magnitude[0] & 0xFFFF;
        }
        if (magnitude.length >= 2) {
            value |= (((long) magnitude[1] & 0xFFFF) << 16);
        }
        if (magnitude.length >= 3) {
            value |= (((long) magnitude[2] & 0xFFFF) << 32);
        }
        if (magnitude.length >= 4) {
            value |= (((long) magnitude[3] & 0xFFFF) << 48);
        }
        return value;
    }

    /// Test if value is zero
    protected boolean isZero() {
        return magnitude.length == 0;
    }

    /// Test if value is one
    protected boolean isOne() {
        if (magnitude.length != 1) {
            return false;
        }
        return magnitude[0] == one;
    }

    /// Test if value is odd
    protected boolean isOdd() {
        if (magnitude.length == 0) {
            return false;
        }
        return ((short) (magnitude[0] & 0x01)) == one;
    }

    /// Modular exponentiation r = (a ^ b) % c where
    /// - a: self
    /// - b: exponent
    /// - c: modulus
    /// Performance test shows software implementation is acceptably slower than native hardware
    /// - Test samples = 399,626,333
    /// - Native 64-bit hardware = 416ns/call
    /// - Software 32-bit implementation = 3613ns/call
    ///
    /// @return r, the result
    public UIntBig modPow(final UIntBig exponent, final UIntBig modulus) {
        if (modulus.isZero()) {
            return new UIntBig();
        }
        if (exponent.isOne()) {
            final UIntBig base = new UIntBig(this);
            base.mod(modulus);
            return base;
        }
        final UIntBig result = new UIntBig(one);
        final UIntBig base = new UIntBig(this);
        base.mod(modulus);
        final UIntBig exp = new UIntBig(exponent);
        while (!exp.isZero()) {
            if (exp.isOdd()) {
                result.times(base);
                result.mod(modulus);
            }
            exp.rightShiftByOne();
            base.times(base);
            base.mod(modulus);
        }
        return result;
    }

    /// Replace self with self % modulus
    protected void mod(final UIntBig modulus) {
        final short[] a = modulus.magnitude;;
        final short[] b = magnitude;
        mod(a, b);
        magnitude = trimZeroMSBs(b);
    }

    /// Reduce b until b < a at offset by repeatedly deducting a from b at offset
    /// Assumes b.length >= a.length + offset
    protected final static void reduce(final short[] a, final short[] b, final int offset) {
        final int valueA = ((int) a[a.length - 1] & 0xFFFF) + (a.length > 1 ? 1 : 0);
        int valueB, carry, quotient;
        short multiplier;
        carry = (a.length + offset < b.length ? (int) b[a.length + offset] & 0xFFFF : 0);
        valueB = (int) b[a.length + offset - 1] & 0xFFFF;
        while (carry != 0 || valueB >= valueA || (offset == 0 && compare(a, b) <= 0)) {
            if (carry > Short.MAX_VALUE) {
                quotient = Short.MAX_VALUE;
            } else if (carry > 0) {
                valueB = (carry << 16 | valueB);
                quotient = valueB / valueA;
            } else {
                quotient = valueB / valueA;
                if (quotient == 0) {
                    quotient = 1;
                }
            }
            multiplier = (quotient > Short.MAX_VALUE ? Short.MAX_VALUE : (short) (quotient & 0xFFFF));
            subtract(a, multiplier, b, offset);
            carry = (a.length + offset < b.length ? (int) b[a.length + offset] & 0xFFFF : 0);
            valueB = (int) b[a.length + offset - 1] & 0xFFFF;
        }
    }

    /// Modular function : b % a
    protected final static void mod(final short[] a, final short[] b) {
        for (int offset=b.length - a.length + 1; offset-->0;) {
            reduce(a, b, offset);
        }
    }

    /// Compare a and b, ignoring leading zeros
    /// @return -1 for a < b, 0 for a == b, 1 for a > b
    protected final static int compare(final short[] a, final short[] b) {
        int i=a.length-1, j=b.length-1;
        while (i >= 0 && a[i] == 0) {
            i--;
        }
        while (j >= 0 && b[j] == 0) {
            j--;
        }
        if (i < j) {
            return -1;
        }
        if (i > j) {
            return 1;
        }
        // i == j, switching to i as index
        int valueA, valueB;
        while (i >= 0) {
            valueA = (int) a[i] & 0xFFFF;
            valueB = (int) b[i] & 0xFFFF;
            if (valueA < valueB) {
                return -1;
            }
            if (valueA > valueB) {
                return 1;
            }
            i--;
        }
        return 0;
    }

    /// Compare self with given value
    /// @return -1 for self < value, 0 for self == value, 1 for self > value
    protected int compare(final UIntBig value) {
        return compare(magnitude, value.magnitude);
    }

    /// Subtraction function : b - a * multiplier (at offset of b)
    /// Note, multiplier range is [0,32767]
    protected final static int subtract(final short[] a, final short multiplier, final short[] b, final int offset) {
        final int times = (int) multiplier & 0xFFFF;
        int valueA, valueB, valueAL, valueAH, result, carry = 0;
        for (int i=0; i<a.length; i++) {
            valueA = (int) a[i] & 0xFFFF;
            valueB = (int) b[i+offset] & 0xFFFF;
            valueA *= times;
            valueAL = valueA & 0xFFFF;
            valueAH = valueA >>> 16;
            result = valueB - valueAL - carry;
            carry = valueAH;
            while (result < 0) {
                result += 0x00010000;
                carry++;
            }
            b[i+offset] = (short) (result & 0xFFFF);
        }
        for (int i=a.length+offset; i<b.length && carry>0; i++) {
            valueB = (int) b[i] & 0xFFFF;
            result = valueB - carry;
            carry = 0;
            while (result < 0) {
                result += 0x00010000;
                carry++;
            }
            b[i] = (short) (result & 0xFFFF);
        }
        return carry;
    }

    /// Replace self with self - value * multiplier (at offset of self)
    /// Note, multiplier range is [0,32767]
    protected int minus(final UIntBig value, final short multiplier, final int offset) {
        final short[] a = value.magnitude;
        final short[] b = magnitude;
        final int underflow = subtract(a, multiplier, b, offset);
        magnitude = trimZeroMSBs(b);
        return underflow;
    }

    /// Replace self with self * multiplier
    protected void times(final UIntBig multiplier) {
        final short[] a = magnitude;
        final short[] b = multiplier.magnitude;
        final short[] product = new short[a.length + b.length];
        int valueA, valueB, carry, carried;
        for (int i=0; i<a.length; i++) {
            valueA = (int) a[i] & 0xFFFF;
            carry = 0;
            for (int j=0; j<b.length; j++) {
                valueB = (int) b[j] & 0xFFFF;
                carried = (int) product[i+j] & 0xFFFF;
                carry += valueA * valueB + carried;
                product[i+j] = (short) (carry & 0xFFFF);
                carry >>>= 16;
            }
            product[i+b.length] = (short) (carry & 0xFFFF);
        }
        magnitude = trimZeroMSBs(product);
    }

    /// Right shift all bits by one bit and insert leading 0 bit
    protected void rightShiftByOne() {
        if (isZero()) {
            return;
        }
        if (isOne()) {
            magnitude = magnitudeZero;
            return;
        }
        for (int i=0; i<magnitude.length-1; i++) {
            magnitude[i] = (short) ((magnitude[i] >>> 1) & 0x7FFF);
            magnitude[i] |= (short) ((magnitude[i+1] << 15) & 0x8000);
        }
        magnitude[magnitude.length - 1] = (short) ((magnitude[magnitude.length - 1] >>> 1) & 0x7FFF);
        magnitude = trimZeroMSBs(magnitude);
    }

    /// Remove leading zeros from array
    protected final static short[] trimZeroMSBs(final short[] magnitude) {
        int i = magnitude.length - 1;
        for (; i>0 && magnitude[i] == zero; i--);
        if (i == 0 && magnitude[0] == zero) {
            return magnitudeZero;
        }
        if (i == magnitude.length - 1) {
            return magnitude;
        }
        final short[] trimmed = new short[i + 1];
        System.arraycopy(magnitude, 0, trimmed, 0, trimmed.length);
        return trimmed;
    }

    /// Count of bits based on highest set bit
    public int bitLength() {
        if (magnitude.length == 0) {
            return 0;
        }
        int length = (magnitude.length > 1 ? (magnitude.length - 1) * 16 : 0);
        int msb = (int) magnitude[magnitude.length - 1] & 0xFFFF;
        while (msb != 0) {
            msb = msb >>> 1;
            length++;
        }
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UIntBig uIntBig = (UIntBig) o;
        return Arrays.equals(magnitude, uIntBig.magnitude);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(magnitude);
    }
}
