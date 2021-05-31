//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SampleList<T extends DoubleValue> implements Iterable<Sample<T>>, Filterable<T> {
    private final Sample[] data;
    private int oldestPosition, newestPosition;

    public SampleList(final int size) {
        this.data = new Sample[size];
        this.oldestPosition = size;
        this.newestPosition = size;
    }

    public SampleList(final int size, final Sample<T> ... samples) {
        this.data = new Sample[size];
        this.oldestPosition = size;
        this.newestPosition = size;
        for (final Sample<T> sample : samples) {
            push(sample);
        }
    }

    public SampleList(final Sample<T> ... samples) {
        this.data = new Sample[samples.length];
        this.oldestPosition = data.length;
        this.newestPosition = data.length;
        for (final Sample<T> sample : samples) {
            push(sample);
        }
    }

    public SampleList(final Iterator<Sample<T>> iterator) {
        this(toArray(iterator));
    }

    private final static <T> Sample<T>[] toArray(final Iterator<Sample<T>> iterator) {
        final List<Sample<T>> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return (Sample<T>[]) list.toArray(new Sample[list.size()]);
    }

    public void push(final Sample<T> sample) {
        incrementNewest();
        data[newestPosition] = sample;
    }

    public void push(final Date taken, final T value) {
        push(new Sample<T>(taken, value));
    }

    public void push(final long secondsSinceUnixEpoch, final T value) {
        push(new Sample<T>(new Date(secondsSinceUnixEpoch), value));
    }

    public int size() {
        if (newestPosition == data.length) return 0;
        if (newestPosition >= oldestPosition) {
            // not overlapping the end
            return newestPosition - oldestPosition + 1;
        }
        // we've overlapped
        return (1 + newestPosition) + (data.length - oldestPosition);
    }

    public Sample<T> get(final int index) {
        if (newestPosition >= oldestPosition) {
            return data[index + oldestPosition];
        }
        if (index + oldestPosition >= data.length) {
            // TODO handle the situation where this pos > newestPosition (i.e. gap in the middle)
            return data[index + oldestPosition - data.length];
        }
        return data[index + oldestPosition];
    }

    public void clearBeforeDate(final Date before) {
        if (oldestPosition == data.length) return;
        while (oldestPosition != newestPosition) {
            if (data[oldestPosition].taken().getTime() < before.getTime()) {
                ++oldestPosition;
                if (data.length == oldestPosition) {
                    // overflowed
                    oldestPosition = 0;
                }
            } else {
                return;
            }
        }
        // now we're on the last element
        if (data[oldestPosition].taken().getTime() < before.getTime()) {
            oldestPosition = data.length;
            newestPosition = data.length;
        }
    }

    public void clear() {
        oldestPosition = data.length;
        newestPosition = data.length;
    }

    public Date latest() {
        if (newestPosition == data.length) {
            return null;
        }
        return data[newestPosition].taken();
    }

    public T latestValue() {
        if (newestPosition == data.length) {
            return null;
        }
        return (T) data[newestPosition].value();
    }

    private void incrementNewest() {
        if (newestPosition == data.length) {
            newestPosition = 0;
            oldestPosition = 0;
        } else {
            if (newestPosition == (oldestPosition - 1)) {
                ++oldestPosition;
                if (oldestPosition == data.length) {
                    oldestPosition = 0;
                }
            }
            ++newestPosition;
        }
        if (newestPosition == data.length) {
            // just gone past the end of the container
            newestPosition = 0;
            if (0 == oldestPosition) {
                ++oldestPosition; // erases oldest if not already removed
            }
        }
    }

    @NonNull
    @Override
    public Iterator<Sample<T>> iterator() {
        return new Iterator<Sample<T>>() {
            private int index = 0;
            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public Sample<T> next() {
                try {
                    final Sample<T> value = get(index++);
                    return value;
                } catch (Throwable e) {
                    return null;
                }
            }
        };
    }

    @Override
    public IteratorProxy<T> filter(final Filter filter) {
        return new IteratorProxy<>(iterator(), filter);
    }

    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append('[');
        for (int i=0; i<size(); i++) {
            if (i > 0) {
                s.append(" ,");
            }
            s.append(get(i).toString());
        }
        s.append(']');
        return s.toString();
    }

    public Summary<T> aggregate(final Aggregate<T> ... aggregates) {
        int maxRuns = 1;
        for (final Aggregate<T> aggregate : aggregates) {
            if (aggregate.runs() > maxRuns) {
                maxRuns = aggregate.runs();
            }
        }

        for (int run=1; run<=maxRuns; run++) {
            for (final Aggregate<T> aggregate : aggregates) {
                aggregate.beginRun(run);
            }

            final Iterator<Sample<T>> iterator = iterator();
            while (iterator.hasNext()) {
                final Sample<T> sample = iterator.next();
                for (final Aggregate<T> aggregate : aggregates) {
                    aggregate.map(sample);
                }
            }
        }
        return new Summary<>(aggregates);
    }
}
