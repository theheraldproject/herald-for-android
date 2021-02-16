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

import java.util.ArrayList;
import java.util.List;

public class ConcreteExtendedDataV1 implements ExtendedData {
    private final PayloadData payloadData;

    public ConcreteExtendedDataV1() {
        payloadData = new PayloadData();
    }

    public ConcreteExtendedDataV1(PayloadData unparsedData) {
        payloadData = unparsedData;
    }

    @Override
    public boolean hasData() {
        return 0 != payloadData.value.length;
    }

    @Override
    public void addSection(UInt8 code, UInt8 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(1));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, UInt16 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(2));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, UInt32 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(4));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, UInt64 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(8));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, Int8 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(1));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, Int16 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(2));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, Int32 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(4));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, Int64 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(8));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, Float16 value) {
        payloadData.append(code);
        payloadData.append(new UInt8(2));
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, String value) {
        payloadData.append(code);
        // Append String adds length to payload
        payloadData.append(value);
    }

    @Override
    public void addSection(UInt8 code, Data value) {
        payloadData.append(code);
        payloadData.append(new UInt8(value.value.length));
        payloadData.append(value);
    }

    @Override
    public PayloadData payload() {
        return payloadData;
    }

    public List<ConcreteExtendedDataSectionV1> getSections() {
        final List<ConcreteExtendedDataSectionV1> sections = new ArrayList<>();

        int pos = 0;

        while (pos < payloadData.value.length) {
            if (payloadData.value.length - 2 <= pos) { // at least 3 in length
                pos = payloadData.value.length;
                continue;
            }
            // read code
            UInt8 code = payloadData.uint8(pos);
            pos = pos + 1;
            // read length
            UInt8 length = payloadData.uint8(pos);
            pos = pos + 1;
            // sanity check length
            if (pos + length.value > payloadData.value.length) {
                length = new UInt8(payloadData.value.length - pos);
            }
            // extract data
            Data data = payloadData.subdata(pos, length.value);

            sections.add(new ConcreteExtendedDataSectionV1(code, length, data));

            // repeat
            pos = pos + length.value;
        }

        return sections;
    }
}