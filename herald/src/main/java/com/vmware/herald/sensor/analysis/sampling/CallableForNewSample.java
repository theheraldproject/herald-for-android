//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

public interface CallableForNewSample<T> {

    void newSample(SampledID sampled, Sample<T> item);
}
