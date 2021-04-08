//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.pipeline;

import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.datatype.DoubleValue;

public interface DataConsumer<T extends DoubleValue> {

    Class<T> inputType();

    void newSample(SampledID sampled, Sample<T> item);
}