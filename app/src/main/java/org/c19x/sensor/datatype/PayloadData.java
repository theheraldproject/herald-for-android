package org.c19x.sensor.datatype;

import android.util.Base64;

/// Encrypted payload data received from target. This is likely to be an encrypted datagram of the target's actual permanent identifier.
public class PayloadData extends Data {

    public PayloadData(byte[] value) {
        super(value);
    }

//    @Override
//    public String base64EncodedString() {
//        return Base64.encodeToString(value, 3, value.length - 3, Base64.DEFAULT | Base64.NO_WRAP);
//    }

    public String shortName() {
        return Base64.encodeToString(value, 3, value.length - 3, Base64.DEFAULT | Base64.NO_WRAP).substring(0, 6);
    }
}
