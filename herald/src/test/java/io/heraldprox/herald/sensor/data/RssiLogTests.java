package io.heraldprox.herald.sensor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Histogram;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

public class RssiLogTests {

    // MARK: - Quantisation tests

    @Test
    public void quantise_empty() throws Exception {
        // Empty list should return empty list
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        final List<RssiLog.PointMeasurement> quantised = RssiLog.quantise(list, TimeInterval.seconds(10));
        assertEquals(0, quantised.size());
    }

    @Test
    public void quantise_one_to_one() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -10d, null));
        final List<RssiLog.PointMeasurement> quantised = RssiLog.quantise(list, TimeInterval.seconds(10));
        assertEquals(1, quantised.size());
        // One measurement returns same measurement with quantised timestamp
        assertEquals(0, quantised.get(0).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", quantised.get(0).target.value);
        assertEquals(-10d, quantised.get(0).rssi, Double.MIN_VALUE);
        assertNull(quantised.get(0).txPower);
    }

    @Test
    public void quantise_two_to_one() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -10d, null));
        list.add(new RssiLog.PointMeasurement(new Date(9), new TargetIdentifier("A"), -20d, null));
        final List<RssiLog.PointMeasurement> quantised = RssiLog.quantise(list, TimeInterval.seconds(10));
        // Two measurements returns new measurement with quantised timestamp and max RSSI
        assertEquals(1, quantised.size());
        assertEquals(0, quantised.get(0).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", quantised.get(0).target.value);
        assertEquals(-10d, quantised.get(0).rssi, Double.MIN_VALUE);
        assertNull(quantised.get(0).txPower);
    }

    @Test
    public void quantise_three_to_two() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -10d, null));
        list.add(new RssiLog.PointMeasurement(new Date(9), new TargetIdentifier("A"), -20d, -12d));
        list.add(new RssiLog.PointMeasurement(new Date(10), new TargetIdentifier("A"), -30d, null));
        final List<RssiLog.PointMeasurement> quantised = RssiLog.quantise(list, TimeInterval.seconds(10));
        assertEquals(2, quantised.size());
        // Two measurements returns new measurement with quantised timestamp and max RSSI and max TxPower for target in time window
        assertEquals(0, quantised.get(0).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", quantised.get(0).target.value);
        assertEquals(-10d, quantised.get(0).rssi, Double.MIN_VALUE);
        assertEquals(-12d, quantised.get(0).txPower, Double.MIN_VALUE);
        // One measurement in new time window returns same measurement with quantised timestamp
        assertEquals(10, quantised.get(1).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", quantised.get(1).target.value);
        assertEquals(-30d, quantised.get(1).rssi, Double.MIN_VALUE);
        assertNull(quantised.get(1).txPower);
    }

    @Test
    public void quantise_four_to_three() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -10d, null));
        list.add(new RssiLog.PointMeasurement(new Date(9), new TargetIdentifier("A"), -20d, -12d));
        list.add(new RssiLog.PointMeasurement(new Date(9), new TargetIdentifier("B"), -20d, null));
        list.add(new RssiLog.PointMeasurement(new Date(11), new TargetIdentifier("A"), -30d, null));
        final List<RssiLog.PointMeasurement> quantised = RssiLog.quantise(list, TimeInterval.seconds(10));
        assertEquals(3, quantised.size());
        // Two measurements returns new measurement with quantised timestamp and max RSSI and max TxPower for target in time window
        assertEquals(0, quantised.get(0).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", quantised.get(0).target.value);
        assertEquals(-10d, quantised.get(0).rssi, Double.MIN_VALUE);
        assertEquals(-12d, quantised.get(0).txPower, Double.MIN_VALUE);
        // One measurement for different target is handled separately from "A"
        assertEquals(0, quantised.get(1).timestamp.secondsSinceUnixEpoch());
        assertEquals("B", quantised.get(1).target.value);
        assertEquals(-20d, quantised.get(1).rssi, Double.MIN_VALUE);
        assertNull(quantised.get(1).txPower);
        // One measurement in new time window returns same measurement with quantised timestamp
        assertEquals(10, quantised.get(2).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", quantised.get(2).target.value);
        assertEquals(-30d, quantised.get(2).rssi, Double.MIN_VALUE);
        assertNull(quantised.get(2).txPower);
    }

    // MARK: - Subdata tests

    @Test
    public void subdata_empty() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        final List<RssiLog.PointMeasurement> subdata = RssiLog.subdata(list, new Date(0), new Date(Long.MAX_VALUE));
        assertEquals(0, subdata.size());
    }

    @Test
    public void subdata_one_to_one() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -10d, -12d));
        final List<RssiLog.PointMeasurement> subdata = RssiLog.subdata(list, new Date(1), new Date(2));
        assertEquals(1, subdata.size());
        assertEquals(1, subdata.get(0).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", subdata.get(0).target.value);
        assertEquals(-10d, subdata.get(0).rssi, Double.MIN_VALUE);
        assertEquals(-12d, subdata.get(0).txPower, Double.MIN_VALUE);
    }

    @Test
    public void subdata_one_to_zero_before() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(0), new TargetIdentifier("A"), -10d, -12d));
        final List<RssiLog.PointMeasurement> subdata = RssiLog.subdata(list, new Date(1), new Date(2));
        assertEquals(0, subdata.size());
    }

    @Test
    public void subdata_one_to_zero_after() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(2), new TargetIdentifier("A"), -10d, -12d));
        final List<RssiLog.PointMeasurement> subdata = RssiLog.subdata(list, new Date(1), new Date(2));
        assertEquals(0, subdata.size());
    }

    // MARK: - Filter by RSSI

    @Test
    public void filterByRSSI_empty() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        final List<RssiLog.PointMeasurement> subdata = RssiLog.filterByRssi(list, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(0, subdata.size());
    }

    @Test
    public void filterByRSSI_one_to_one() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -10d, -12d));
        final List<RssiLog.PointMeasurement> subdata = RssiLog.filterByRssi(list, -10d, -9d);
        assertEquals(1, subdata.size());
        assertEquals(1, subdata.get(0).timestamp.secondsSinceUnixEpoch());
        assertEquals("A", subdata.get(0).target.value);
        assertEquals(-10d, subdata.get(0).rssi, Double.MIN_VALUE);
        assertEquals(-12d, subdata.get(0).txPower, Double.MIN_VALUE);
    }

    @Test
    public void filterByRSSI_one_to_zero_under() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -11d, -12d));
        final List<RssiLog.PointMeasurement> subdata = RssiLog.filterByRssi(list, -10d, -9d);
        assertEquals(0, subdata.size());
    }

    @Test
    public void filterByRSSI_one_to_zero_over() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -9d, -12d));
        final List<RssiLog.PointMeasurement> subdata = RssiLog.filterByRssi(list, -10d, -9d);
        assertEquals(0, subdata.size());
    }

    // MARK: - Histogram

    @Test
    public void histogramOfRssi_empty() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        final Histogram histogram = RssiLog.histogramOfRssi(list, -100d, 0d);
        assertEquals(0, histogram.count());
    }

    @Test
    public void histogramOfRssi_one() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -50d, null));
        final Histogram histogram = RssiLog.histogramOfRssi(list, -100d, 0d);
        assertEquals(1, histogram.count());
        assertEquals(1, histogram.count(-50));
    }

    @Test
    public void histogramOfRssi_one_round_up() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -50.4d, null));
        final Histogram histogram = RssiLog.histogramOfRssi(list, -100d, 0d);
        assertEquals(1, histogram.count());
        assertEquals(1, histogram.count(-50));
        assertEquals(0, histogram.count(-51));
    }

    @Test
    public void histogramOfRssi_one_round_down() throws Exception {
        final List<RssiLog.PointMeasurement> list = new ArrayList<>();
        list.add(new RssiLog.PointMeasurement(new Date(1), new TargetIdentifier("A"), -50.6d, null));
        final Histogram histogram = RssiLog.histogramOfRssi(list, -100d, 0d);
        assertEquals(1, histogram.count());
        assertEquals(0, histogram.count(-50));
        assertEquals(1, histogram.count(-51));
    }

    // MARK: - Histogram smoothing

    @Test
    public void smoothAcrossBins_empty() throws Exception {
        final Histogram histogram = new Histogram(0,10);
        final Histogram smoothed = RssiLog.smoothAcrossBins(histogram, 0);
        assertEquals(0, smoothed.count());
    }

    @Test
    public void smoothAcrossBins_one() throws Exception {
        final Histogram histogram = new Histogram(0,10);
        histogram.add(5, 100);
        assertEquals(100, RssiLog.smoothAcrossBins(histogram, 0).count());
        assertEquals(100, RssiLog.smoothAcrossBins(histogram, 1).count());
        // Window of 3 means 100 / 3 = 33 at 4, 5, 6
        assertEquals(99, RssiLog.smoothAcrossBins(histogram, 3).count());
        assertEquals(33, RssiLog.smoothAcrossBins(histogram, 3).count(4));
        assertEquals(33, RssiLog.smoothAcrossBins(histogram, 3).count(5));
        assertEquals(33, RssiLog.smoothAcrossBins(histogram, 3).count(6));
        // Window of 5 means 100 / 5 = 20 at 3, 4, 5, 6, 7
        assertEquals(100, RssiLog.smoothAcrossBins(histogram, 5).count());
        assertEquals(20, RssiLog.smoothAcrossBins(histogram, 5).count(3));
        assertEquals(20, RssiLog.smoothAcrossBins(histogram, 5).count(4));
        assertEquals(20, RssiLog.smoothAcrossBins(histogram, 5).count(5));
        assertEquals(20, RssiLog.smoothAcrossBins(histogram, 5).count(6));
        assertEquals(20, RssiLog.smoothAcrossBins(histogram, 5).count(7));
    }
}
