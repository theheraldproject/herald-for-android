//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisProviderManager<T extends DoubleValue, U extends DoubleValue> implements AnalysisProvider<T, U> {
    private final List<AnalysisProvider<T,U>> providers = new ArrayList<>();

    public AnalysisProviderManager(final AnalysisProvider<T, U> ... providers) {
        this.providers.addAll(Arrays.asList(providers));
    }

    public void add(final AnalysisProvider<T, U> provider) {
        providers.add(provider);
    }

    @Override
    public boolean analyse(Date timeNow, SampledID sampled, SampleList<T> src, CallableForNewSample<U> callable) {
        boolean generated = false;
        for (final AnalysisProvider<T, U> provider : providers) {
            generated = generated || provider.analyse(timeNow, sampled, src, callable);
        }
        return generated;
    }
}
