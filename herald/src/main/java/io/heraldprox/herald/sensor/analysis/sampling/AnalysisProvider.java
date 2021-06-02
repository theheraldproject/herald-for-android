//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface AnalysisProvider<T extends DoubleValue, U extends DoubleValue> {

    @NonNull
    Class<T> inputType();

    @NonNull
    Class<U> outputType();

    boolean analyse(@NonNull final Date timeNow, @NonNull final SampledID sampled, @NonNull final SampleList<T> input, @NonNull final SampleList<U> output, @NonNull final CallableForNewSample<U> callable);
}
