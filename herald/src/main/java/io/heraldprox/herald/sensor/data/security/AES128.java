//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;

/**
 * AES128 encryption algorithm
 */
public class AES128 implements Encryption {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.Security.AES128");
    @NonNull
    private final PseudoRandomFunction random;

    public AES128(@NonNull final PseudoRandomFunction pseudoRandomFunction) {
        this.random = pseudoRandomFunction;
    }

    public AES128() {
        this(new SecureRandomFunction());
    }

    @Nullable
    @Override
    public Data encrypt(@NonNull Data data, @NonNull EncryptionKey with) {
        // Convert key data to hash to ensure key length is 256 bits
        final Data keyData = new SHA256().hash(with);
        final SecretKey key = new SecretKeySpec(keyData.value, "AES");
        // Generate random initialisation vector (128 bits = 16 bytes)
        final Data ivData = new Data((byte) 0, 16);
        if (!random.nextBytes(ivData)) {
            logger.fault("encrypt failed, cannot generate random IV");
            return null;
        }
        final IvParameterSpec iv = new IvParameterSpec(ivData.value);
        // Encrypt data with key and iv
        try {
            final Cipher cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherEncrypt.init(Cipher.ENCRYPT_MODE, key, iv);
            final byte[] encrypted = cipherEncrypt.doFinal(data.value);
            if (null == encrypted) {
                logger.fault("encrypt failed, cannot encrypt data");
                return null;
            }
            final Data encryptedData = new Data(encrypted);
            // Build result = iv + encrypted
            final Data result = new Data();
            result.append(ivData);
            result.append(encryptedData);
            return result;
        } catch (Throwable e) {
            logger.fault("encrypt failed, exception", e);
            return null;
        }
    }

    @Nullable
    @Override
    public Data decrypt(@NonNull Data data, @NonNull EncryptionKey with) {
        // Convert key data to hash to ensure key length is 256 bits
        final Data keyData = new SHA256().hash(with);
        final SecretKey key = new SecretKeySpec(keyData.value, "AES");
        // Get iv from first 16 bytes of data
        if (!(data.value.length > 16)) {
            logger.fault("decrypt failed, cannot decode IV data fragment");
            return null;
        }
        final Data ivData = data.prefix(16);
        final IvParameterSpec iv = new IvParameterSpec(ivData.value);
        // Get encrypted data after iv
        final Data encryptedData = data.suffix(16);
        if (null == encryptedData) {
            logger.fault("decrypt failed, cannot decode encrypted data fragment");
            return null;
        }
        // Decrypt data
        try {
            final Cipher cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherEncrypt.init(Cipher.DECRYPT_MODE, key, iv);
            final byte[] decrypted = cipherEncrypt.doFinal(encryptedData.value);
            if (null == decrypted) {
                logger.fault("decrypt failed, cannot decrypted data");
                return null;
            }
            final Data decryptedData = new Data(decrypted);
            return decryptedData;
        } catch (Throwable e) {
            logger.fault("decrypt failed, exception", e);
            return null;
        }
    }
}
