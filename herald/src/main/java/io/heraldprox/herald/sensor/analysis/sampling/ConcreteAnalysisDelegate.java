//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class ConcreteAnalysisDelegate<T extends DoubleValue> implements AnalysisDelegate<T> {
    @NonNull
    private final Class<T> inputType;
    @NonNull
    private final ListManager<T> listManager;
    @NonNull
    private final SampleList<T> sampleList;

    public ConcreteAnalysisDelegate(@NonNull final Class<T> inputType, final int listSize) {
        this.inputType = inputType;
        this.listManager = new ListManager<>(listSize);
        this.sampleList = new SampleList<>(listSize);
    }

    @NonNull
    @Override
    public Class<T> inputType() {
        return inputType;
    }

    @Override
    public void reset() {
        listManager.clear();
    }

    @Override
    public void removeSamplesFor(SampledID sampled) {
        // Fix for https://github.com/theheraldproject/herald-for-android/issues/239
        listManager.remove(sampled);
    }

    @NonNull
    @Override
    public SampleList<T> samples() {
        return sampleList;
    }

    @NonNull
    public SampleList<T> samples(@NonNull final SampledID sampledID) {
        return listManager.list(sampledID);
    }

    @Override
    public void newSample(@NonNull final SampledID sampled, @NonNull final Sample<T> item) {
        listManager.push(sampled, item);
        sampleList.push(item);
    }
}