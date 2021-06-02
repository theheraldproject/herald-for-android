package io.heraldprox.herald.sensor.payload.extended;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UInt8;

public class ConcreteExtendedDataSectionV1 {
    @NonNull
    public final UInt8 code;
    @NonNull
    public final UInt8 length;
    @NonNull
    public final Data data;

    public ConcreteExtendedDataSectionV1(@NonNull final UInt8 code, @NonNull final UInt8 length, @NonNull final Data data) {
        this.code = code;
        this.length = length;
        this.data = data;
    }
}