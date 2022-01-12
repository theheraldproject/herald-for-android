package io.heraldprox.herald.sensor.analysis.algorithm.distance;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.TemporalHistogramModel;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Histogram;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

public class TemporalHistogramModelTests {

    @Test
    public void histogram_empty() {
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, -100, 0);
        final Histogram histogram = model.histogram(new Date(0), new Date(TimeInterval.hour.value * 2));
        assertEquals(0, histogram.count());
    }

    @Test
    public void histogram_one() {
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, -100, 0);
        model.add(new Date(0), "A", -50);
        model.add(new Date(TimeInterval.day.value), "FLUSH_TRIGGER", 0); // Trigger flush
        final Histogram histogram = model.histogram(new Date(0), new Date(TimeInterval.hour.value));
        assertEquals(1, histogram.count());
        assertEquals(-50, histogram.mode().intValue());
    }

    @Test
    public void histogram_one_quantisation() {
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, -100, 0);
        model.add(new Date(0), "A", -50);
        model.add(new Date(1), "A", -40);
        model.add(new Date(TimeInterval.day.value), "FLUSH_TRIGGER", 0); // Trigger flush
        final Histogram histogram = model.histogram(new Date(0), new Date(TimeInterval.hour.value));
        assertEquals(1, histogram.count());
        assertEquals(-40, histogram.mode().intValue());
    }

    @Test
    public void histogram_two_quantisation() {
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, -100, 0);
        model.add(new Date(0), "A", -50);
        model.add(new Date(1), "A", -40);
        model.add(new Date(2), "B", -60);
        model.add(new Date(TimeInterval.day.value), "FLUSH_TRIGGER", 0); // Trigger flush
        final Histogram histogram = model.histogram(new Date(0), new Date(TimeInterval.hour.value));
        assertEquals(2, histogram.count());
        assertEquals(1, histogram.count(-40)); // "A"
        assertEquals(1, histogram.count(-60)); // "B"
    }

    @Test
    public void histogram_two_periods() {
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, -100, 0);
        model.add(new Date(0), "A", -50);
        model.add(new Date(TimeInterval.hour.value), "A", -20);
        model.add(new Date(TimeInterval.day.value), "FLUSH_TRIGGER", 0); // Trigger flush
        final Histogram histogram1 = model.histogram(new Date(0), new Date(TimeInterval.hour.value));
        assertEquals(1, histogram1.count());
        assertEquals(-50, histogram1.mode().intValue());
        final Histogram histogram2 = model.histogram(new Date(TimeInterval.hour.value), new Date(TimeInterval.hour.value * 2));
        assertEquals(1, histogram2.count());
        assertEquals(-20, histogram2.mode().intValue());
        final Histogram histogram3 = model.histogram(new Date(0), new Date(TimeInterval.hour.value * 2));
        assertEquals(2, histogram3.count());
        assertEquals(1, histogram3.count(-20));
        assertEquals(1, histogram3.count(-50));
    }
}
