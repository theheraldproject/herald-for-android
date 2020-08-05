package org.c19x.sensor.datatype;

import java.util.Date;

/// Payload timestamp, should normally be Date, but it may change to UInt64 in the future to use server synchronised relative timestamp.
public class PayloadTimestamp {
    public final Date value;

    public PayloadTimestamp(Date value) {
        this.value = value;
    }
}
