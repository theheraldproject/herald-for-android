//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.pipeline;

import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Generic DataConsumer for directing samples to registered data consumers based on sample data type
public class PipelineManager {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "PipelineManager");
    private final Map<Class<? extends DoubleValue>, List<DataConsumer<? extends DoubleValue>>> dataConsumers = new ConcurrentHashMap<>();

    /// Add data consumer for receiving sample data
    public synchronized void add(final DataConsumer<? extends DoubleValue> dataConsumer) {
        final Class<? extends DoubleValue> inputType = dataConsumer.inputType();
        List<DataConsumer<? extends DoubleValue>> list = dataConsumers.get(inputType);
        if (list == null) {
            list = new ArrayList<>();
            dataConsumers.put(inputType, list);
        }
        list.add(dataConsumer);
    }

    /// Direct sample to data consumers based on sample data type
    public <T extends DoubleValue> void newSample(SampledID sampled, Sample<T> item) {
        final Class<? extends DoubleValue> inputType = item.value().getClass();
        final List<DataConsumer<? extends DoubleValue>> list = dataConsumers.get(inputType);
        if (list == null) {
            logger.debug("newSample, no consumer (inputType={})", inputType);
            return;
        }
        logger.debug("newSample (inputType={},consumers={})", inputType, list.size());
        for (final DataConsumer<? extends DoubleValue> dataConsumer : list) {
            ((DataConsumer<T>) dataConsumer).newSample(sampled, item);
        }
    }
}