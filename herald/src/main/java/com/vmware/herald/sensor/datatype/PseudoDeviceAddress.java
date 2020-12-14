//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import com.vmware.herald.sensor.ble.BLESensorConfiguration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/// Pseudo device address to enable caching of device payload without relying on device mac address
// that may change frequently like the A10 and A20.
public class PseudoDeviceAddress {
    private static RandomSource randomSource = new RandomSource(RandomSource.Method.Random);
    public final long address;
    public final byte[] data;

    /// Pseudo device address is generated from an adaptation of Random, not SecureRandom.
    /// This is necessary to avoid app blocking caused by SecureRandom on idle devices with
    /// limited entropy. SecureRandom uses /dev/urandom (derived from /dev/random) as random
    /// seed source which is a shared resource that when exhausted causes disruption across
    /// the whole system beyond the app. /dev/random gathers entropy from system events such
    /// as storage activities and user actions which can easily become exhausted on idle
    /// devices. Tests have shown blocking can start within 4 to 8 hours, and time to recover
    /// increases over time, leading the app and underlying services to eventually halt.
    /// The same issue has been observed on both mobile and server hardware, hence the use
    /// of SecureRandom should be reserved for the production of strong encryption keys on
    /// rare occasions, rather than repeated use in the production of ephemeral time limited
    /// address data.
    /// Given /dev/random is easily exhausted on idle mobile devices because entropy is gathered
    /// from a specific set of events (e.g. boot up, storage, user activities) that should
    /// normally occur more frequently than encryption key generation requests. A similar
    /// approach can be taken to adapt Random to use an entropy source that is guaranteed to
    /// be non-exhaustive in this context, thus avoiding the blocking issue while achieving
    /// appropriate strength that is fit for purpose. The adaptation takes advantage of the
    /// continuous running nature of the proximity detection process where timing events are
    /// highly variable due to external factors such as OS state, phones in the vicinity,
    /// Bluetooth connection time, and other system processes. The result is a reliable
    /// entropy source that is sufficiently challenging to predict for this purpose.
    /// In short, an attacker will need to establish the seed of Math.random() based on
    /// limited observations of truncated values where any number of values and bits could
    /// have been skipped between observations and from initialisation. While cracking the
    /// seed of Random is trivial when the value index is known (i.e. it simply confirms
    /// Random value sequence is deterministic, given the seed, as per Java documentation),
    /// it is significantly more challenging when the position data is unknown, and information
    /// has been discarded (truncated) in the process, especially when the position is in
    /// essence selected by a highly random process.
    /// In practice, using secure random can cause blocking on app initialisation, bluetooth
    /// power cycle, advert refresh that occurs once every 15 minutes, and also blocking of
    /// underlying system services which impacts a wide range of services including Bluetooth
    /// operation and garbage collection, leading to zero detection until sufficient entropy has
    /// been collected, which will take increasing time when the device is idle.
    public PseudoDeviceAddress(final RandomSource.Method method) {
        // Bluetooth device address is 48-bit (6 bytes), using
        // the same length to offer the same collision avoidance
        // Choose between random, secure random singleton, secure random, and NIST compliant secure random as random source
        // - Random is non-blocking and has been adapted to obtain entropy from reliable source in this context. It is
        //   sufficiently secure for this purpose, validated and recommended.
        // - SecureRandomSingleton is blocking after 4-8 hours on idle devices and inappropriate for this use case, not recommended
        // - SecureRandom is blocking after 4-8 hours on idle devices and inappropriate for this use case, not recommended
        // - SecureRandomNIST is block after 6 hours on idle devices and inappropriate for this use case, not recommended
        if (randomSource == null || randomSource.method != method) {
            randomSource = new RandomSource(method);
        }
        this.data = encode(randomSource.nextLong());
        this.address = decode(this.data);
    }

    /// Default constructor uses Random as random source, see above and RandomSource for details.
    public PseudoDeviceAddress() {
        this(BLESensorConfiguration.pseudoDeviceAddressRandomisation);
    }

    public PseudoDeviceAddress(final byte[] data) {
        this.data = data;
        this.address = decode(data);
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
        return Base64.encode(data);
    }
}
