//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.RSSI;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "unchecked"})
public class SampleListTests {

    @Test
    public void sample_basic() {
        final Sample<RSSI> s = new Sample<>(new Date(1234), new RSSI(-55));
        assertEquals(s.taken().secondsSinceUnixEpoch(), 1234);
        assertEquals(s.value().value, -55, Double.MIN_VALUE);
    }

    @Test
    public void sample_from_parts() {
        final Sample<RSSI> s = new Sample<>(new Date(1234), new RSSI(-55));
        final Sample<RSSI> s2 = new Sample<>(s.taken(), s.value());
        assertEquals(s2.taken().secondsSinceUnixEpoch(), 1234);
        assertEquals(s2.value().value, -55, Double.MIN_VALUE);
    }

    @Test
    public void sample_from_parts_deep() {
        final Sample<RSSI> s = new Sample<>(new Date(1234), new RSSI(-55));
        final Sample<RSSI> s2 = new Sample<>(new Date(s.taken().secondsSinceUnixEpoch()), s.value());
        assertEquals(s2.taken().secondsSinceUnixEpoch(), 1234);
        assertEquals(s2.value().value, -55, Double.MIN_VALUE);
    }

    @Test
    public void sample_copy_ctor() {
        final Sample<RSSI> s = new Sample<>(new Date(1234), new RSSI(-55));
        final Sample<RSSI> s2 = new Sample<>(s);
        assertEquals(s2.taken().secondsSinceUnixEpoch(), 1234);
        assertEquals(s2.value().value, -55, Double.MIN_VALUE);
    }

    @Test
    public void sample_copy_assign() {
        //noinspection UnnecessaryLocalVariable
        final Sample<RSSI> s = new Sample<>(new Date(1234), new RSSI(-55));
        //noinspection UnnecessaryLocalVariable
        final Sample<RSSI> s2 = s;
        assertEquals(s2.taken().secondsSinceUnixEpoch(), 1234);
        assertEquals(s2.value().value, -55, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_empty() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        assertEquals(sl.size(), 0);
    }

    @Test
    public void samplelist_notfull() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        assertEquals(sl.size(), 3);
        assertEquals(sl.get(0).value().value, -55, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -60, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -58, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_exactlyfull() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        assertEquals(sl.size(), 5);
        assertEquals(sl.get(0).value().value, -55, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -60, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -58, Double.MIN_VALUE);
        assertEquals(sl.get(3).value().value, -61, Double.MIN_VALUE);
        assertEquals(sl.get(4).value().value, -54, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_oneover() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        assertEquals(sl.size(), 5);
        assertEquals(sl.get(0).value().value, -60, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -58, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -61, Double.MIN_VALUE);
        assertEquals(sl.get(3).value().value, -54, Double.MIN_VALUE);
        assertEquals(sl.get(4).value().value, -47, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_threeover() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        assertEquals(sl.size(), 5);
        assertEquals(sl.get(0).value().value, -61, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -54, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -47, Double.MIN_VALUE);
        assertEquals(sl.get(3).value().value, -48, Double.MIN_VALUE);
        assertEquals(sl.get(4).value().value, -49, Double.MIN_VALUE);
    }


    @Test
    public void samplelist_justunderfullagain() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        assertEquals(sl.size(), 5);
        assertEquals(sl.get(0).value().value, -54, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -47, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -48, Double.MIN_VALUE);
        assertEquals(sl.get(3).value().value, -49, Double.MIN_VALUE);
        assertEquals(sl.get(4).value().value, -45, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_fullagain() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        assertEquals(sl.size(), 5);
        assertEquals(sl.get(0).value().value, -47, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -48, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -49, Double.MIN_VALUE);
        assertEquals(sl.get(3).value().value, -45, Double.MIN_VALUE);
        assertEquals(sl.get(4).value().value, -44, Double.MIN_VALUE);
    }

    // MARK: - Now handle deletion by time

    @Test
    public void samplelist_clearoneold() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        sl.clearBeforeDate(new Date(1304));
        assertEquals(sl.size(), 4);
        assertEquals(sl.get(0).value().value, -48, Double.MIN_VALUE);
        assertEquals(sl.get(1).value().value, -49, Double.MIN_VALUE);
        assertEquals(sl.get(2).value().value, -45, Double.MIN_VALUE);
        assertEquals(sl.get(3).value().value, -44, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_clearfourold() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        sl.clearBeforeDate(new Date(1307));
        assertEquals(sl.size(), 1);
        assertEquals(sl.get(0).value().value, -44, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_clearallold() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        sl.clearBeforeDate(new Date(1308));
        assertEquals(sl.size(), 0);
    }

    // MARK: - Now handle clear()

    @Test
    public void samplelist_clear() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        sl.clear();
        assertEquals(sl.size(), 0);
    }

    // MARK: - Now handle iterators

    @Test
    public void samplelist_iterator_empty() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        assertFalse(sl.iterator().hasNext());
    }

    @Test
    public void samplelist_iterator_single() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -55, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_three() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -55, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -60, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -58, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_exactlyfull() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -55, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -60, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -58, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -61, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -54, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_oneover() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -60, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -58, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -61, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -54, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -47, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_twoover() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -58, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -61, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -54, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -47, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -48, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_threeover() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -61, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -54, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -47, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -48, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -49, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }


    @Test
    public void samplelist_iterator_justunderfullagain() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -54, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -47, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -48, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -49, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -45, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_fullagain() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertTrue(iter.hasNext());
        assertEquals(iter.next().value().value, -47, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -48, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -49, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -45, Double.MIN_VALUE);
        assertEquals(iter.next().value().value, -44, Double.MIN_VALUE);
        assertFalse(iter.hasNext());
    }

    @Test
    public void samplelist_iterator_cleared() {
        final SampleList<RSSI> sl = new SampleList<>(5);
        sl.push(new Date(1234), new RSSI(-55));
        sl.push(new Date(1244), new RSSI(-60));
        sl.push(new Date(1265), new RSSI(-58));
        sl.push(new Date(1282), new RSSI(-61));
        sl.push(new Date(1294), new RSSI(-54));
        sl.push(new Date(1302), new RSSI(-47));
        sl.push(new Date(1304), new RSSI(-48));
        sl.push(new Date(1305), new RSSI(-49));
        sl.push(new Date(1306), new RSSI(-45));
        sl.push(new Date(1307), new RSSI(-44));
        sl.clear();
        final Iterator<Sample<RSSI>> iter = sl.iterator();
        assertFalse(iter.hasNext());
    }

    // MARK: - Now handle other container functionality required

    @Test
    public void sample_init() {
        final Sample<RSSI> sample = new Sample<>(10, new RSSI(-55));
        assertEquals(sample.taken().secondsSinceUnixEpoch(), 10);
        assertEquals(sample.value().value, -55, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_init_list() {
        final Sample<RSSI> sample1 = new Sample<>(10, new RSSI(-55));
        final Sample<RSSI> sample2 = new Sample<>(20, new RSSI(-65));
        final Sample<RSSI> sample3 = new Sample<>(30, new RSSI(-75));
        final SampleList<RSSI> sl = new SampleList<>(3, sample1, sample2, sample3);
        assertEquals(sl.get(0).taken().secondsSinceUnixEpoch(), 10);
        assertEquals(sl.get(0).value().value, -55, Double.MIN_VALUE);
        assertEquals(sl.get(1).taken().secondsSinceUnixEpoch(), 20);
        assertEquals(sl.get(1).value().value, -65, Double.MIN_VALUE);
        assertEquals(sl.get(2).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(sl.get(2).value().value, -75, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_init_list_deduced() {
        final Sample<RSSI> sample1 = new Sample<>(10, new RSSI(-55));
        final Sample<RSSI> sample2 = new Sample<>(20, new RSSI(-65));
        final Sample<RSSI> sample3 = new Sample<>(30, new RSSI(-75));
        final SampleList<RSSI> sl = new SampleList<>(sample1, sample2, sample3);
        assertEquals(sl.get(0).taken().secondsSinceUnixEpoch(), 10);
        assertEquals(sl.get(0).value().value, -55, Double.MIN_VALUE);
        assertEquals(sl.get(1).taken().secondsSinceUnixEpoch(), 20);
        assertEquals(sl.get(1).value().value, -65, Double.MIN_VALUE);
        assertEquals(sl.get(2).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(sl.get(2).value().value, -75, Double.MIN_VALUE);
    }

    @Test
    public void samplelist_init_list_alldeduced() {
        final SampleList<RSSI> sl = new SampleList<>(
                new Sample<>(10, new RSSI(-55)),
                new Sample<>(20, new RSSI(-65)),
                new Sample<>(30, new RSSI(-75)));
        assertEquals(sl.get(0).taken().secondsSinceUnixEpoch(), 10);
        assertEquals(sl.get(0).value().value, -55, Double.MIN_VALUE);
        assertEquals(sl.get(1).taken().secondsSinceUnixEpoch(), 20);
        assertEquals(sl.get(1).value().value, -65, Double.MIN_VALUE);
        assertEquals(sl.get(2).taken().secondsSinceUnixEpoch(), 30);
        assertEquals(sl.get(2).value().value, -75, Double.MIN_VALUE);
    }
}
