package io.heraldprox.herald.sensor.payload.extended;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Float16;
import io.heraldprox.herald.sensor.datatype.Int16;
import io.heraldprox.herald.sensor.datatype.Int32;
import io.heraldprox.herald.sensor.datatype.Int64;
import io.heraldprox.herald.sensor.datatype.Int8;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt32;
import io.heraldprox.herald.sensor.datatype.UInt64;
import io.heraldprox.herald.sensor.datatype.UInt8;

import java.util.ArrayList;
import java.util.List;

public class ConcreteExtendedDataV1 implements ExtendedData {
    @NonNull
    private final PayloadData payloadData;

    public ConcreteExtendedDataV1() {
        payloadData = new PayloadData();
    }

    public ConcreteExtendedDataV1(@NonNull final PayloadData unparsedData) {
        payloadData = unparsedData;
    }

    @Override
    public boolean hasData() {
        return 0 != payloadData.value.length;
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final UInt8 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(1));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final UInt16 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(2));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final UInt32 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(4));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final UInt64 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(8));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final Int8 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(1));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final Int16 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(2));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final Int32 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(4));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final Int64 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(8));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final Float16 value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(2));
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final String value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        // Append String adds length to payload
        payloadData.append(value);
    }

    @Override
    public void addSection(@NonNull final UInt8 code, @NonNull final Data value) {
        //noinspection ConstantConditions
        if (null == code || null == value) {
            return;
        }
        payloadData.append(code);
        payloadData.append(new UInt8(value.value.length));
        payloadData.append(value);
    }

    @NonNull
    @Override
    public PayloadData payload() {
        return payloadData;
    }

    @NonNull
    public List<ConcreteExtendedDataSectionV1> getSections() {
        final List<ConcreteExtendedDataSectionV1> sections = new ArrayList<>();

        int pos = 0;

        while (pos < payloadData.value.length) {
            if (payloadData.value.length - 2 <= pos) { // at least 3 in length
                pos = payloadData.value.length;
                continue;
            }
            // read code
            final UInt8 code = payloadData.uint8(pos);
            pos = pos + 1;
            // read length
            UInt8 length = payloadData.uint8(pos);
            pos = pos + 1;
            // sanity check length
            if (length != null && pos + length.value > payloadData.value.length) {
                length = new UInt8(payloadData.value.length - pos);
            }
            // extract data
            if (length != null) {
                final Data data = payloadData.subdata(pos, length.value);

                if (code != null && data != null) {
                    sections.add(new ConcreteExtendedDataSectionV1(code, length, data));
                }

                // repeat
                pos = pos + length.value;
            }
        }

        return sections;
    }
}