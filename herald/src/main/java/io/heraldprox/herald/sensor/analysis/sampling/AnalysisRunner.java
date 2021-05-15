//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class AnalysisRunner {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "AnalysisRunner");
    private final AnalysisProviderManager analysisProviderManager;
    private final AnalysisDelegateManager analysisDelegateManager;
    private final VariantSet variantSet;

    public AnalysisRunner(final AnalysisProviderManager analysisProviderManager, final AnalysisDelegateManager analysisDelegateManager, final int defaultListSize) {
        this.analysisDelegateManager = analysisDelegateManager;
        this.analysisProviderManager = analysisProviderManager;
        this.variantSet = new VariantSet(defaultListSize);
    }

    public VariantSet variantSet() {
        return variantSet;
    }

    public <T extends DoubleValue> void newSample(SampledID sampled, Sample<T> item) {
        variantSet.push(sampled, item);
    }

    public void run() {
        run(new Date());
    }

    public void run(Date timeNow) {
        for (final SampledID sampled : variantSet.sampledIDs()) {
            analysisProviderManager.analyse(timeNow, sampled, variantSet, analysisDelegateManager);
        }
    }
}
