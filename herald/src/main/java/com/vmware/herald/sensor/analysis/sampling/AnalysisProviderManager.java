//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisProviderManager {
    private final Set<Class<? extends DoubleValue>> outputTypes = new HashSet<>();
    private final Map<Class<? extends DoubleValue>, List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>>> lists = new ConcurrentHashMap<>();
    private final List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> providers = new ArrayList<>();
    private final Map<Class<? extends DoubleValue>, CallableForNewSample<? extends DoubleValue>> callables = new ConcurrentHashMap<>();

    public AnalysisProviderManager(final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> ... providers) {
        for (final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider : providers) {
            add(provider);
        }
    }

    public Set<Class<? extends DoubleValue>> inputTypes() {
        return lists.keySet();
    }

    public Set<Class<? extends DoubleValue>> outputTypes() {
        return outputTypes;
    }

    public void add(final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider) {
        final Class<? extends DoubleValue> inputType = provider.inputType();
        final Class<? extends DoubleValue> outputType = provider.outputType();
        final List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> list = list(inputType);
        list.add(provider);
        outputTypes.add(outputType);
        providers.add(provider);
    }

    private synchronized List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> list(final Class<? extends DoubleValue> inputType) {
        List<AnalysisProvider<? extends DoubleValue, ? extends DoubleValue>> list = lists.get(inputType);
        if (null == list) {
            list = new ArrayList<>(1);
            lists.put(inputType, list);
        }
        return list;
    }

    private synchronized <U extends DoubleValue> CallableForNewSample<U> callable(final Class<U> type, final AnalysisDelegateManager delegates) {
        CallableForNewSample<? extends DoubleValue> callable = callables.get(type);
        if (null == callable) {
            callable = new CallableForNewSample<U>() {
                @Override
                public void newSample(SampledID sampled, Sample<U> item) {
                    delegates.newSample(sampled, item);
                }
            };
            callables.put(type, callable);
        }
        return (CallableForNewSample<U>) callable;
    }

    public <T extends DoubleValue, U extends DoubleValue> void analyse(final Date timeNow, final SampledID sampled, final VariantSet variantSet, final AnalysisDelegateManager delegates) {
        for (final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider : providers) {
            final SampleList<T> input = (SampleList<T>) variantSet.listManager(provider.inputType(), sampled);
            final SampleList<U> output = (SampleList<U>) variantSet.listManager(provider.outputType(), sampled);
            final AnalysisProvider<T,U> typedProvider = (AnalysisProvider<T,U>) provider;
            typedProvider.analyse(timeNow, sampled, input, output, callable(typedProvider.outputType(), delegates));
        }
    }
}
