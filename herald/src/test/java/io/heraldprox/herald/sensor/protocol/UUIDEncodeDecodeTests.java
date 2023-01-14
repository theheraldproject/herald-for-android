package io.heraldprox.herald.sensor.protocol;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UInt64;
import io.heraldprox.herald.sensor.datatype.Int64;

public class UUIDEncodeDecodeTests {
    @Test
    public void testUUIDEncodeDecode() {
        String knownGoodUUID = "0c079836-f7aa-4f47-a855-17b3798423c4";
        UUID fromString = UUID.fromString(knownGoodUUID);
        String encodedResult = fromString.toString();
        assertEquals(knownGoodUUID,encodedResult);

        // Best method
//        Data encodedUUID = new Data();
//        encodedUUID.append(new Int64(fromString.getLeastSignificantBits()));
//        encodedUUID.append(new Int64(fromString.getMostSignificantBits()));
//        assertEquals(16,encodedUUID.length());
//        System.out.println("Backward Hex encoded String: " + encodedUUID.hexEncodedString());
//        encodedUUID = encodedUUID.reversed();
//        assertEquals(16,encodedUUID.length());

        // alternative method (more computationally intensive)
//        Data encodedUUID = Data.fromHexEncodedString(fromString.toString().replaceAll("-","").toUpperCase());
//        assertEquals(16,encodedUUID.length());

        // method 3 - use new Data method
        PayloadData encodedUUID = new PayloadData();
        encodedUUID.append(fromString);
        assertEquals(16,encodedUUID.length());

        String asHex = encodedUUID.hexEncodedString();
        System.out.println("Hex encoded String: " + asHex);
        String knownCapsNoDashes = knownGoodUUID.replaceAll("-","").toUpperCase();
        assertEquals(knownCapsNoDashes,asHex);

        // method 1 & 2
//        Data encodedRev = encodedUUID.reversed();
//        assertEquals(16,encodedRev.length());
//        UUID decodedUUID = new UUID(encodedRev.int64(8).value,encodedRev.int64(0).value);
        // method 3
        UUID decodedUUID = encodedUUID.uuid(0);
        assertEquals(fromString,decodedUUID);
    }
}
