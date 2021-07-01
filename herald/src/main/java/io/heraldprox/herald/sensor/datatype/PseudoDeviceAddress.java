//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;

import java.util.Objects;

/**
 * Pseudo device address to enable caching of device payload without relying on device mac address
 * that may change frequently like the A10 and A20.
 *
 * Pseudo device address is by default generated from an adaptation SecureRandom.
 * This is necessary to avoid app blocking caused by SecureRandom on idle devices with
 * limited entropy. SecureRandom uses /dev/urandom (derived from /dev/random) as random
 * seed source which is a shared resource that when exhausted causes disruption across
 * the whole system beyond the app. /dev/random gathers entropy from system events such
 * as storage activities and user actions which can easily become exhausted on idle
 * devices. Tests have shown blocking can start within 4 to 8 hours, and time to recover
 * increases over time, leading the app and underlying services to eventually halt.
 * The same issue has been observed on both mobile and server hardware, hence the use
 * of SecureRandom should be reserved for the production of strong encryption keys on
 * rare occasions, rather than repeated use in the production of ephemeral time limited
 * address data.
 * Given /dev/random is easily exhausted on idle mobile devices because entropy is gathered
 * from a specific set of events (e.g. boot up, storage, user activities) that should
 * normally occur more frequently than encryption key generation requests. A similar
 * approach can be taken to adapt SecureRandom to use an entropy source that is guaranteed
 * to be non-exhaustive in this context, thus avoiding the blocking issue while achieving
 * appropriate strength that is fit for purpose. The adaptation takes advantage of the
 * continuous running nature of the proximity detection process where timing and detection
 * events are highly variable due to external factors such as OS state, phones in the
 * vicinity,  Bluetooth connection time, and other system processes. The result is a
 * reliable entropy source that is sufficiently challenging to predict for this purpose.
 **/
public class PseudoDeviceAddress {
    public final long address;
    @NonNull
    public final byte[] data;

    /**
     * Generates a random PseudoDeviceAddress based on the requested RandomSource
     * @param randomSource The random source to use for PseudoDeviceAddress generation, the
     *                     recommended source is NonBlockingSecureRandom
     */
    public PseudoDeviceAddress(@NonNull final RandomSource randomSource) {
        // Bluetooth device address is 48-bit (6 bytes), using the same length to offer the same collision avoidance
        // Recommended random source is NonBlockingSecureRandom
        // - All included random sources obtain truly random entropy data from reliable sources in the application context.
        //   - 1. Random source call time is determined by a combination of this device's time keeping and also other
        //        devices' performance which are both variable and unpredictable at nano time scale.
        //   - 2. BLE MAC address of target devices and detection time are unpredictable as they are both dependent on
        //        user environment and activities, and also behaviour of other users. The order of detection is unpredictable,
        //        and the actual address is also derived from the other device's SecureRandom source.
        // - Non-blocking random sources are recommended for the target application domain as a blocking event caused by
        //   exhaustion of entropy will impact wider system (e.g. BLE MAC address generation) and prevent detection of
        //   target devices, thus impacts risk estimation in disease control.
        //   - 1. NonBlockingPRNG will never block, as it is based on a combination of Random, and entropy data. It is
        //        sufficiently secure for the target domain, but now superceded by NonBlockingSecureRandom.
        //   - 2. NonBlockingCSPRNG will never block, as it is based on a combination of Random, SHA256, and entropy data.
        //        It improves on NonBlockingPRNG by offering cryptographic separation of observable random data and
        //        internal state. This is also sufficiently secure for the target domain, but now superceded by
        //        NonBlockingSecureRandom.
        //   - 3. NonBlockingSecureRandom will never block. It is based on a combination of SHA256, xor, truncation, and
        //        self-populating time-based entropy data. This is now the recommended random source for generating
        //        pseudo device addresses.
        // - Blocking random sources are not recommended for the target application, and only provided here to support
        //   applications with strict security compliance requirements, or when the device is known to be frequently
        //   active, thus the chance of exhausting system entropy is low.
        //   - 1. BlockingSecureRandomSingleton is blocking after 4-8 hours on idle devices.
        //   - 2. BlockingSecureRandom is blocking after 4-8 hours on idle devices.
        //   - 3. BlockingSecureRandomNIST is blocking after 6 hours on idle devices.
        // Encoder will discard the first two of the 8 bytes as the address is only 6 bytes long
        this.data = encode(randomSource.nextLong());
        this.address = decode(this.data);
    }

    /**
     * Default constructor uses NonBlockingSecureRandom as random source, see above and RandomSource for details.
     */
    public PseudoDeviceAddress() {
        this(BLESensorConfiguration.pseudoDeviceAddressRandomisation);
    }

    /**
     * Constructs a PseudoDeviceAddress from externally generated data.
     * @param data Externally generated data (either real address, or should be securely randomly generated)
     */
    public PseudoDeviceAddress(@NonNull final byte[] data) {
        this.address = decode(data);
        this.data = encode(this.address);
    }

    /**
     * Constructs a PseudoDeviceAddress from externally generated data.
     * @param value Externally generated data (either real address, or should be securely randomly generated)
     */
    public PseudoDeviceAddress(final long value) {
        this.data = encode(value);
        this.address = decode(data);
    }

    @NonNull
    protected static byte[] encode(final long value) {
        final Data encoded = new Data();
        encoded.append(new Int64(value));
        //noinspection ConstantConditions
        return encoded.subdata(2, 6).value;
    }

    protected static long decode(@NonNull final byte[] data) {
        final Data decoded = new Data((byte) 0, 2);
        decoded.append(new Data(data));
        if (decoded.value.length < 8) {
            decoded.append(new Data((byte) 0, 8 - decoded.value.length));
        }
        final Int64 int64 = decoded.int64(0);
        return (null == int64 ? 0 : int64.value);
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        PseudoDeviceAddress that = (PseudoDeviceAddress) o;
        return address == that.address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @NonNull
    @Override
    public String toString() {
        return Base64.encode(data);
    }
}
