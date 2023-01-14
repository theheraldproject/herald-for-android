//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import org.junit.Test;

import java.io.PrintWriter;

import io.heraldprox.herald.sensor.TestUtil;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Tuple;
import io.heraldprox.herald.sensor.datatype.UInt8;

import static org.junit.Assert.assertEquals;

public class TransportLayerSecurityTests {

    @Test
    public void testTransportLayerSecuritySession() {
        final DiffieHellmanParameters diffieHellmanParameters = DiffieHellmanParameters.random128;
        final TransportLayerSecurity alice = new ConcreteTransportLayerSecurity(diffieHellmanParameters);
        final TransportLayerSecurity bob = new ConcreteTransportLayerSecurity(diffieHellmanParameters);
        for (int i=0; i<=10; i++) {
            System.out.println("testTransportLayerSecurity (count=" + i + ")");
            final Data data = new Data((byte) i, i);
            // Alice reads public key from Bob
            final KeyExchangePublicKey bobPublicKey = bob.readPublicKey();
            System.out.println("testTransportLayerSecurity (count=" + i + ",bobPublicKeyCount=" + bobPublicKey.value.length + ")");
            // Alice encrypted data for Bob
            final Data aliceEncryptedData = alice.writeEncryptedData(bobPublicKey, data);
            System.out.println("testTransportLayerSecurity (count=" + i + ",aliceEncryptedDataCount=" + aliceEncryptedData.value.length + ")");
            // Bob decrypts data from Alice
            final Tuple<TransportLayerSecuritySessionID, Data> bobReceivedData = bob.receiveEncryptedData(aliceEncryptedData);
            final TransportLayerSecuritySessionID bobSessionId = bobReceivedData.a;
            final Data bobDecryptedData = bobReceivedData.b;
            assertEquals(data, bobDecryptedData);
            // Alice reads encrypted data from Bob
            final Data bobEncryptedData = bob.readEncryptedData(bobSessionId, data);
            System.out.println("testTransportLayerSecurity (count=" + i + ",bobEncryptedDataCount=" + bobEncryptedData.value.length + ")");
            // Alice decrypts data from Bob
            final Tuple<TransportLayerSecuritySessionID, Data> aliceReceivedData = alice.receiveEncryptedData(bobEncryptedData);
            final Data aliceDecryptedData = aliceReceivedData.b;
            assertEquals(data, aliceDecryptedData);
        }
    }

    @Test
    public void testCrossPlatform() throws Exception {
        // Need to make random constant for cross platform test
        final DiffieHellmanParameters diffieHellmanParameters = DiffieHellmanParameters.random128;
        final PseudoRandomFunction random = new TestRandomFunction(new UInt8(0));
        final TransportLayerSecurity alice = new ConcreteTransportLayerSecurity(diffieHellmanParameters, random);
        final TransportLayerSecurity bob = new ConcreteTransportLayerSecurity(diffieHellmanParameters, random);
        final PrintWriter out = TestUtil.androidPrintWriter("transportLayerSecurity.csv");
        out.println("key,bobPublicKey,aliceEncryptedData,bobSessionId,bobDecryptedData,bobEncryptedData,aliceSessionId,aliceDecryptedData");
        for (int i=0; i<=10; i++) {
            final Data data = new Data((byte) i, i);
            // Alice reads public key from Bob
            final KeyExchangePublicKey bobPublicKey = bob.readPublicKey();
            // Alice encrypted data for Bob
            final Data aliceEncryptedData = alice.writeEncryptedData(bobPublicKey, data);
            // Bob decrypts data from Alice
            final Tuple<TransportLayerSecuritySessionID, Data> bobReceivedData = bob.receiveEncryptedData(aliceEncryptedData);
            final TransportLayerSecuritySessionID bobSessionId = bobReceivedData.a;
            final Data bobDecryptedData = bobReceivedData.b;
            assertEquals(data, bobDecryptedData);
            // Alice reads encrypted data from Bob
            final Data bobEncryptedData = bob.readEncryptedData(bobSessionId, data);
            // Alice decrypts data from Bob
            final Tuple<TransportLayerSecuritySessionID, Data> aliceReceivedData = alice.receiveEncryptedData(bobEncryptedData);
            final TransportLayerSecuritySessionID aliceSessionId = aliceReceivedData.a;
            final Data aliceDecryptedData = aliceReceivedData.b;
            assertEquals(data, aliceDecryptedData);
            out.println(i+","+bobPublicKey.hexEncodedString()+","+aliceEncryptedData.hexEncodedString()+","+
                    bobSessionId.toString()+","+bobDecryptedData.hexEncodedString()+","+
                    bobEncryptedData.hexEncodedString()+","+aliceSessionId.toString()+","
                    +aliceDecryptedData.hexEncodedString());
        }
        out.flush();
        out.close();
        // Pending iOS implementation
        TestUtil.assertEqualsCrossPlatform("transportLayerSecurity.csv");
    }
}
