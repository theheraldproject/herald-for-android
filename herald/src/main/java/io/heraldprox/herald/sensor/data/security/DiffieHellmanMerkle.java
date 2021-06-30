//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.UIntBig;

/**
 * Diffie-Hellman-Merkle key exchange using NCSC Foundation Profile MODP group 14 (2048-bit) by default
 */
public class DiffieHellmanMerkle implements KeyExchange {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.Security.DiffieHellmanMerkle");
    @NonNull
    private final PseudoRandomFunction random;

    /**
     * Parameters for Diffie-Hellman key agreement
     * NCSC Foundation Profile for TLS requires key exchange using
     * DH Group 14 (2048-bit MODP Group) - which is RFC3526 MODP Group 14
     */
    @NonNull
    private final DiffieHellmanParameters parameters;

    public DiffieHellmanMerkle(@NonNull final DiffieHellmanParameters parameters, @NonNull final PseudoRandomFunction random) {
        this.parameters = parameters;
        this.random = random;
    }

    public DiffieHellmanMerkle(@NonNull final DiffieHellmanParameters parameters) {
        this(parameters, new SecureRandomFunction());
    }

    public DiffieHellmanMerkle() {
        this(DiffieHellmanParameters.modpGroup14, new SecureRandomFunction());
    }

    // MARK: - KeyExchange

    @NonNull
    @Override
    public KeyExchangeKeyPair keyPair() {
        final UIntBig privateKey = new UIntBig(parameters.p.bitLength() - 2, random);
        final KeyExchangePrivateKey privateKeyData = new KeyExchangePrivateKey(privateKey.data());
        // publicKey = (base ^ exponent) % modulus = (g ^ privateKey) % p
        final UIntBig base = parameters.g;
        //noinspection UnnecessaryLocalVariable
        final UIntBig exponent = privateKey;
        final UIntBig modulus = parameters.p;
        final UIntBig publicKey = base.modPow(exponent, modulus);
        final KeyExchangePublicKey publicKeyData = new KeyExchangePublicKey(publicKey.data());
        return new KeyExchangeKeyPair(privateKeyData, publicKeyData);
    }

    @Nullable
    @Override
    public KeyExchangeSharedKey sharedKey(@NonNull KeyExchangePrivateKey own, @NonNull KeyExchangePublicKey peer) {
        // sharedKey = (base ^ exponent) % modulus = (peerPublicKey ^ ownPrivateKey) % p
        try {
            final UIntBig base = new UIntBig(peer);
            final UIntBig exponent = new UIntBig(own);
            final UIntBig modulus = parameters.p;
            final UIntBig sharedKey = base.modPow(exponent, modulus);
            //noinspection UnnecessaryLocalVariable
            final KeyExchangeSharedKey sharedKeyData = new KeyExchangeSharedKey(sharedKey.data());
            return sharedKeyData;
        } catch (Throwable e) {
            return null;
        }
    }

    // MARK: - Optional in-situ test functions

    /**
     * Run performance test on phone hardware.
     * Note : Use release build for performance tests as it is generally faster than debug build
     */
    public void performanceTest(final int samples) {
        long timeKeyPair = 0;
        long timeSharedKey = 0;
        long timeRoundtrip = 0;
        for (int i=0; i<samples; i++) {
            // Roundtrip key generation and exchange
            final long t0 = System.nanoTime();
            final KeyExchangeKeyPair aliceKeyPair = keyPair();
            final long t1 = System.nanoTime();
            final KeyExchangeKeyPair bobKeyPair = keyPair();
            final long t2 = System.nanoTime();
            final KeyExchangeSharedKey aliceSharedKey = sharedKey(aliceKeyPair.privateKey, bobKeyPair.publicKey);
            final long t3 = System.nanoTime();
            final KeyExchangeSharedKey bobSharedKey = sharedKey(bobKeyPair.privateKey, aliceKeyPair.publicKey);
            final long t4 = System.nanoTime();
            if (null == aliceSharedKey) {
                logger.fault("performanceTest, alice key is null");
                continue;
            }
            if (null == bobSharedKey) {
                logger.fault("performanceTest, bob key is null");
                continue;
            }
            if (!aliceSharedKey.equals(bobSharedKey)) {
                logger.fault("performanceTest, shared key mismatch");
                continue;
            }
            // Update time counters
            timeKeyPair += (t1-t0);
            timeKeyPair += (t2-t1);
            timeSharedKey += (t3-t2);
            timeSharedKey += (t4-t3);
            timeRoundtrip += (t4-t0);
        }
        logger.debug("performanceTest (samples={},roundTrip={}ns,keyPair={}ns,sharedKey={}ns)",
            samples, (timeRoundtrip / samples), (timeKeyPair / (samples * 2)), (timeSharedKey / (samples * 2)));
    }
}

