//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import io.heraldprox.herald.sensor.analysis.aggregates.Gaussian;
import io.heraldprox.herald.sensor.analysis.aggregates.Mean;
import io.heraldprox.herald.sensor.analysis.aggregates.Median;
import io.heraldprox.herald.sensor.analysis.aggregates.Mode;
import io.heraldprox.herald.sensor.analysis.aggregates.Variance;
import io.heraldprox.herald.sensor.analysis.algorithms.distance.FowlerBasic;
import io.heraldprox.herald.sensor.analysis.algorithms.risk.RiskAggregationBasic;
import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.IteratorProxy;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.analysis.sampling.SampleList;
import io.heraldprox.herald.sensor.analysis.sampling.Summary;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.Int32;
import io.heraldprox.herald.sensor.datatype.RSSI;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "unchecked"})
public class RangesTests {

    @Test
    public void ranges_iterator_proxy() {
        final Filter<Int32> workingAge = new InRange<>(18, 65);
        final SampleList<Int32> ages = new SampleList<>(5);
        ages.push(10, new Int32(12));
        ages.push(20,new Int32(14));
        ages.push(30,new Int32(19));
        ages.push(40,new Int32(45));
        ages.push(50,new Int32(66));
        final IteratorProxy<Int32> proxy = ages.filter(new NoOp<>());
        assertTrue(proxy.hasNext());
        assertEquals(proxy.next().value().value, 12);
        assertEquals(proxy.next().value().value, 14);
        assertEquals(proxy.next().value().value, 19);
        assertEquals(proxy.next().value().value, 45);
        assertEquals(proxy.next().value().value, 66);
        assertFalse(proxy.hasNext());
    }

    @Test
    public void ranges_filter_typed() {
        final Filter<Int32> workingAge = new InRange<>(18, 65);
        final SampleList<Int32> ages = new SampleList<>(5);
        ages.push(10, new Int32(12));
        ages.push(20,new Int32(14));
        ages.push(30,new Int32(19));
        ages.push(40,new Int32(45));
        ages.push(50,new Int32(66));
        final IteratorProxy<Int32> iter = ages.filter(workingAge);
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, 19);
        assertEquals(iter.next().value().value, 45);
        assertFalse(iter.hasNext());
    }

    @Test
    public void ranges_filter_generic() {
        final Filter<Int32> workingAge = new InRange<>(18, 65);
        final SampleList<Int32> ages = new SampleList<>(5);
        ages.push(10, new Int32(12));
        ages.push(20,new Int32(14));
        ages.push(30,new Int32(19));
        ages.push(40,new Int32(45));
        ages.push(50,new Int32(66));

        final SampleList<Int32> workingAges = ages.filter(workingAge).toView();

        final Iterator<Sample<Int32>> iter = workingAges.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, 19);
        assertEquals(iter.next().value().value, 45);
        assertFalse(iter.hasNext());

        assertEquals(workingAges.size(), 2);
        assertEquals(workingAges.get(0).value().value, 19);
        assertEquals(workingAges.get(1).value().value, 45);
    }

    @Test
    public void ranges_filter_multi() {
        final Filter<Int32> workingAge = new InRange<>(18, 65);
        final Filter<Int32> over21 = new GreaterThan<>(21);
        final SampleList<Int32> ages = new SampleList<>(5);
        ages.push(10, new Int32(12));
        ages.push(20,new Int32(14));
        ages.push(30,new Int32(19));
        ages.push(40,new Int32(45));
        ages.push(50,new Int32(66));

        final SampleList<Int32> workingAges = ages.filter(workingAge).filter(over21).toView();

        final Iterator<Sample<Int32>> iter = workingAges.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, 45);
        assertFalse(iter.hasNext());

        assertEquals(workingAges.size(), 1);
        assertEquals(workingAges.get(0).value().value, 45);
    }

    @Test
    public void ranges_iterator_rssisamples() {
        final Filter<RSSI> valid = new InRange<>(-99, -10);
        final Filter<RSSI> strong = new LessThan<>(-59);
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(1234, new RSSI(-9));
        sl.push(1244, new RSSI(-60));
        sl.push(1265, new RSSI(-58));
        sl.push(1282, new RSSI(-61));
        sl.push(1294, new RSSI(-100));

        final IteratorProxy<RSSI> proxy = sl.filter(new NoOp());
        assertTrue(proxy.hasNext());
        assertEquals(proxy.next().value().value, -9, Double.MIN_VALUE);
        assertEquals(proxy.next().value().value, -60, Double.MIN_VALUE);
        assertEquals(proxy.next().value().value, -58, Double.MIN_VALUE);
        assertEquals(proxy.next().value().value, -61, Double.MIN_VALUE);
        assertEquals(proxy.next().value().value, -100, Double.MIN_VALUE);
        assertFalse(proxy.hasNext());
    }

    @Test
    public void ranges_filter_multi_rssisamples() {
        final Filter<RSSI> valid = new InRange<>(-99, -10);
        final Filter<RSSI> strong = new LessThan<>(-59);
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(1234, new RSSI(-9));
        sl.push(1244, new RSSI(-60));
        sl.push(1265, new RSSI(-58));
        sl.push(1282, new RSSI(-61));
        sl.push(1294, new RSSI(-100));

        final SampleList<RSSI> values = sl.filter(valid).filter(strong).toView();
        final Iterator<Sample<RSSI>> iter = values.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -60, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -61, Double.MIN_VALUE);
        assertFalse(iter.hasNext());

        assertEquals(values.size(), 2);
        assertEquals(values.get(0).value().value, -60, Double.MIN_VALUE);
        assertEquals(values.get(1).value().value, -61, Double.MIN_VALUE);
    }

    @Test
    public void ranges_filter_multi_summarise() {
        final Filter<RSSI> valid = new InRange<>(-99, -10);
        final Filter<RSSI> strong = new LessThan<>(-59);
        final SampleList<RSSI> sl = new SampleList<>(20);
        sl.push(1234, new RSSI(-9));
        sl.push(1244, new RSSI(-60));
        sl.push(1265, new RSSI(-58));
        sl.push(1282, new RSSI(-62));
        sl.push(1282, new RSSI(-68));
        sl.push(1282, new RSSI(-68));
        sl.push(1294, new RSSI(-100));

        final SampleList<RSSI> values = sl.filter(valid).filter(strong).toView();
        final Mean<RSSI> mean = new Mean<>();
        final Mode<RSSI> mode = new Mode<>();
        final Variance<RSSI> variance = new Variance<>();
        final Median<RSSI> median = new Median<>();
        final Gaussian<RSSI> gaussian = new Gaussian<>();

        // values = -60, -62, -68, -68
        final Summary<RSSI> summary = values.aggregate(mean, mode, variance, median, gaussian);
        assertEquals(summary.get(Mean.class), -64.5, Double.MIN_VALUE);
        assertEquals(summary.get(Mode.class), -68, Double.MIN_VALUE);
        assertEquals(summary.get(Variance.class), 17, Double.MIN_VALUE);
        assertEquals(summary.get(Median.class), -65, Double.MIN_VALUE);
        assertEquals(summary.get(Gaussian.class), -64.5, Double.MIN_VALUE);
        assertEquals(gaussian.model().variance(), 17, 0.00000001);
    }

    @Test
    public void ranges_filter_multi_since_summarise() {
        final Filter<RSSI> valid = new InRange<>(-99, -10);
        final Filter<RSSI> strong = new LessThan<>(-59);
        final Filter<RSSI> afterPoint = new Since<>(1245);
        final SampleList<RSSI> sl = new SampleList<>(20);
        sl.push(1234, new RSSI(-9));
        sl.push(1244, new RSSI(-60));
        sl.push(1265, new RSSI(-58));
        sl.push(1282, new RSSI(-62));
        sl.push(1282, new RSSI(-68));
        sl.push(1282, new RSSI(-68));
        sl.push(1294, new RSSI(-100));

        final SampleList<RSSI> values = sl.filter(afterPoint).filter(valid).filter(strong).toView();
        final Mean<RSSI> mean = new Mean<>();
        final Mode<RSSI> mode = new Mode<>();
        final Variance<RSSI> variance = new Variance<>();
        final Median<RSSI> median = new Median<>();
        final Gaussian<RSSI> gaussian = new Gaussian<>();

        // values = -62, -68, -68
        final Summary<RSSI> summary = values.aggregate(mean, mode, variance, median, gaussian);
        assertEquals(summary.get(Mean.class), -66, Double.MIN_VALUE);
        assertEquals(summary.get(Mode.class), -68, Double.MIN_VALUE);
        assertEquals(summary.get(Variance.class), 12, Double.MIN_VALUE);
        assertEquals(summary.get(Median.class), -68, Double.MIN_VALUE);
        assertEquals(summary.get(Gaussian.class), -66, Double.MIN_VALUE);
        assertEquals(gaussian.model().variance(), 12, 0.00000001);
    }

    @Test
    public void ranges_distance_aggregate() {
        final Filter<RSSI> valid = new InRange<>(-99, -10);
        final Filter<RSSI> strong = new LessThan<>(-59);
        final Filter<RSSI> afterPoint = new Since<>(1245);
        final SampleList<RSSI> sl = new SampleList<>(20);
        sl.push(1234, new RSSI(-9));
        sl.push(1244, new RSSI(-60));
        sl.push(1265, new RSSI(-58));
        sl.push(1282, new RSSI(-62));
        sl.push(1282, new RSSI(-68));
        sl.push(1282, new RSSI(-68));
        sl.push(1294, new RSSI(-100));

        final SampleList<RSSI> values = sl.filter(afterPoint).filter(valid).filter(strong).toView();
        final Mean<RSSI> mean = new Mean<>();
        final Mode<RSSI> mode = new Mode<>();
        final Variance<RSSI> variance = new Variance<>();
        final Median<RSSI> median = new Median<>();
        final Gaussian<RSSI> gaussian = new Gaussian<>();

        // values = -62, -68, -68
        final Summary<RSSI> summary = values.aggregate(mean, mode, variance, median, gaussian);
        assertEquals(summary.get(Mean.class), -66, Double.MIN_VALUE);
        assertEquals(summary.get(Mode.class), -68, Double.MIN_VALUE);
        assertEquals(summary.get(Variance.class), 12, Double.MIN_VALUE);
        assertEquals(summary.get(Median.class), -68, Double.MIN_VALUE);
        assertEquals(summary.get(Gaussian.class), -66, Double.MIN_VALUE);
        assertEquals(gaussian.model().variance(), 12, 0.00000001);
        final double modeValue = summary.get(Mode.class);
        final double sd = Math.sqrt(variance.reduce());

        // See second diagram at https://heraldprox.io/bluetooth/distance
        // i.e. https://heraldprox.io/images/distance-rssi-regression.png
        final FowlerBasic<RSSI> toDistance = new FowlerBasic<>(-50, -24);

        final Summary<RSSI> distance = sl.filter(afterPoint).filter(valid).filter(strong).filter(new InRange(modeValue - 2*sd, modeValue + 2*sd)).aggregate(toDistance);
        assertEquals(distance.get(FowlerBasic.class), 5.6235, 0.0005);
    }

    @Test
    public void ranges_risk_aggregate() {
        // First we simulate a list of actual distance samples over time, using a vector of pairs
        final List<Sample<Distance>> sourceDistances = new ArrayList<>();
        sourceDistances.add(new Sample<>(1235, new Distance(5.5)));
        sourceDistances.add(new Sample<>(1240, new Distance(4.7)));
        sourceDistances.add(new Sample<>(1245, new Distance(3.9)));
        sourceDistances.add(new Sample<>(1250, new Distance(3.2)));
        sourceDistances.add(new Sample<>(1255, new Distance(2.2)));
        sourceDistances.add(new Sample<>(1260, new Distance(1.9)));
        sourceDistances.add(new Sample<>(1265, new Distance(1.0)));
        sourceDistances.add(new Sample<>(1270, new Distance(1.3)));
        sourceDistances.add(new Sample<>(1275, new Distance(2.0)));
        sourceDistances.add(new Sample<>(1280, new Distance(2.2)));

        // The below would be in your aggregate handling code...
        final SampleList<Distance> distanceList = new SampleList<>(2);

        // For n distances we maintain n-1 distance-risks in a list, and continuously add to it
        // (i.e. we don't recalculate risk over all previous time - too much data)
        // Instead we keep a distance-time number for this known 'contact' which lasts up to 15 minutes.
        // (i.e. when the mac address changes in Bluetooth)
        // We would then store that single risk-time number against that single contact ID - much less data!
        final double timeScale = 1.0; // default is 1 second
        final double distanceScale = 1.0; // default is 1 metre, not scaled
        final double minimumDistanceClamp = 1.0; // As per Oxford Risk Model, anything < 1m ...
        final double minimumRiskScoreAtClamp = 1.0; // ...equals a risk of 1.0, ...
        // double logScale = 1.0; // ... and falls logarithmically after that
        // NOTE: The above values are pick for testing and may not be epidemiologically accurate!
        final RiskAggregationBasic riskScorer = new RiskAggregationBasic(timeScale,distanceScale,minimumDistanceClamp,minimumRiskScoreAtClamp);

        // Now generate a sequence of Risk Scores over time
        double interScore = 0.0;
        double firstNonZeroInterScore = 0.0;
        for (final Sample<Distance> sourceDistance : sourceDistances) {
            // A new distance has been calculated!
            distanceList.push(sourceDistance.taken(), sourceDistance.value());
            // Let's see if we have a new risk score!
            final Summary<Distance> riskSlice = distanceList.aggregate(riskScorer);
            // Add to our exposure risk for THIS contact
            // Note: We're NOT resetting over time, as the riskScorer will hold our total risk exposure from us.
            //       We could instead extract this slice, store it in a counter, and reset the risk Scorer if
            //       we needed to alter the value somehow or add the risk slices themselves to a new list.
            //       Instead, we only do this for each contact in total (i.e. up to 15 minutes per riskScorer).
            interScore = riskSlice.get(RiskAggregationBasic.class);
            if (0.0 == firstNonZeroInterScore && interScore > 0) {
                firstNonZeroInterScore = interScore;
            }
            System.err.println("RiskAggregationBasic inter score: " + interScore);
        }

        // Now we have the total for our 'whole contact duration', not scaled for how far in the past it is
        final double riskScore = riskScorer.reduce();
        System.err.println("RiskAggregationBasic final score: " + interScore);
        assertTrue(interScore > 0.0); // final inter score should be non zero
        assertTrue(riskScore > 0.0); // final score should be non zero
        assertTrue(riskScore > firstNonZeroInterScore); // should be additive over time too
    }

    /**
     * TODO Given a list of risk-distance numbers, and the approximate final time of that contact, calculate
     * a risk score when the risk of infection drops off linearly over 14 days. (like COVID-19)
     * (Ideally we'd have a more robust epidemiological model, but this will suffice for example purposes)
     */
}
