//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.pipeline;

import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.List;

public abstract class ConcreteDataProducer<T extends DoubleValue> {
    private final List<DataConsumer<T>> dataConsumers = new ArrayList<>(1);

    public void add(DataConsumer<T> dataConsumer) {
        dataConsumers.add(dataConsumer);
    }

    public void remove(DataConsumer<T> dataConsumer) {
        dataConsumers.remove(dataConsumer);
    }

    protected void publish(final SampledID sampled, final Sample<T> sample) {
        for (final DataConsumer<T> dataConsumer : dataConsumers) {
            dataConsumer.newSample(sampled, sample);
        }
    }
}