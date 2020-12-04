//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import android.util.Base64;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

/// Pseudo device address to enable caching of device payload without relying on device mac address
// that may change frequently like the A10 and A20.
public class PseudoDeviceAddress {
    public final long address;
    public final byte[] data;

    public PseudoDeviceAddress() {
        this(false);
    }

    /// Using secure random can cause blocking on app initialisation due to lack of entropy
    /// on some devices. Worst case scenario is app blocking upon initialisation, bluetooth
    /// power cycle, or advert refresh that occurs once every 15 minutes, leading to zero
    /// detection until sufficient entropy has been collected, which may take time given
    /// the device is likely to be idle. Not using secure random is acceptable and recommended
    /// in this instance because it is non-blocking and the sequence has sufficient uncertainty
    /// introduced programmatically to make an attack impractical from limited obeservations.
    protected PseudoDeviceAddress(final boolean useSecureRandom) {
        // Bluetooth device address is 48-bit (6 bytes), using
        // the same length to offer the same collision avoidance
        final long value = (useSecureRandom ? getSecureRandom().nextLong() : getRandomLong());
        this.data = encode(value);
        this.address = decode(this.data);
    }

    public PseudoDeviceAddress(final byte[] data) {
        this.data = data;
        this.address = decode(data);
    }

    /// Non-blocking random number generator with appropriate strength for this purpose
    protected final static long getRandomLong() {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.PseudoDeviceAddress");
        try {
            // Use a different instance with random seed from another sequence each time
            final Random random = new Random(Math.round(Math.random() * Long.MAX_VALUE));
            // Skip a random number of bytes from another sequence
            random.nextBytes(new byte[256 + (int) Math.round(Math.random() * 1024)]);
            return random.nextLong();
        } catch (Throwable e) {
            logger.fault("Could not retrieve Random instance", e);
            return Math.round(Math.random() * Long.MAX_VALUE);
        }
    }

    // Use a different instance each time, so you cannot infer a sequence
    protected final static SecureRandom getSecureRandom() {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.PseudoDeviceAddress");
        try {
            // Retrieve a SHA1PRNG
            final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            // Generate a secure seed
            final SecureRandom seedSr = new SecureRandom();
            // We need a 440 bit seed - see NIST SP800-90A
            final byte[] seed = seedSr.generateSeed(55);
            sr.setSeed(seed); // seed with random number
            // Securely generate bytes
            sr.nextBytes(new byte[256 + sr.nextInt(1024)]); // start from random position
            return sr;
        } catch (Throwable e) {
            logger.fault("Could not retrieve SHA1PRNG SecureRandom instance", e);
            return new SecureRandom();
        }
    }

    protected final static byte[] encode(final long value) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putLong(0, value);
        final byte[] data = new byte[6];
        System.arraycopy(byteBuffer.array(), 0, data, 0, data.length);
        return data;
    }

    protected final static long decode(final byte[] data) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getLong(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PseudoDeviceAddress that = (PseudoDeviceAddress) o;
        return address == that.address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return Base64.encodeToString(data, Base64.DEFAULT | Base64.NO_WRAP);
    }
}
