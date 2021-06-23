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

public interface ExtendedData {
    boolean hasData();
    void addSection(@NonNull final UInt8 code, @NonNull final UInt8 value);
    void addSection(@NonNull final UInt8 code, @NonNull final UInt16 value);
    void addSection(@NonNull final UInt8 code, @NonNull final UInt32 value);
    void addSection(@NonNull final UInt8 code, @NonNull final UInt64 value);
    void addSection(@NonNull final UInt8 code, @NonNull final Int8 value);
    void addSection(@NonNull final UInt8 code, @NonNull final Int16 value);
    void addSection(@NonNull final UInt8 code, @NonNull final Int32 value);
    void addSection(@NonNull final UInt8 code, @NonNull final Int64 value);
    void addSection(@NonNull final UInt8 code, @NonNull final Float16 value);
    void addSection(@NonNull final UInt8 code, @NonNull final String value);
    void addSection(@NonNull final UInt8 code, @NonNull final Data value);
    @NonNull
    PayloadData payload();
}