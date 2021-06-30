//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Single-pass model for normal distribution (Gaussian). Model enables computation of mean, variance,
 * and standard deviation in single pass.
 */
public class Distribution {
    // Accumulator for count
    private long n = 0;
    // Accumulator for mean
    private double m1 = 0;
    // Accumulator for variance
    private double m2 = 0;
    // Min and max value
    private double min = Double.MAX_VALUE;
    private double max = -Double.MAX_VALUE;

    /**
     * Initialise empty distribution.
     */
    public Distribution() {
    }

    /**
     * Initialise distribution with mean and frequency. Implies 0 variance.
     * @param x Mean value.
     * @param f Sample count.
     */
    public Distribution(final double x, final long f) {
        n = f;
        m1 = x;
        min = x;
        max = x;
    }

    /**
     * Add single occurrence of sample value to distribution.
     * @param x Sample value.
     */
    public synchronized void add(final double x) {
        // Update count, mean, variance
        final long n1 = n;
        n++;
        final double delta = x - m1;
        final double delta_n = delta / n;
        final double term1 = delta * delta_n * n1;
        m1 += delta_n;
        m2 += term1;
        // Update min, max
        if (x < min) {
            min = x;
        }
        if (x > max) {
            max = x;
        }
    }

    /**
     * Add multiple occurrences of sample value to distribution.
     * @param x Sample value.
     * @param f Number of occurrences.
     */
    public void add(final double x, final long f) {
        add(new Distribution(x, f));
    }

    /**
     * Add another distribution to this distribution. Equivalent to building a distribution with
     * all the samples captured by the two separate distributions.
     * @param distribution Distribution to merged with this distribution.
     */
    public void add(@NonNull final Distribution distribution) {
        // Copy other distribution if this distribution is empty
        if (0 == n) {
            n = distribution.n;
            m1 = distribution.m1;
            m2 = distribution.m2;
            min = distribution.min;
            max = distribution.max;
            return;
        }
        // Combine distribution if this distribution is not empty
        final Distribution combined = new Distribution();
        combined.n = n + distribution.n;

        final double delta = distribution.m1 - m1;
        final double delta2 = delta * delta;

        combined.m1 = (n * m1 + distribution.n * distribution.m1) / combined.n;
        combined.m2 = m2 + distribution.m2 + delta2 * n * distribution.n / combined.n;
        combined.min = Math.min(min, distribution.min);
        combined.max = Math.max(max, distribution.max);

        n = combined.n;
        m1 = combined.m1;
        m2 = combined.m2;
        min = combined.min;
        max = combined.max;
    }

    /**
     * Sample count.
     * @return Number of samples accumulated by this distribution.
     */
    public long count() {
        return n;
    }

    /**
     * Sample mean.
     * @return Mean, or null if count is 0.
     */
    @Nullable
    public Double mean() {
        if (n > 0) {
            return m1;
        } else {
            return null;
        }
    }

    /**
     * Sample variance.
     * @return Variance, or null if count is 0 or 1.
     */
    @Nullable
    public Double variance() {
        if (n > 1) {
            return m2 / (n - 1d);
        } else {
            return null;
        }
    }

    /**
     * Sample standard deviation.
     * @return Standard deviation, or null if count is 0 or 1.
     */
    @Nullable
    public Double standardDeviation() {
        if (n > 1) {
            return StrictMath.sqrt(m2 / (n - 1d));
        } else {
            return null;
        }
    }

    /**
     * Minimum sample value.
     * @return Minimum sample value, or null if count is 0.
     */
    @Nullable
    public Double min() {
        if (n > 0) {
            return min;
        } else {
            return null;
        }
    }

    /**
     * Maximum sample value.
     * @return Maximum sample value, or null if count is 0.
     */
    @Nullable
    public Double max() {
        if (n > 0) {
            return max;
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "[count=" + count() + ",mean=" + mean() + ",sd=" + standardDeviation() + ",min=" + min() + ",max=" + max() + "]";
    }
}
