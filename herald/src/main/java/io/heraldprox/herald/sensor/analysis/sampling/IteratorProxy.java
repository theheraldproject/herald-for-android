//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.Iterator;

public class IteratorProxy<T extends DoubleValue> implements Iterator<Sample<T>>, Filterable<T> {
    private Iterator<Sample<T>> source;
    private Filter<T> filter;
    private Sample<T> nextItem;
    private boolean nextItemSet = false;

    public IteratorProxy(final Iterator<Sample<T>> source, final Filter<T> filter) {
        this.source = source;
        this.filter = filter;
    }

    @Override
    public boolean hasNext() {
        return nextItemSet || moveToNextItem();
    }

    @Override
    public Sample<T> next() {
        if (!nextItemSet && !moveToNextItem()) {
            return null;
        }
        nextItemSet = false;
        return nextItem;
    }

    @Override
    public IteratorProxy<T> filter(final Filter filter) {
        return new IteratorProxy<>(this, filter);
    }

    public Summary<T> aggregate(final Aggregate<T> ... aggregates) {
        return toView().aggregate(aggregates);
    }

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
