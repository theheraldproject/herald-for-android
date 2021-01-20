package com.vmware.herald.sensor.payload.extended;

import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.Float16;
import com.vmware.herald.sensor.datatype.Int16;
import com.vmware.herald.sensor.datatype.Int32;
import com.vmware.herald.sensor.datatype.Int64;
import com.vmware.herald.sensor.datatype.Int8;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.UInt16;
import com.vmware.herald.sensor.datatype.UInt32;
import com.vmware.herald.sensor.datatype.UInt64;
import com.vmware.herald.sensor.datatype.UInt8;

public interface ExtendedData {
    boolean hasData();
    void addSection(UInt8 code, UInt8 value);
    void addSection(UInt8 code, UInt16 value);
    void addSection(UInt8 code, UInt32 value);
    void addSection(UInt8 code, UInt64 value);
    void addSection(UInt8 code, Int8 value);
    void addSection(UInt8 code, Int16 value);
    void addSection(UInt8 code, Int32 value);
    void addSection(UInt8 code, Int64 value);
    void addSection(UInt8 code, Float16 value);
    void addSection(UInt8 code, String value);
    void addSection(UInt8 code, Data value);
    PayloadData payload();
}