//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.analysis.algorithms.distance.FowlerBasicAnalyser;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.Distance;
import com.vmware.herald.sensor.datatype.RSSI;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AnalysisRunnerTests {

    /// [Who]   As a DCT app developer
    /// [What]  I want to link my live application data to an analysis runner easily
    /// [Value] So I don't have to write plumbing code for Herald itself
    ///
    /// [Who]   As a DCT app developer
    /// [What]  I want to periodically run analysis aggregates automatically
    /// [Value] So I don't miss any information, and have accurate, regular, samples
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
        final AnalysisDelegate<Distance> myDelegate = new DummyDistanceDelegate();

        final AnalysisDelegateManager<Distance> adm = new AnalysisDelegateManager<>(myDelegate);
        final AnalysisProviderManager<RSSI, Distance> apm = new AnalysisProviderManager<>(distanceAnalyser);

        final AnalysisRunner<RSSI, Distance> runner = new AnalysisRunner<>(apm, adm, 25);

        // run at different times and ensure that it only actually runs three times (sample size == 3)
        src.run(20, runner);
        src.run(40, runner); // Runs here, because we have data for 10,20,>>30<<,40 <- next run time based on this 'latest' data time
        src.run(60, runner);
        src.run(80, runner); // Runs here because we have extra data for 50,60,>>70<<,80 <- next run time based on this 'latest' data time
        src.run(95, runner);


        assertEquals(((DummyDistanceDelegate) myDelegate).lastSampledID.value, 1234);
        final SampleList<Distance> samples = myDelegate.samples();
        // didn't reach 4x30 seconds, so no tenth sample, and didn't run at 60 because previous run was at time 40
        assertEquals(samples.size(), 2);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 40);
        assertTrue(samples.get(0).value().value != 0.0);
        assertEquals(samples.get(1).taken().secondsSinceUnixEpoch(), 80);
        assertTrue(samples.get(1).value().value != 0.0);
        System.err.println(samples);
    }

    private final static class DummyRSSISource {
        private final SampledID sampledID;
        private final SampleList<RSSI> data;

        public DummyRSSISource(final SampledID sampledID, final SampleList<RSSI> data) {
            this.sampledID = sampledID;
            this.data = data;
        }

        public void run(final long timeTo, final AnalysisRunner<RSSI, Distance> runner) {
            for (final Sample<RSSI> v : data) {
                if (v.taken().secondsSinceUnixEpoch() <= timeTo) {
                    runner.newSample(sampledID, v);
                }
            }
            runner.run(new Date(timeTo));
        }
    }

    private final static class DummyDistanceDelegate implements AnalysisDelegate<Distance> {
        private SampledID lastSampledID = new SampledID(0);
        private SampleList<Distance> distances = new SampleList<>(25);

        @Override
        public void newSample(SampledID sampled, Sample<Distance> item) {
            this.lastSampledID = sampled;
            distances.push(item);
        }

        @Override
        public void reset() {
            distances.clear();
            lastSampledID = new SampledID(0);
        }

        @Override
        public SampleList<Distance> samples() {
            return distances;
        }

        public SampledID lastSampledID() {
            return lastSampledID;
        }
    }
}
