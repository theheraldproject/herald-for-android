//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface AnalysisDelegate<T extends DoubleValue> extends CallableForNewSample<T> {

    Class<T> inputType();

    void reset();

    SampleList<T> samples();
}