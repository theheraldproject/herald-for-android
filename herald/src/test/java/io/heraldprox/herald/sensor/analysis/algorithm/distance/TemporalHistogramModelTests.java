package io.heraldprox.herald.sensor.analysis.algorithm.distance;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.TemporalHistogramModel;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.data.TextFileBuffer;
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

    @Test
    public void textFileWrite() throws Exception {
        final TextFile textFile = new TextFileBuffer();
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, 0, 2, textFile);
        // 0 is in quantisation buffer only
        model.add(new Date(0), "A", 0);
        assertEquals("", textFile.contentsOf());
        // 1 triggers quantisation of 0 and creation of new histogram period A
        model.add(new Date(TimeInterval.hour.value), "A", 1);
        assertEquals("", textFile.contentsOf());
        // 2 triggers quantisation of 1 and creation of new histogram period B which triggers flushing of period A to file
        model.add(new Date(TimeInterval.day.value), "FLUSH_TRIGGER", 2); // Trigger flush
        assertEquals(
                "start,end,0,1,2\n" +
                "1970-01-01 00:00:00.000+0000,1970-01-01 01:00:00.000+0000,1,0,0\n",
                textFile.contentsOf());
        // Manually trigger flushing of quantisation buffer and all histogram periods to file
        model.flush();
        assertEquals(
                "start,end,0,1,2\n" +
                        "1970-01-01 00:00:00.000+0000,1970-01-01 01:00:00.000+0000,1,0,0\n" +
                        "1970-01-01 01:00:00.000+0000,1970-01-01 02:00:00.000+0000,0,1,0\n" +
                        "1970-01-02 00:00:00.000+0000,1970-01-02 01:00:00.000+0000,0,0,1\n",
                textFile.contentsOf());
    }

    @Test
    public void textFileRead() throws Exception {
        final TextFile textFile = new TextFileBuffer(
                "start,end,0,1,2\n" +
                        "1970-01-01 00:00:00.000+0000,1970-01-01 01:00:00.000+0000,1,0,0\n" +
                        "1970-01-01 01:00:00.000+0000,1970-01-01 02:00:00.000+0000,0,1,0\n" +
                        "1970-01-02 00:00:00.000+0000,1970-01-02 01:00:00.000+0000,0,0,1\n");
        final TemporalHistogramModel model = new TemporalHistogramModel(TimeInterval.minute, TimeInterval.hour, 0, 2, textFile);
        final Histogram histogram0 = model.histogram(new Date(0), new Date(TimeInterval.hour.value));
        assertEquals(1, histogram0.count());
        assertEquals(0, histogram0.mode().intValue());
        assertEquals(1, histogram0.count(0));
        final Histogram histogram1 = model.histogram(new Date(TimeInterval.hour.value), new Date(2 * TimeInterval.hour.value));
        assertEquals(1, histogram1.count());
        assertEquals(1, histogram1.mode().intValue());
        assertEquals(1, histogram1.count(1));
        final Histogram histogram2 = model.histogram(new Date(2 * TimeInterval.hour.value), new Date(2 * TimeInterval.day.value));
        assertEquals(1, histogram2.count());
        assertEquals(2, histogram2.mode().intValue());
        assertEquals(1, histogram2.count(2));
    }
}
