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
        if (list == null) {
            list = new ArrayList<>(1);
            lists.put(inputType, list);
        }
        return list;
    }

    public <T extends DoubleValue, U extends DoubleValue> void analyse(final Date timeNow, final SampledID sampled, final VariantSet variantSet, final AnalysisDelegateManager delegates) {
        for (final AnalysisProvider<? extends DoubleValue, ? extends DoubleValue> provider : providers) {
            final SampleList<T> input = (SampleList<T>) variantSet.listManager(provider.inputType(), sampled);
            final SampleList<U> output = (SampleList<U>) variantSet.listManager(provider.outputType(), sampled);
            final AnalysisProvider<T,U> typedProvider = (AnalysisProvider<T,U>) provider;
            typedProvider.analyse(timeNow, sampled, input, output, new CallableForNewSample<U>() {
                @Override
                public void newSample(SampledID sampled, Sample<U> item) {
                    delegates.newSample(sampled, item);
                }
            });
        }
    }


//
//    public <T extends DoubleValue> void newSample(SampledID sampled, Sample<T> sample) {
//        final Class<? extends DoubleValue> inputType = sample.value().getClass();
//        final List<AnalysisDelegate<? extends DoubleValue>> list = lists.get(inputType);
//        if (list == null) {
//            return;
//        }
//        for (final AnalysisDelegate<? extends DoubleValue> delegate : list) {
//            ((AnalysisDelegate<T>) delegate).newSample(sampled, sample);
//        }
//    }
//
//
//
//    @Override
//    public boolean analyse(Date timeNow, SampledID sampled, SampleList<T> src, CallableForNewSample<U> callable) {
//        boolean generated = false;
//        for (final AnalysisProvider<T, U> provider : providers) {
//            generated = generated || provider.analyse(timeNow, sampled, src, callable);
//        }
//        return generated;
//    }
}
