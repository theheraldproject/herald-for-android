//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.pipeline;

import com.vmware.herald.sensor.analysis.algorithms.distance.FowlerBasicDataProcessor;
import com.vmware.herald.sensor.analysis.sampling.AnalysisRunnerTests;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampleList;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.datatype.Distance;
import com.vmware.herald.sensor.datatype.Int16;
import com.vmware.herald.sensor.datatype.Int8;
import com.vmware.herald.sensor.datatype.RSSI;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineRunnerTests {

    @Test
    public void typeDetection() {
        final PipelineManager pipelineRunner = new PipelineManager();
        final ConcreteDataConsumer<RSSI> rssiConsumer = new ConcreteDataConsumer<>(RSSI.class, 10);
        final ConcreteDataConsumer<Int8> int8Consumer = new ConcreteDataConsumer<>(Int8.class, 10);
        final ConcreteDataConsumer<Int16> int16Consumer = new ConcreteDataConsumer<>(Int16.class, 10);
        final FowlerBasicDataProcessor fowlerBasicDataProcessor = new FowlerBasicDataProcessor();
        pipelineRunner.add(rssiConsumer);
        pipelineRunner.add(int8Consumer);
        pipelineRunner.add(int16Consumer);
        pipelineRunner.add(fowlerBasicDataProcessor);
        pipelineRunner.newSample(new SampledID(1), new Sample<RSSI>(1, new RSSI(1)));
        pipelineRunner.newSample(new SampledID(1), new Sample<Int8>(2, new Int8(1)));
        pipelineRunner.newSample(new SampledID(1), new Sample<Int16>(3, new Int16(1)));
        assertEquals(rssiConsumer.sampleList(new SampledID(1)).size(), 1);
        assertEquals(int8Consumer.sampleList(new SampledID(1)).size(), 1);
        assertEquals(int16Consumer.sampleList(new SampledID(1)).size(), 1);
    }

    /// [Who]   As a DCT app developer
    /// [What]  I want to link my live application data to an analysis runner easily
    /// [Value] So I don't have to write plumbing code for Herald itself
    ///
    /// [Who]   As a DCT app developer
    /// [What]  I want to periodically run analysis aggregates automatically
    /// [Value] So I don't miss any information, and have accurate, regular, samples
    @Test
    public void analysisrunner_basic() {
        // Define pipeline PipelineManager -> FowlerBasicDataProcessor -> ConcreteDataConsumer
        final PipelineManager pipelineManager = new PipelineManager();
        final DataProcessor<RSSI, Distance> dataProcessor = new FowlerBasicDataProcessor(30, -50, -24);
        final DataConsumer<Distance> dataConsumer = new ConcreteDataConsumer<>(Distance.class, 25);
        pipelineManager.add(dataProcessor);
        dataProcessor.add(dataConsumer);

        // Feed data to pipeline, and let pipelineManager direct samples according to sample data type
        final SampledID sampledID = new SampledID(1234);
        pipelineManager.newSample(sampledID, new Sample<RSSI>(10, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(20, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(30, new RSSI(-55)));
        // Runs here, because we have data for 10,20,>>30<<,40 <- next run time based on this 'latest' data time
        pipelineManager.newSample(sampledID, new Sample<RSSI>(40, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(50, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(60, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(70, new RSSI(-55)));
        // Runs here because we have extra data for 50,60,>>70<<,80 <- next run time based on this 'latest' data time
        pipelineManager.newSample(sampledID, new Sample<RSSI>(80, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(90, new RSSI(-55)));
        pipelineManager.newSample(sampledID, new Sample<RSSI>(100, new RSSI(-55)));

        // Check output collected by myDelegate
        final ConcreteDataConsumer<Distance> myDelegate = (ConcreteDataConsumer<Distance>) dataConsumer;
        assertEquals(myDelegate.latestSampled().value, 1234);
        final SampleList<Distance> samples = myDelegate.sampleList(sampledID);
        // didn't reach 4x30 seconds, so no tenth sample, and didn't run at 60 because previous run was at time 40
        assertEquals(samples.size(), 2);
        assertEquals(samples.get(0).taken().secondsSinceUnixEpoch(), 40);
        assertTrue(samples.get(0).value().value != 0.0);
        assertEquals(samples.get(1).taken().secondsSinceUnixEpoch(), 80);
        assertTrue(samples.get(1).value().value != 0.0);
        System.err.println(samples);
    }

}
