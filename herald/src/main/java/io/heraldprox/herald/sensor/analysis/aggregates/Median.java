//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.aggregates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.analysis.sampling.Aggregate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;

public class Median<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private final Queue<Double> minHeap = new PriorityQueue<>(10);
    private final Queue<Double> maxHeap = new PriorityQueue<>(10, Collections.reverseOrder());

    @Override
    public int runs() {
        return 1;
    }

    @Override
    public void beginRun(final int thisRun) {
        run = thisRun;
    }

    @Override
    public void map(@NonNull final Sample<T> value) {
        if (run > 1) return;
        add(value.value().doubleValue());
    }

    @Nullable
    @Override
    public Double reduce() {
        return median();
    }

    @Override
    public void reset() {
        minHeap.clear();
        maxHeap.clear();
    }

    private void add(final double value) {
        if (minHeap.size() == maxHeap.size()) {
            maxHeap.offer(value);
            minHeap.offer(maxHeap.poll());
        } else {
            minHeap.offer(value);
            maxHeap.offer(minHeap.poll());
        }
    }

    @Nullable
    private Double median() {
        if (minHeap.size() > maxHeap.size()) {
            return minHeap.peek();
        } else {
            final Double min = minHeap.peek();
            final Double max = maxHeap.peek();
            if (min != null && max != null) {
                return (min + max) / 2;
            }
            return null;
        }
    }
}
