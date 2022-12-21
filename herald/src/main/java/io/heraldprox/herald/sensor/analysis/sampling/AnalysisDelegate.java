//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface AnalysisDelegate<T extends DoubleValue> extends CallableForNewSample<T> {

    @NonNull
    Class<T> inputType();

    void reset();

    void removeSamplesFor(SampledID sampled);

    @NonNull
    SampleList<T> samples();
}