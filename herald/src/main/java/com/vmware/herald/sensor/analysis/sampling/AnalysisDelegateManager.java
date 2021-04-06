//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnalysisDelegateManager<T extends DoubleValue> implements CallableForNewSample<T> {
    private final List<AnalysisDelegate<T>> delegates = new ArrayList<>();

    public AnalysisDelegateManager(final AnalysisDelegate<T> ... delegates) {
        this.delegates.addAll(Arrays.asList(delegates));
    }

    public void add(final AnalysisDelegate<T> delegate) {
        delegates.add(delegate);
    }

    @Override
    public void newSample(SampledID sampled, Sample<T> sample) {
        for (final AnalysisDelegate<T> delegate : delegates) {
            delegate.newSample(sampled, sample);
        }
    }
}
