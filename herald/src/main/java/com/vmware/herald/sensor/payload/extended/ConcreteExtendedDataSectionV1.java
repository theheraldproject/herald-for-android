package com.vmware.herald.sensor.payload.extended;

import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.UInt8;

public class ConcreteExtendedDataSectionV1 {
    public final UInt8 code;
    public final UInt8 length;
    public final Data data;

    public ConcreteExtendedDataSectionV1(UInt8 code, UInt8 length, Data data) {
        this.code = code;
        this.length = length;
        this.data = data;
    }
}