package io.heraldprox.herald.sensor.data;

import io.heraldprox.herald.sensor.data.security.DiffieHellmanMerkle;
import io.heraldprox.herald.sensor.data.security.DiffieHellmanParameters;
import io.heraldprox.herald.sensor.data.security.KeyExchange;
import io.heraldprox.herald.sensor.data.security.KeyExchangeKeyPair;
import io.heraldprox.herald.sensor.data.security.KeyExchangeSharedKey;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UIntBig;

import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SecurityTests {

    @Test
    public void testKeyExchange() {
        final KeyExchange keyExchange = new DiffieHellmanMerkle(DiffieHellmanParameters.modpGroup1);

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



    // RFC-3526
    //    2048-bit MODP Group
    //    This group is assigned id 14.
    //    This prime is: 2^2048 - 2^1984 - 1 + 2^64 * { [2^1918 pi] + 124476 }
    //    Its hexadecimal value is:
    //    FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1
    //    29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD
    //    EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245
    //    E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED
    //    EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D
    //    C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F
    //    83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D
    //    670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B
    //    E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9
    //    DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510
    //    15728E5A 8AACAA68 FFFFFFFF FFFFFFFF
    //    The generator is: 2.
    @Test
    public void test_dh() {
        final String modpGroup14Key = (
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
                "15728E5A 8AACAA68 FFFFFFFF FFFFFFFF")
                .replaceAll(" ", "");
        final UIntBig p = new UIntBig(modpGroup14Key);
        assertEquals(new BigInteger(modpGroup14Key, 16).toString(16).toUpperCase(), p.hexEncodedString());
        final UIntBig g = new UIntBig(2);
        assertEquals(2, g.uint64());
        System.out.println("p bits: "+p.bitLength());
        assertEquals(new BigInteger(modpGroup14Key, 16).bitLength(), p.bitLength());
        System.out.println("g bits: "+g.bitLength());
        assertEquals(BigInteger.valueOf(2).bitLength(), g.bitLength());
        System.out.println("p = "+p.hexEncodedString());
        assertEquals(modpGroup14Key, p.hexEncodedString());

        final SecureRandom secureRandom = new SecureRandom();
        final String alicePrivateKeyString = "23B07A05486A3D9932638FA4FE8D1D226FB6101593FA647580ADD324637F8EEF7B98AA4547A689B68BFA1BE1099DBBCE244134C59BBEEA0256C236866FDF179298C1B5A57404A32F6E73BAA9352DC3E7E3E66EE9D6724A1D381D02474894C9031DEB48B3E70A6899B7C0DD568C4845867F27A8AD4D2A8EE578DE531BEE9574B5BBBEF8FECEC7AAB6D2C5D225743AAC4181B185CB5DB89A85A6E5C5708DEACE88228F55C6E841DC203B1E23BF4FDAB202B355DF5BB13AAFF9FFC7C1CE1CA699236818FC92C7157EE13069CD54401B7DDDA742BBD6D05EBD72E25DE07683CB05172FC8526A3F62DBCF54FAF41C4F8674C2F9F13450D5AEC4D752DA444F57ABF857";
        //final UIntBig alicePrivateKey = new UIntBig(p.bitLength()-2, secureRandom);
        final UIntBig alicePrivateKey = new UIntBig(alicePrivateKeyString);
        System.out.println("alice private key bits: "+alicePrivateKey.bitLength());
        System.out.println("alice private key = "+alicePrivateKey.hexEncodedString());
        final String alicePublicKeyString = "E03560806F04BBD8D910D283581FCA1F47858CA4A4CEE93C410E55C13E25275239626D20BED40EFFF839B9FA8A3D7B6BD034229AD1096CFB45D2761F771AD68AA14424C0CB7E67BE87D94C0AE2C70C3F6A53B56F9711DDEAAC0B6B8B0F117105EDD56E77DAA6328B8B49E20DB80DE87691CDB555A6B0536CAD6B4A4D6588EFB0619DFB0D6EE2AED2F604F9FEBAF976BEEFC327FC567C1ACFBC66503F02DA13BEA9AA81B8E5C726D2070DC4A25423BDDD75DB5A086ED39C9EF694C8E4BCCD906D005C69245D3E3C9F201604276E6687BD8096D97B2E0C2FE57328846B13D464D8624D33503D12E3A92E0802FFF29DFBBB1AA69DB29D21E25F1FBE6AFBA6F9F17E";
        final UIntBig alicePublicKey = g.modPow(alicePrivateKey, p);
        System.out.println("alice public key bits: "+alicePublicKey.bitLength());
        System.out.println("alice public key = "+alicePublicKey.hexEncodedString());
        assertEquals(alicePublicKeyString, alicePublicKey.hexEncodedString());

        final String bobPrivateKeyString = "376FE02C5466E9461B17F4C1040D95D817D51C3AAA150EC03933E84DD5A2EA3ECFE3BC658494754EDBD27C6AAECD6502C1C28EE29E89B47EA30AE25F69C31FB1FDD361CFAC829A6C4B14F725E0B4A7997DA9467E33DAB3183EF1450BEC14AB00B177697DDAB043FAE0D710159F166F04B04B6F559E447E29E13DA8AA428D876AD385145C1B1B7D7C56EF2E93D54ABA5A4C9BF2C87F9A4EA93CA570CDD8D71837C4E122E4F9DCE841B61D5B77DC3A12A4B5935F54178FC4E8BEF7557B1D9D85159E43D38E6737594E7BC81148CB6F39D3B16BF83B898412687D598725618161849621A95CA8E69D2F243FEF8ED4570DAA635ADC27DBF40B966BF4FC23BE9516D3";
        //final UIntBig bobPrivateKey = new UIntBig(p.bitLength()-2, secureRandom);
        final UIntBig bobPrivateKey = new UIntBig(bobPrivateKeyString);
        System.out.println("bob private key bits: "+bobPrivateKey.bitLength());
        System.out.println("bob private key = "+bobPrivateKey.hexEncodedString());
        final String bobPublicKeyString = "4BE53C7F7C132CEAABD72ED6D4F6EB6B19EA15BE4EB910DACEFF235629AC21915611281629FB16D64203E43A731FDD6D41D8BF52DABC559890EA5A189867C1DF3817E519A110767E3904F7100A215E2A9B0E5E506285893F938DD458D277A6D85CA56C808E10A65E6B23D06DB540F5FBF93A2AB29CB83C6D77775491F45787E5C9FB36B93829F469865B92AE6BF23342716E2CFCF0A8C650A91D68AF255DB3FFC584E82E3E7D9FCF424714349EDD929077FC595EEB57A680F90DB610070F3478704F1F052C64821691CA1F2D14E8D7D959AEFA4D410910682279723B6878694941C6F0316574EEF2EA0C185D8D88DC052706E275117A528E3C7A79D47CB712CC";
        final UIntBig bobPublicKey = g.modPow(bobPrivateKey, p);
        System.out.println("bob public key bits: "+bobPublicKey.bitLength());
        System.out.println("bob public key = "+bobPublicKey.hexEncodedString());
        assertEquals(bobPublicKeyString, bobPublicKey.hexEncodedString());

        final String sharedKeyString = "0921B0F31050DEF971EAD302D1C6FC374DA6DD8041ADEE5D4C4CBBB9C676C43FDFD1DAD660CC1B58B87BF4664F270B18DF8996C01F0E13CE86B9C4EB52DCA7905CA4DA20D054DE8C50664626AAF9A47EF84B232BABFE33E27C3C7FF2FC11E3DA5DDF650C3BF0563C0EADFF97BD4EDB5B49C0C3887D9D002D46491C8FEDE61C0A856F808A3A3A0FEDA78E28FF02EF48716CBCADF87281F350DAF35A7D3A679BB1ABA436AF06983ABCC822E19963330980D26658CB99190C0261E7398F526F543B88E24058D7572BA2D3831AE256AFBB188464D6E01C1928047AB6D2F75C6F0F490D51639AB1CE4E554E811F1EA5CE22BD1D48A13FD531211DAFC20E1F0A7ED886";
        final UIntBig aliceSharedKey = bobPublicKey.modPow(alicePrivateKey, p);
        System.out.println("alice shared key bits: "+aliceSharedKey.bitLength());
        System.out.println("alice shared key = "+aliceSharedKey.hexEncodedString());
        final UIntBig bobSharedKey = alicePublicKey.modPow(bobPrivateKey, p);
        System.out.println("bob shared key bits: "+bobSharedKey.bitLength());
        System.out.println("bob shared key = "+bobSharedKey.hexEncodedString());

        assertEquals(aliceSharedKey, bobSharedKey);
        assertEquals(sharedKeyString, aliceSharedKey.hexEncodedString());
        assertEquals(sharedKeyString, bobSharedKey.hexEncodedString());
    }


    @Test
    public void test_aes() throws Exception {
        // Manual cross-platform test to establish compatible cipher, key, hash, and exchange format

        // Generate secret key
        final String key = "examplekey";
        final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        final byte[] keyData = sha256.digest(key.getBytes(StandardCharsets.UTF_8));
        System.out.println("key = " + new Data(keyData).hexEncodedString());
        final SecretKey secretKey = new SecretKeySpec(keyData, "AES");

        // IV required for CBC
        final byte[] ivData = new byte[16];
        final IvParameterSpec iv = new IvParameterSpec(ivData);

        // Encrypt data
        final String data = "hello";
        final Cipher cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        final byte[] cipherText = cipherEncrypt.doFinal(data.getBytes(StandardCharsets.UTF_8));
        System.out.println("encrypted = " + new Data(cipherText).hexEncodedString());

        // Decrypt data
        final Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, iv);
        final byte[] clearText = cipherDecrypt.doFinal(cipherText);
        final String dataOut = new String(clearText, StandardCharsets.UTF_8);
        System.out.println("decrypted = " + dataOut);
        assertEquals(data, dataOut);
    }
}
