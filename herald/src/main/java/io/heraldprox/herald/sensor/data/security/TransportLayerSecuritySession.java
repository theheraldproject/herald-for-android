//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.datatype.UInt32;

public class TransportLayerSecuritySession {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.Security.TransportLayerSecuritySession");
    // Security primitives
    @NonNull
    private final KeyExchange keyExchange;
    @NonNull
    private final Integrity integrity;
    @NonNull
    private final Encryption encryption;
    // Own key pair and session ID derived from public key
    @NonNull
    public final TransportLayerSecuritySessionID ownId;
    @NonNull
    public final KeyExchangePublicKey ownPublicKey;
    @NonNull
    private final KeyExchangePrivateKey ownPrivateKey;
    // Peer public key and peer session ID derived from peer public key
    @Nullable
    public TransportLayerSecuritySessionID peerId = null;
    @Nullable
    private KeyExchangePublicKey peerPublicKey = null;
    // Shared key derived from key exchange
    @Nullable
    private EncryptionKey sharedKey = null;
    // Session expiry criteria
    @NonNull
    private final Date timestamp = new Date();
    private int encryptCounter = 0;
    public boolean expired() {
        return encryptCounter > 0 || new TimeInterval(new Date().secondsSinceUnixEpoch() - timestamp.secondsSinceUnixEpoch()).value > TimeInterval.minutes(5).value;
    }

    public TransportLayerSecuritySession(@NonNull final KeyExchange keyExchange, @NonNull final Integrity integrity, @NonNull final Encryption encryption) {
        this.keyExchange = keyExchange;
        this.integrity = integrity;
        this.encryption = encryption;
        final KeyExchangeKeyPair ownKeyPair = keyExchange.keyPair();
        this.ownPrivateKey = ownKeyPair.privateKey;
        this.ownPublicKey = ownKeyPair.publicKey;
        this.ownId = new TransportLayerSecuritySessionID(integrity.hash(ownPublicKey).uint32(0));
    }

    @Nullable
    public TransportLayerSecuritySessionID establishSession(@NonNull final KeyExchangePublicKey peerPublicKey) {
        final KeyExchangeSharedKey sharedKey = keyExchange.sharedKey(ownPrivateKey, peerPublicKey);
        if (null == sharedKey) {
            logger.fault("establishSession failed, cannot derive shared key (ownPrivateKeyCount={},peerPublicKeyCount={})", ownPrivateKey.value.length, peerPublicKey.value.length);
            return null;
        }
        final UInt32 peerIdValue = integrity.hash(peerPublicKey).uint32(0);
        if (null == peerIdValue) {
            logger.fault("establishSession failed, cannot derive peer ID (peerPublicKeyCount={})", peerPublicKey.value.length);
            return null;

        }
        this.peerId = new TransportLayerSecuritySessionID(peerIdValue);
        this.peerPublicKey = peerPublicKey;
        this.sharedKey = new EncryptionKey(sharedKey);
        return this.peerId;
    }

    @Nullable
    public Data encrypt(@NonNull final Data data) {
        final EncryptionKey encryptionKey = sharedKey;
        if (null == encryptionKey) {
            logger.fault("encrypt failed, missing shared encryption key");
            return null;
        }
        // Encrypt data using shared key
        final Data encryptedData = encryption.encrypt(data, encryptionKey);
        if (null == encryptedData) {
            logger.fault("encrypt failed, cannot encrypt data with shared encryption key (dataCount={},encryptionKeyCount={})", data.value.length, encryptionKey.value.length);
            return null;
        }
        // Update usage count to prevent over use of the same encryption key
        encryptCounter++;
        return encryptedData;
    }

    @Nullable
    public Data decrypt(@NonNull final Data data) {
        final EncryptionKey encryptionKey = sharedKey;
        if (null == encryptionKey) {
            logger.fault("decrypt failed, missing shared encryption key");
            return null;
        }
        // Decrypt data using shared key
        final Data decryptedData = encryption.decrypt(data, encryptionKey);
        if (null == decryptedData) {
            logger.fault("decrypt failed, cannot decrypt data with shared encryption key (dataCount={},encryptionKeyCount={})", data.value.length, encryptionKey.value.length);
            return null;
        }
        return decryptedData;
    }
}
