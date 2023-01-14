//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import org.junit.Test;

import java.io.PrintWriter;

import io.heraldprox.herald.sensor.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KeyExchangeTests {

    @Test
    public void testKeyPair() {
        final KeyExchange keyExchange = new DiffieHellmanMerkle(DiffieHellmanParameters.random128);
        final KeyExchangeKeyPair keyPair = keyExchange.keyPair();
        // Note: Private/Public key data always starts with 08000000 here because the first 4 bytes is the key length
        System.out.println("privateKey=" + keyPair.privateKey.hexEncodedString()+",length=" + keyPair.privateKey.value.length);
        System.out.println("publicKey=" + keyPair.publicKey.hexEncodedString()+",length=" + keyPair.publicKey.value.length);
        // Count = UInt32 (4 bytes) + 128-bit key (16 bytes) = 20 bytes
        assertEquals(keyPair.privateKey.value.length, 20);
        assertEquals(keyPair.publicKey.value.length, 20);
    }

    @Test
    public void testKeyExchange() {
        final KeyExchange keyExchange = new DiffieHellmanMerkle(DiffieHellmanParameters.random128);

        final KeyExchangeKeyPair aliceKeyPair = keyExchange.keyPair();
        System.out.println("alice private key bytes: " + aliceKeyPair.privateKey.value.length);
        System.out.println("alice private key = " + aliceKeyPair.privateKey.hexEncodedString());
        System.out.println("alice public key bytes: " + aliceKeyPair.publicKey.value.length);
        System.out.println("alice public key = " + aliceKeyPair.publicKey.hexEncodedString());

        final KeyExchangeKeyPair bobKeyPair = keyExchange.keyPair();
        System.out.println("alice private key bytes: " + bobKeyPair.privateKey.value.length);
        System.out.println("bob private key = " + bobKeyPair.privateKey.hexEncodedString());
        System.out.println("bob public key bytes: " + bobKeyPair.publicKey.value.length);
        System.out.println("bob public key = " + bobKeyPair.publicKey.hexEncodedString());

        final KeyExchangeSharedKey aliceSharedKey = keyExchange.sharedKey(aliceKeyPair.privateKey, bobKeyPair.publicKey);
        assertNotNull(aliceSharedKey);
        System.out.println("alice shared key bytes: " + aliceSharedKey.value.length);
        System.out.println("alice shared key = " + aliceSharedKey.hexEncodedString());
        final KeyExchangeSharedKey bobSharedKey = keyExchange.sharedKey(bobKeyPair.privateKey, aliceKeyPair.publicKey);
        assertNotNull(bobSharedKey);
        System.out.println("bob shared key bytes: " + bobSharedKey.value.length);
        System.out.println("bob shared key = " + bobSharedKey.hexEncodedString());

        assertEquals(aliceSharedKey, bobSharedKey);
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final KeyExchange keyExchange = new DiffieHellmanMerkle(DiffieHellmanParameters.random128);
        final KeyExchangePrivateKey alicePrivateKey = new KeyExchangePrivateKey("08000000D467F3ABF521BABDF238F07602BC6F28");
        final KeyExchangePublicKey alicePublicKey = new KeyExchangePublicKey("080000003BD578EC0E412261EE10F80E0C055896");
        final KeyExchangePrivateKey bobPrivateKey = new KeyExchangePrivateKey("0800000055981B228A3030AFCB2E6CF5B0A7822F");
        final KeyExchangePublicKey bobPublicKey = new KeyExchangePublicKey("08000000D644A2045C53D6CCF6B5180756C85E16");
        final KeyExchangeSharedKey aliceSharedKey = keyExchange.sharedKey(alicePrivateKey, bobPublicKey);
        final KeyExchangeSharedKey bobSharedKey = keyExchange.sharedKey(bobPrivateKey, alicePublicKey);
        assertEquals(aliceSharedKey, bobSharedKey);
        final PrintWriter out = TestUtil.androidPrintWriter("keyExchange.csv");
        out.println("key,value");
        out.println("alicePrivate," + alicePrivateKey.hexEncodedString());
        out.println("alicePublic," + alicePublicKey.hexEncodedString());
        out.println("bobPrivate," + bobPrivateKey.hexEncodedString());
        out.println("bobPublic," + bobPublicKey.hexEncodedString());
        out.println("aliceShared," + aliceSharedKey.hexEncodedString());
        out.println("bobShared," + bobSharedKey.hexEncodedString());
        out.flush();
        out.close();
        // Pending iOS implementation
        TestUtil.assertEqualsCrossPlatform("keyExchange.csv");
    }
}
