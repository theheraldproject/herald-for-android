//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.pipeline;

import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampleList;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcreteDataConsumer<T extends DoubleValue> implements DataConsumer<T> {
    private final Class<T> inputType;
    private final int sampleListSize;
    private final List<SampledID> sampledIDs = new ArrayList<>();
    private final Map<SampledID, SampleList<T>> sampleLists = new ConcurrentHashMap<>();
    private SampledID latestSampled = null;

    protected ConcreteDataConsumer(final Class<T> inputType, final int sampleListSize) {
        this.inputType = inputType;
        this.sampleListSize = sampleListSize;
    }

    @Override
    public Class<T> inputType() {
        return inputType;
    }

    @Override
    public void newSample(SampledID sampled, Sample<T> item) {
        final SampleList<T> sampleList = sampleList(sampled);
        sampleList.push(item);
        latestSampled = sampled;
        process(sampled, sampleList);
    }

    public void clear() {
        sampleLists.clear();
        sampledIDs.clear();
    }

    public synchronized SampleList<T> sampleList(final SampledID sampled) {
        SampleList<T> sampleList = sampleLists.get(sampled);
        if (sampleList == null) {
            sampleList = new SampleList<>(sampleListSize);
            sampleLists.put(sampled, sampleList);
            sampledIDs.add(sampled);
            Collections.sort(sampledIDs);
        }
        return sampleList;
    }

    public SampledID latestSampled() {
        return latestSampled;
    }

    /// Process new sample
    protected void process(SampledID sampled, SampleList<T> sampleList) {}
}