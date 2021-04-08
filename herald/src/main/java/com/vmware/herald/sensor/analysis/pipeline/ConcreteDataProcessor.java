//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.pipeline;

import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampleList;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcreteDataProcessor<T extends DoubleValue, U extends DoubleValue> extends ConcreteDataConsumer<T> implements DataProcessor<T,U> {
    private final List<DataConsumer<U>> dataConsumers = new ArrayList<>(1);

    public ConcreteDataProcessor(final Class<T> inputType, final int sampleListSize) {
        super(inputType, sampleListSize);
    }

    @Override
    public void add(DataConsumer<U> dataConsumer) {
        dataConsumers.add(dataConsumer);
    }

    @Override
    public void publish(SampledID sampled, Sample<U> sample) {
        for (final DataConsumer<U> dataConsumer : dataConsumers) {
            dataConsumer.newSample(sampled, sample);
        }
    }

    /// Process new sample, use publish() to release new result to consumers
    @Override
    protected void process(SampledID sampled, SampleList<T> sampleList) {}
}