//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class AnalysisProviderManager {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "AnalysisProviderManager");
    private final Set<Class<? extends DoubleValue>> outputTypes = new HashSet<>();
    private final Map<Class<? extends DoubleValue>, List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>>> lists = new ConcurrentHashMap<>();
    private final List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> providers = new ArrayList<>();
    private final Map<Class<? extends DoubleValue>, CallableForNewSample<? extends DoubleValue>> callables = new ConcurrentHashMap<>();

    public AnalysisProviderManager(@NonNull final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> ... providers) {
        for (final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider : providers) {
            add(provider);
        }
    }

    @NonNull
    public Set<Class<? extends DoubleValue>> inputTypes() {
        return lists.keySet();
    }

    @NonNull
    public Set<Class<? extends DoubleValue>> outputTypes() {
        return outputTypes;
    }

    public void add(@NonNull final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider) {
        final Class<? extends DoubleValue> inputType = provider.inputType();
        final Class<? extends DoubleValue> outputType = provider.outputType();
        final List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> list = list(inputType);
        list.add(provider);
        outputTypes.add(outputType);
        providers.add(provider);
    }

    @NonNull
    private synchronized List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> list(@NonNull final Class<? extends DoubleValue> inputType) {
        List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> list = lists.get(inputType);
        if (null == list) {
            list = new ArrayList<>(1);
            lists.put(inputType, list);
        }
        return list;
    }

    @NonNull
    private synchronized <U extends DoubleValue> CallableForNewSample<U> callable(@NonNull final Class<U> type, @NonNull final AnalysisDelegateManager delegates) {
        CallableForNewSample<? extends DoubleValue> callable = callables.get(type);
        if (null == callable) {
            callable = new CallableForNewSample<U>() {
                @Override
                public void newSample(@NonNull final SampledID sampled, @NonNull final Sample<U> item) {
                    delegates.newSample(sampled, item);
                }
            };
            callables.put(type, callable);
        }
        return (CallableForNewSample<U>) callable;
    }

    @SuppressWarnings("UnusedReturnValue")
    public <T extends DoubleValue, U extends DoubleValue> boolean analyse(@NonNull final Date timeNow, @NonNull final SampledID sampled, @NonNull final VariantSet variantSet, @NonNull final AnalysisDelegateManager delegates) {
        boolean update = false;
        for (final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider : providers) {
            final SampleList<T> input = (SampleList<T>) variantSet.listManager(provider.inputType(), sampled);
            final SampleList<U> output = (SampleList<U>) variantSet.listManager(provider.outputType(), sampled);
            final AnalysisProvider<T,U> typedProvider = (AnalysisProvider<T,U>) provider;
            final boolean hasUpdate = typedProvider.analyse(timeNow, sampled, input, output, callable(typedProvider.outputType(), delegates));
            update = update || hasUpdate;
        }
        return update;
    }
}
