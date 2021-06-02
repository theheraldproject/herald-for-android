//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisDelegateManager {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Analysis.AnalysisDelegateManager");
    private final Map<Class<? extends DoubleValue>, List<AnalysisDelegate<? extends DoubleValue>>> lists = new ConcurrentHashMap<>();

    public AnalysisDelegateManager(@NonNull final AnalysisDelegate<? extends DoubleValue> ... delegates) {
        for (final AnalysisDelegate<? extends DoubleValue> delegate : delegates) {
            add(delegate);
        }
    }

    @NonNull
    public Set<Class<? extends DoubleValue>> inputTypes() {
        return lists.keySet();
    }

    public void add(@NonNull final AnalysisDelegate<? extends DoubleValue> delegate) {
        final Class<? extends DoubleValue> inputType = delegate.inputType();
        final List<AnalysisDelegate<? extends DoubleValue>> list = list(inputType);
        list.add(delegate);
    }

    @NonNull
    private synchronized List<AnalysisDelegate<? extends DoubleValue>> list(@NonNull final Class<? extends DoubleValue> inputType) {
        List<AnalysisDelegate<? extends DoubleValue>> list = lists.get(inputType);
        if (null == list) {
            list = new ArrayList<>(1);
            lists.put(inputType, list);
        }
        return list;
    }

    public <T extends DoubleValue> void newSample(@NonNull final SampledID sampled, @NonNull final Sample<T> sample) {
        final Class<? extends DoubleValue> inputType = sample.value().getClass();
        final List<AnalysisDelegate<? extends DoubleValue>> list = lists.get(inputType);
        if (null == list) {
            return;
        }
        for (final AnalysisDelegate<? extends DoubleValue> delegate : list) {
            try {
                //noinspection unchecked
                ((AnalysisDelegate<T>) delegate).newSample(sampled, sample);
            } catch (Throwable e) {
                logger.fault("newSample failed to cast delegate", e);
            }
        }
    }
}
