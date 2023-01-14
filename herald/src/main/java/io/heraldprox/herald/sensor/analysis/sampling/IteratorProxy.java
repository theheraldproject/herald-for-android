//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.Iterator;

public class IteratorProxy<T extends DoubleValue> implements Iterator<Sample<T>>, Filterable<T> {
    @NonNull
    private final Iterator<Sample<T>> source;
    @NonNull
    private final Filter<T> filter;
    @Nullable
    private Sample<T> nextItem = null;
    private boolean nextItemSet = false;

    public IteratorProxy(@NonNull final Iterator<Sample<T>> source, @NonNull final Filter<T> filter) {
        this.source = source;
        this.filter = filter;
    }

    @Override
    public boolean hasNext() {
        return nextItemSet || moveToNextItem();
    }

    @Nullable
    @Override
    public Sample<T> next() {
        if (!nextItemSet && !moveToNextItem()) {
            return null;
        }
        nextItemSet = false;
        return nextItem;
    }

    @Override
    public void remove() {
        // Unsupported operation
    }

    @NonNull
    @Override
    public IteratorProxy<T> filter(@NonNull final Filter<T> filter) {
        return new IteratorProxy<>(this, filter);
    }

    @NonNull
    public Summary<T> aggregate(@NonNull final Aggregate<T> ... aggregates) {
        return toView().aggregate(aggregates);
    }

    @NonNull
    public SampleList<T> toView() {
        return new SampleList<>(this);
    }

    private boolean moveToNextItem() {
        while (source.hasNext()) {
            final Sample<T> item = source.next();
            if (filter.test(item)) {
                nextItem = item;
                nextItemSet = true;
                return true;
            }
        }
        return false;
    }
}
