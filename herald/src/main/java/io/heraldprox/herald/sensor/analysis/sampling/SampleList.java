//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unchecked")
public class SampleList<T extends DoubleValue> implements Iterable<Sample<T>>, Filterable<T> {
    @NonNull
    private final Sample[] data;
    private int oldestPosition, newestPosition;

    public SampleList(final int size) {
        this.data = new Sample[size];
        this.oldestPosition = size;
        this.newestPosition = size;
    }

    public SampleList(final int size, @NonNull final Sample<T> ... samples) {
        this.data = new Sample[size];
        this.oldestPosition = size;
        this.newestPosition = size;
        for (final Sample<T> sample : samples) {
            push(sample);
        }
    }

    public SampleList(@NonNull final Sample<T> ... samples) {
        this.data = new Sample[samples.length];
        this.oldestPosition = data.length;
        this.newestPosition = data.length;
        for (final Sample<T> sample : samples) {
            push(sample);
        }
    }

    public SampleList(@NonNull final Iterator<Sample<T>> iterator) {
        this(toArray(iterator));
    }

    @NonNull
    private static <T> Sample<T>[] toArray(@NonNull final Iterator<Sample<T>> iterator) {
        final List<Sample<T>> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        //noinspection SuspiciousToArrayCall,ToArrayCallWithZeroLengthArrayArgument
        return (Sample<T>[]) list.toArray(new Sample[list.size()]);
    }

    public void push(@NonNull final Sample<T> sample) {
        incrementNewest();
        data[newestPosition] = sample;
    }

    public void push(@NonNull final Date taken, @NonNull final T value) {
        push(new Sample<>(taken, value));
    }

    public void push(final long secondsSinceUnixEpoch, @NonNull final T value) {
        push(new Sample<>(new Date(secondsSinceUnixEpoch), value));
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

    @Nullable
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

    public void clearBeforeDate(@NonNull final Date before) {
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

    @Nullable
    public Date latest() {
        if (newestPosition == data.length) {
            return null;
        }
        if (null == data[newestPosition]) {
            return null;
        }
        return data[newestPosition].taken();
    }

    @Nullable
    public T latestValue() {
        if (newestPosition == data.length) {
            return null;
        }
        if (null == data[newestPosition]) {
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

            @Nullable
            @Override
            public Sample<T> next() {
                try {
                    //noinspection UnnecessaryLocalVariable
                    final Sample<T> value = get(index++);
                    return value;
                } catch (Throwable e) {
                    return null;
                }
            }

            @Override
            public void remove() {
                // Unsupported operation
            }
        };
    }

    @NonNull
    @Override
    public IteratorProxy<T> filter(@NonNull final Filter filter) {
        return new IteratorProxy<>(iterator(), filter);
    }

    @NonNull
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append('[');
        for (int i=0; i<size(); i++) {
            if (i > 0) {
                s.append(" ,");
            }
            final Sample<T> sample = get(i);
            if (sample != null) {
                s.append(sample.toString());
            }
        }
        s.append(']');
        return s.toString();
    }

    @NonNull
    public Summary<T> aggregate(@NonNull final Aggregate<T> ... aggregates) {
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

            for (final Sample<T> sample : this) {
                for (final Aggregate<T> aggregate : aggregates) {
                    aggregate.map(sample);
                }
            }
        }
        return new Summary<>(aggregates);
    }
}
