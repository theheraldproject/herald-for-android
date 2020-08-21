package org.c19x.sensor.datatype;

public class Sample {
    protected long n;
    protected double m1, m2, m3, m4;
    private double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;

    public Sample() {
    }

    public Sample(final double x, final int n) {
        for (int i = n; i-- > 0; ) {
            add(x);
        }
    }

    public synchronized void add(final double x) {
        final long n1 = n;
        n++;
        final double delta = x - m1;
        final double delta_n = delta / n;
        final double delta_n2 = delta_n * delta_n;
        final double term1 = delta * delta_n * n1;
        m1 += delta_n;
        m4 += term1 * delta_n2 * (n * n - 3 * n + 3) + 6 * delta_n2 * m2 - 4 * delta_n * m3;
        m3 += term1 * delta_n * (n - 2) - 3 * delta_n * m2;
        m2 += term1;
        if (x < min) {
            min = x;
        }
        if (x > max) {
            max = x;
        }
    }

    public long count() {
        return n;
    }

    public Double mean() {
        if (n > 0) {
            return m1;
        } else {
            return null;
        }
    }

    public Double standardDeviation() {
        if (n > 1) {
            return StrictMath.sqrt(m2 / (n - 1d));
        } else {
            return null;
        }
    }

    public Double upperBound(final double standardDeviations) {
        final Double mean = mean();
        final Double sd = standardDeviation();
        if (mean != null && sd != null) {
            return mean + standardDeviations * sd;
        } else {
            return null;
        }
    }

    public Double min() {
        if (n > 0) {
            return min;
        } else {
            return null;
        }
    }

    public Double max() {
        if (n > 0) {
            return max;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "[count=" + count() + ",mean=" + mean() + ",sd=" + standardDeviation() + ",min=" + min() + ",max=" + max() + "]";
    }
}
