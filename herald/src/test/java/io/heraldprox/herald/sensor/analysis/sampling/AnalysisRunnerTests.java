//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.FowlerBasicAnalyser;
import io.heraldprox.herald.sensor.analysis.algorithms.distance.SmoothedLinearModel;
import io.heraldprox.herald.sensor.analysis.algorithms.distance.SmoothedLinearModelAnalyser;
import io.heraldprox.herald.sensor.analysis.algorithms.distance.SelfCalibratedModel;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.Int8;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "unchecked"})
public class AnalysisRunnerTests {

    @Test
    public void listmanager() {
        final ListManager<RSSI> listManager = new ListManager<>(10);
        final SampleList<RSSI> sampleList1 = listManager.list(new SampledID(1));
        final SampleList<RSSI> sampleList2 = listManager.list(new SampledID(2));
        final SampleList<RSSI> sampleList2b = listManager.list(new SampledID(2));
        assertEquals(listManager.sampledIDs().size(), 2);
        listManager.remove(new SampledID(1));
        assertEquals(listManager.sampledIDs().size(), 1);
        assertEquals(listManager.sampledIDs().iterator().next().value, 2);
        listManager.clear();
        assertEquals(listManager.sampledIDs().size(), 0);
    }

    @Test
    public void variantset_listmanager() {
        final VariantSet variantSet = new VariantSet(15);
        final ListManager<RSSI> listManagerRSSI = variantSet.listManager(RSSI.class);
        final ListManager<Int8> listManagerInt8 = variantSet.listManager(Int8.class);
        assertEquals(variantSet.size(), 2);
        assertEquals(listManagerRSSI.size(), 0);
        assertEquals(listManagerInt8.size(), 0);

        final SampleList<RSSI> sampleListRSSI = listManagerRSSI.list(new SampledID(1234));
        final SampleList<Int8> sampleListInt8 = listManagerInt8.list(new SampledID(5678));
        assertEquals(sampleListRSSI.size(), 0);
        assertEquals(sampleListInt8.size(), 0);

        sampleListRSSI.push(new Sample<>(0, new RSSI(12)));
        sampleListInt8.push(new Sample<>(10, new Int8(14)));
        variantSet.push(new SampledID(5678), new Sample<>(20, new Int8(15)));
        assertEquals(variantSet.listManager(RSSI.class, new SampledID(1234)).size(), 1);
        assertEquals(variantSet.listManager(Int8.class, new SampledID(5678)).size(), 2);

        variantSet.remove(new SampledID(1234));
        assertEquals(listManagerRSSI.size(), 0);
        assertEquals(listManagerInt8.size(), 1);

        variantSet.remove(RSSI.class);
        assertEquals(variantSet.size(), 1);
    }

    /**
     * [Who]   As a DCT app developer
     * [What]  I want to link my live application data to an analysis runner easily
     * [Value] So I don't have to write plumbing code for Herald itself
     *
     * [Who]   As a DCT app developer
     * [What]  I want to periodically run analysis aggregates automatically
     * [Value] So I don't miss any information, and have accurate, regular, samples
     */
    @Test
    public void analysisrunner_basic() {
        final SampleList<RSSI> srcData = new SampleList<>(25);
        srcData.push(10, new RSSI(-55));
        srcData.push(20, new RSSI(-55));
        srcData.push(30, new RSSI(-55));
        srcData.push(40, new RSSI(-55));
        srcData.push(50, new RSSI(-55));
        srcData.push(60, new RSSI(-55));
        srcData.push(70, new RSSI(-55));
        srcData.push(80, new RSSI(-55));
        srcData.push(90, new RSSI(-55));
        srcData.push(100, new RSSI(-55));
        final DummyRSSISource src = new DummyRSSISource(new SampledID(1234), srcData);

        final AnalysisProvider<RSSI, Distance> distanceAnalyser = new FowlerBasicAnalyser(30, -50, -24);
        final DummyDistanceDelegate myDelegate = new DummyDistanceDelegate();

        final AnalysisDelegateManager adm = new AnalysisDelegateManager(myDelegate);
        final AnalysisProviderManager apm = new AnalysisProviderManager(distanceAnalyser);

        final AnalysisRunner runner = new AnalysisRunner(apm, adm, 25);

        // run at different times
        src.run(20, runner);
        src.run(40, runner); // Runs here, because we have data for 10,20,>>30<<,40 <- next run time based on this 'latest' data time
        src.run(60, runner);
        src.run(80, runner); // Runs here because we have extra data for 50,60,>>70<<,80 <- next run time based on this 'latest' data time
        src.run(95, runner);


        assertEquals(myDelegate.lastSampledID.value, 1234);
        final SampleList<Distance> samples = myDelegate.samples();
        assertEquals(samples.size(), 2);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 40);
        assertTrue(samples.get(0).value().value != 0.0);
        assertEquals(samples.get(1).taken().secondsSinceUnixEpoch(), 80);
        assertTrue(samples.get(1).value().value != 0.0);
    }

    @Test
    public void analysisrunner_smoothedLinearModel() {
        final SampleList<RSSI> srcData = new SampleList<>(25);
        srcData.push(0, new RSSI(-68));
        srcData.push(10, new RSSI(-68));
        srcData.push(20, new RSSI(-68));
        srcData.push(30, new RSSI(-68));
        srcData.push(40, new RSSI(-68));
        srcData.push(50, new RSSI(-68));
        srcData.push(60, new RSSI(-68));
        final DummyRSSISource src = new DummyRSSISource(new SampledID(1234), srcData);

        final AnalysisProvider<RSSI, Distance> distanceAnalyser = new SmoothedLinearModelAnalyser(new TimeInterval(10), TimeInterval.minute, new SmoothedLinearModel<RSSI>(-17.7275, -0.2754));
        final DummyDistanceDelegate myDelegate = new DummyDistanceDelegate();

        final AnalysisDelegateManager adm = new AnalysisDelegateManager(myDelegate);
        final AnalysisProviderManager apm = new AnalysisProviderManager(distanceAnalyser);
        final AnalysisRunner runner = new AnalysisRunner(apm, adm, 25);

        // run at different times and ensure that it only actually runs once
        src.run(60, 10, runner);
        src.run(60, 20, runner);
        src.run(60, 30, runner);
        src.run(60, 40, runner);
        src.run(60, 50, runner);
        src.run(60, 60, runner); // Runs here, because we have data for 0,10,20,>>30<<,40,50,60 <- next run time based on this 'latest' data time


        assertEquals(myDelegate.lastSampledID.value, 1234);
        final SampleList<Distance> samples = myDelegate.samples();
        assertEquals(samples.size(), 1);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(samples.get(0).value().value, 1.0, 0.001);
    }

    @Test
    public void analysisrunner_smoothedLinearSelfCalibratedModel_uncalibrated() {
        final SampleList<RSSI> srcData = new SampleList<>(25);
        srcData.push(0, new RSSI(-55));
        srcData.push(10, new RSSI(-55));
        srcData.push(20, new RSSI(-55));
        srcData.push(30, new RSSI(-55));
        srcData.push(40, new RSSI(-55));
        srcData.push(50, new RSSI(-55));
        srcData.push(60, new RSSI(-55));
        final DummyRSSISource src = new DummyRSSISource(new SampledID(1234), srcData);

        final SelfCalibratedModel<RSSI> smoothedLinearModel = new SelfCalibratedModel<>(
                new Distance(0.2), new Distance(1),
                TimeInterval.zero, TimeInterval.hours(12), null);
        final AnalysisProvider<RSSI, Distance> distanceAnalyser = new SmoothedLinearModelAnalyser(new TimeInterval(10), TimeInterval.minute, smoothedLinearModel);
        final DummyDistanceDelegate myDelegate = new DummyDistanceDelegate();

        final AnalysisDelegateManager adm = new AnalysisDelegateManager(myDelegate);
        final AnalysisProviderManager apm = new AnalysisProviderManager(distanceAnalyser);
        final AnalysisRunner runner = new AnalysisRunner(apm, adm, 25);

        // run at different times and ensure that it only actually runs once
        src.run(60, 10, runner);
        src.run(60, 20, runner);
        src.run(60, 30, runner);
        src.run(60, 40, runner);
        src.run(60, 50, runner);
        src.run(60, 60, runner); // Runs here, because we have data for 0,10,20,>>30<<,40,50,60 <- next run time based on this 'latest' data time


        assertEquals(myDelegate.lastSampledID.value, 1234);
        final SampleList<Distance> samples = myDelegate.samples();
        assertEquals(samples.size(), 1);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(samples.get(0).value().value, 0.2, 0.001);
    }

    @Test
    public void analysisrunner_smoothedLinearSelfCalibratedModel_lower_range() {
        final SampleList<RSSI> srcData = new SampleList<>(25);
        srcData.push(0, new RSSI(-72));
        srcData.push(10, new RSSI(-72));
        srcData.push(20, new RSSI(-72));
        srcData.push(30, new RSSI(-72));
        srcData.push(40, new RSSI(-72));
        srcData.push(50, new RSSI(-72));
        srcData.push(60, new RSSI(-72));
        final DummyRSSISource src = new DummyRSSISource(new SampledID(1234), srcData);

        final SelfCalibratedModel<RSSI> smoothedLinearModel = new SelfCalibratedModel<>(
                new Distance(0.2), new Distance(1),
                TimeInterval.zero, TimeInterval.hours(12), null);
        final AnalysisProvider<RSSI, Distance> distanceAnalyser = new SmoothedLinearModelAnalyser(new TimeInterval(10), TimeInterval.minute, smoothedLinearModel);
        final DummyDistanceDelegate myDelegate = new DummyDistanceDelegate();

        final AnalysisDelegateManager adm = new AnalysisDelegateManager(myDelegate);
        final AnalysisProviderManager apm = new AnalysisProviderManager(distanceAnalyser);
        final AnalysisRunner runner = new AnalysisRunner(apm, adm, 25);

        // Self-calibration for lower range
        for (int rssi=-98; rssi<=-45; rssi++) {
            smoothedLinearModel.histogram.add(rssi);
        }
        smoothedLinearModel.update();

        // run at different times and ensure that it only actually runs once
        src.run(60, 10, runner);
        src.run(60, 20, runner);
        src.run(60, 30, runner);
        src.run(60, 40, runner);
        src.run(60, 50, runner);
        src.run(60, 60, runner); // Runs here, because we have data for 0,10,20,>>30<<,40,50,60 <- next run time based on this 'latest' data time


        assertEquals(myDelegate.lastSampledID.value, 1234);
        final SampleList<Distance> samples = myDelegate.samples();
        assertEquals(samples.size(), 1);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(samples.get(0).value().value, 1.0, 0.001);
    }


    @Test
    public void analysisrunner_smoothedLinearSelfCalibratedModel_upper_range() {
        final SampleList<RSSI> srcData = new SampleList<>(25);
        srcData.push(0, new RSSI(-27));
        srcData.push(10, new RSSI(-27));
        srcData.push(20, new RSSI(-27));
        srcData.push(30, new RSSI(-27));
        srcData.push(40, new RSSI(-27));
        srcData.push(50, new RSSI(-27));
        srcData.push(60, new RSSI(-27));
        final DummyRSSISource src = new DummyRSSISource(new SampledID(1234), srcData);

        final SelfCalibratedModel<RSSI> smoothedLinearModel = new SelfCalibratedModel<>(
                new Distance(0.2), new Distance(1),
                TimeInterval.zero, TimeInterval.hours(12), null);
        final AnalysisProvider<RSSI, Distance> distanceAnalyser = new SmoothedLinearModelAnalyser(new TimeInterval(10), TimeInterval.minute, smoothedLinearModel);
        final DummyDistanceDelegate myDelegate = new DummyDistanceDelegate();

        final AnalysisDelegateManager adm = new AnalysisDelegateManager(myDelegate);
        final AnalysisProviderManager apm = new AnalysisProviderManager(distanceAnalyser);
        final AnalysisRunner runner = new AnalysisRunner(apm, adm, 25);

        // Self-calibration for upper range
        for (int rssi=-44; rssi<=-10; rssi++) {
            smoothedLinearModel.histogram.add(rssi);
        }
        smoothedLinearModel.update();

        // run at different times and ensure that it only actually runs once
        src.run(60, 10, runner);
        src.run(60, 20, runner);
        src.run(60, 30, runner);
        src.run(60, 40, runner);
        src.run(60, 50, runner);
        src.run(60, 60, runner); // Runs here, because we have data for 0,10,20,>>30<<,40,50,60 <- next run time based on this 'latest' data time


        assertEquals(myDelegate.lastSampledID.value, 1234);
        final SampleList<Distance> samples = myDelegate.samples();
        assertEquals(samples.size(), 1);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(samples.get(0).value().value, 1.0, 0.001);
    }

    private final static class DummyRSSISource {
        private final SampledID sampledID;
        private final SampleList<RSSI> data;

        public DummyRSSISource(final SampledID sampledID, final SampleList<RSSI> data) {
            this.sampledID = sampledID;
            this.data = data;
        }

        public void run(final long timeTo, final AnalysisRunner runner) {
            runner.variantSet().clear();
            for (final Sample<RSSI> v : data) {
                if (v.taken().secondsSinceUnixEpoch() <= timeTo) {
                    runner.newSample(sampledID, v);
                }
            }
            runner.run(new Date(timeTo));
        }

        public void run(final long sampleTimeTo, final long analysisTimeTo, final AnalysisRunner runner) {
            runner.variantSet().clear();
            for (final Sample<RSSI> v : data) {
                if (v.taken().secondsSinceUnixEpoch() <= sampleTimeTo) {
                    runner.newSample(sampledID, v);
                }
            }
            runner.run(new Date(analysisTimeTo));
        }
    }

    private final static class DummyDistanceDelegate implements AnalysisDelegate<Distance> {
        private SampledID lastSampledID = new SampledID(0);
        private final SampleList<Distance> distances = new SampleList<>(25);

        @Override
        public void newSample(@NonNull final SampledID sampled, @NonNull final Sample<Distance> item) {
            this.lastSampledID = sampled;
            distances.push(item);
        }

        @NonNull
        @Override
        public Class<Distance> inputType() {
            return Distance.class;
        }

        @Override
        public void reset() {
            distances.clear();
            lastSampledID = new SampledID(0);
        }

        @Override
        public void removeSamplesFor(SampledID sampled) {
            // Do nothing - not relevant to the test
        }

        @NonNull
        @Override
        public SampleList<Distance> samples() {
            return distances;
        }

        public SampledID lastSampledID() {
            return lastSampledID;
        }
    }
}
