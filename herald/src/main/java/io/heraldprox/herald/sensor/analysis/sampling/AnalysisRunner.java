//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class AnalysisRunner {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "AnalysisRunner");
    @NonNull
    private final AnalysisProviderManager analysisProviderManager;
    @NonNull
    private final AnalysisDelegateManager analysisDelegateManager;
    @NonNull
    private final VariantSet variantSet;

    public AnalysisRunner(@NonNull final AnalysisProviderManager analysisProviderManager, @NonNull final AnalysisDelegateManager analysisDelegateManager, final int defaultListSize) {
        this.analysisDelegateManager = analysisDelegateManager;
        this.analysisProviderManager = analysisProviderManager;
        this.variantSet = new VariantSet(defaultListSize);
    }

    @NonNull
    public VariantSet variantSet() {
        return variantSet;
    }

    public <T extends DoubleValue> void newSample(@NonNull final SampledID sampled, @NonNull final Sample<T> item) {
        variantSet.push(sampled, item);
    }

    public void run() {
        run(new Date());
    }

    public void run(@NonNull final Date timeNow) {
        for (final SampledID sampled : variantSet.sampledIDs()) {
            analysisProviderManager.analyse(timeNow, sampled, variantSet, analysisDelegateManager);
        }
    }
}
