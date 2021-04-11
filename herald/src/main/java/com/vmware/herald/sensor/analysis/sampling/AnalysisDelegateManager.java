//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisDelegateManager {
    private final Map<Class<? extends DoubleValue>, List<AnalysisDelegate<? extends DoubleValue>>> lists = new ConcurrentHashMap<>();

    public AnalysisDelegateManager(final AnalysisDelegate<? extends DoubleValue> ... delegates) {
        for (final AnalysisDelegate<? extends DoubleValue> delegate : delegates) {
            add(delegate);
        }
    }

    public Set<Class<? extends DoubleValue>> inputTypes() {
        return lists.keySet();
    }

    public void add(final AnalysisDelegate<? extends DoubleValue> delegate) {
        final Class<? extends DoubleValue> inputType = delegate.inputType();
        final List<AnalysisDelegate<? extends DoubleValue>> list = list(inputType);
        list.add(delegate);
    }

    private synchronized List<AnalysisDelegate<? extends DoubleValue>> list(final Class<? extends DoubleValue> inputType) {
        List<AnalysisDelegate<? extends DoubleValue>> list = lists.get(inputType);
        if (null == list) {
            list = new ArrayList<>(1);
            lists.put(inputType, list);
        }
        return list;
    }

    public <T extends DoubleValue> void newSample(SampledID sampled, Sample<T> sample) {
        final Class<? extends DoubleValue> inputType = sample.value().getClass();
        final List<AnalysisDelegate<? extends DoubleValue>> list = lists.get(inputType);
        if (null == list) {
            return;
        }
        for (final AnalysisDelegate<? extends DoubleValue> delegate : list) {
            ((AnalysisDelegate<T>) delegate).newSample(sampled, sample);
        }
    }
}
