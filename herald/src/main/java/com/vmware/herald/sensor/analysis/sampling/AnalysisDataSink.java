//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

public interface AnalysisDataSink<T extends DoubleValue> {

    void newSample(SampledID sampled, Sample<T> item);
}