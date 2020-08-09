package org.c19x.sensor.datatype;

/// Encrypted payload data received from target. This is likely to be an encrypted datagram of the target's actual permanent identifier.
public class PayloadData extends Data {

    public PayloadData(byte[] value) {
        super(value);
    }
}
