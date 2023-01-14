//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import org.junit.Test;

import java.io.PrintWriter;

import io.heraldprox.herald.sensor.TestUtil;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UInt8;

import static org.junit.Assert.assertEquals;

public class EncryptionTests {

    @Test
    public void testEncryption() {
        final Encryption encryption = new AES128();
        final EncryptionKey encryptionKey = new EncryptionKey("0800000017A6DD51E0869A46AB0DEB8D6399B942");
        for (int i=0; i<100; i++) {
            final Data data = new Data((byte) i, i);
            final Data encrypted = encryption.encrypt(data, encryptionKey);
            final Data decrypted = encryption.decrypt(encrypted, encryptionKey);
            assertEquals(data, decrypted);
        }
    }

    @Test
    public void testCrossPlatform() throws Exception {
        // Need to make IV constant for cross platform test
        final Encryption encryption = new AES128(new TestRandomFunction(new UInt8(0)));
        final EncryptionKey encryptionKey = new EncryptionKey("0800000017A6DD51E0869A46AB0DEB8D6399B942");
        final PrintWriter out = TestUtil.androidPrintWriter("encryption.csv");
        out.println("key,encrypted,decrypted");
        for (int i=0; i<100; i++) {
            final Data data = new Data((byte) i, i);
            final Data encrypted = encryption.encrypt(data, encryptionKey);
            final Data decrypted = encryption.decrypt(encrypted, encryptionKey);
            assertEquals(data, decrypted);
            out.println(i+","+encrypted.hexEncodedString()+","+decrypted.hexEncodedString());
        }
        out.flush();
        out.close();
        // Pending iOS implementation
        TestUtil.assertEqualsCrossPlatform("encryption.csv");
    }
}
