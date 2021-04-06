//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class AnalysisRunner<T extends DoubleValue, U extends DoubleValue> implements CallableForNewSample<T> {
    private final AnalysisProviderManager<T, U> analysisProviderManager;
    private final AnalysisDelegateManager<U> analysisDelegateManager;
    private final ListManager<T> buffer;

    public AnalysisRunner(final AnalysisProviderManager<T, U> analysisProviderManager, final AnalysisDelegateManager<U> analysisDelegateManager, final int bufferCapacity) {
        this.analysisDelegateManager = analysisDelegateManager;
        this.analysisProviderManager = analysisProviderManager;
        this.buffer = new ListManager<>(bufferCapacity);
    }

    @Override
    public void newSample(SampledID sampled, Sample<T> item) {
        buffer.list(sampled).push(item);
    }

    public void run(Date timeNow) {
        for (final SampledID sampled : buffer.lists()) {
            final SampleList<T> list = buffer.list(sampled);
            analysisProviderManager.analyse(timeNow, sampled, list, analysisDelegateManager);
        }
    }
}
