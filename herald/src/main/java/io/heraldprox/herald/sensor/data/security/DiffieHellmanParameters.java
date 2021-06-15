//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.UIntBig;

/**
 * Common Diffie-Hellman parameters
 */
public class DiffieHellmanParameters {
    @NonNull
    public final UIntBig p;
    @NonNull
    public final UIntBig g;

    /**
     * OpenSSL generated safe prime : 128-bits
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters random128 = getInstance(
            "C8132E2C84B73BE9D9AD805E228E5F87", "2");

    /**
     * OpenSSL generated safe prime : 256-bits
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters random256 = getInstance(
            "D6E86C6CA81EFFA45AF8921B1D2C1E5F" +
            "1B644A7DBCDC528D3B31E46EE367F877", "2");

    /**
     * OpenSSL generated safe prime : 512-bits
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters random512 = getInstance(
            "F8D1E3F7C41D8E20525045E9CFFD2886" +
            "C10E795649C57A59E30D0A764B14AA69" +
            "B9CC2651419C71384D33BBD47705A6FB" +
            "60F599C548C442E55EC7F457AA355C17", "2");

    /**
     * RFC 2409 MODP Group 1 : 768-bits : First Oakley Group : Generator = 2
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters modpGroup1 = getInstance(
            "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
            "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
            "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
            "E485B576 625E7EC6 F44C42E9 A63A3620 FFFFFFFF FFFFFFFF",
     "2");

    /**
     * RFC 2409 MODP Group 2 : 1024-bits : Second Oakley Group : Generator = 2
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters modpGroup2 = getInstance(
            "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
            "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
            "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
            "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED" +
            "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE65381" +
            "FFFFFFFF FFFFFFFF",
    "2");

    /**
     * RFC3526 MODP Group 5 : 1536-bits : Generator = 2
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters modpGroup5 = getInstance(
            "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
            "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
            "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
            "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED" +
            "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D" +
            "C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F" +
            "83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D" +
            "670C354E 4ABC9804 F1746C08 CA237327 FFFFFFFF FFFFFFFF",
    "2");

    /**
     * RFC3526 MODP Group 14 : 2048-bits : Generator = 2.
     * <br>
     * Satisfies NCSC Foundation Profile for TLS standard
     */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public final static DiffieHellmanParameters modpGroup14 = getInstance(
            "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
            "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
            "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
            "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED" +
            "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D" +
            "C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F" +
            "83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D" +
            "670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B" +
            "E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9" +
            "DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510" +
            "15728E5A 8AACAA68 FFFFFFFF FFFFFFFF",
    "2");

    /**
     * Diffie-Hellman key exchange parameters
     * @param p Safe prime.
     * @param g Generator.
     */
    public DiffieHellmanParameters(@NonNull UIntBig p, @NonNull UIntBig g) {
        this.p = p;
        this.g = g;
    }

    /**
     * Get instance of Diffie-Hellman key exchange parameters from hex encoded strings for P and G.
     * @param pHexEncodedString Hex encoded string for parameter P.
     * @param gHexEncodedString Hex encoded string for parameter G.
     * @return
     */
    @Nullable
    public static DiffieHellmanParameters getInstance(@NonNull String pHexEncodedString, @NonNull String gHexEncodedString) {
        try {
            final UIntBig pValue = new UIntBig(pHexEncodedString.replaceAll(" ", ""));
            final UIntBig gValue = new UIntBig(gHexEncodedString.replaceAll(" ", ""));
            return new DiffieHellmanParameters(pValue, gValue);
        } catch (Throwable e) {
            return null;
        }
    }
}
