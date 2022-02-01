//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.io.File;

import io.heraldprox.herald.sensor.data.TextFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HistogramTests {

    @Test
    public void testAdd() {
        final Histogram histogram = new Histogram(-10, 10);
        for (int i=-20; i<20; i++) {
            histogram.add(i);
        }
        for (int i=-20; i<20; i++) {
            if (i < -10 || i > 10) {
                assertEquals(0, histogram.count(i));
            } else {
                assertEquals(1, histogram.count(i));
            }
        }
        assertEquals(21, histogram.count());
    }

    @Test
    public void testClear() {
        final Histogram histogram = new Histogram(-10, 10);
        for (int i=-20; i<20; i++) {
            histogram.add(i);
        }
        histogram.clear();
        for (int i=-20; i<20; i++) {
            assertEquals(0, histogram.count(i));

        }
        assertEquals(0, histogram.count());
    }

    @Test
    public void testReadWrite() {
        final File folder = new File("test_histogramTests");
        final File file = new File(folder, "testReadWrite");
        file.delete();
        folder.delete();
        final Histogram histogram = new Histogram(-10, 10);
        for (int i=-20; i<20; i++) {
            histogram.add(i);
        }
        final TextFile textFile = new TextFile(file);
        histogram.write(textFile);
        final Histogram histogramRead = new Histogram(-10, 10, TimeInterval.never, textFile);
        for (int i=-20; i<20; i++) {
            if (i < -10 || i > 10) {
                assertEquals(0, histogramRead.count(i));
            } else {
                assertEquals(1, histogramRead.count(i));
            }
        }
        assertEquals(21, histogramRead.count());
        file.delete();
        folder.delete();
    }

    @Test
    public void minMaxMode() {
        final Histogram histogram = new Histogram(0,100);
        assertNull(histogram.minValue());
        assertNull(histogram.maxValue());
        assertNull(histogram.mode());
        // 50 = 1
        histogram.add(50);
        assertNotNull(histogram.minValue());
        assertNotNull(histogram.maxValue());
        assertNotNull(histogram.mode());
        assertEquals(50, histogram.minValue().intValue());
        assertEquals(50, histogram.maxValue().intValue());
        assertEquals(50, histogram.mode().intValue());
        // 50 = 1, 60 = 2
        histogram.add(60, 2);
        assertNotNull(histogram.minValue());
        assertNotNull(histogram.maxValue());
        assertNotNull(histogram.mode());
        assertEquals(50, histogram.minValue().intValue());
        assertEquals(60, histogram.maxValue().intValue());
        assertEquals(60, histogram.mode().intValue());
    }
}
