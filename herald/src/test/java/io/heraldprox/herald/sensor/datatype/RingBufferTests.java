//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import io.heraldprox.herald.sensor.datatype.random.RingBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RingBufferTests {

    @Test
    public void testEmpty() throws Exception {
        final RingBuffer ringBuffer = new RingBuffer(3);
        assertEquals(0, ringBuffer.size());
        assertEquals(0, ringBuffer.get().value.length);
        assertEquals(0, ringBuffer.get(0));
        assertNull(ringBuffer.hash());
    }

    @Test
    public void testRing() throws Exception {
        final RingBuffer ringBuffer = new RingBuffer(2);
        // Buffer = 1
        ringBuffer.push((byte) 1);
        assertEquals(1, ringBuffer.size());
        assertEquals(1, ringBuffer.get().value.length);
        assertEquals(1, ringBuffer.get().value[0]);
        assertEquals(1, ringBuffer.get(0));
        final Data hash1 = ringBuffer.hash();
        assertNotNull(hash1);
        // Buffer = 1,2
        ringBuffer.push((byte) 2);
        assertEquals(2, ringBuffer.size());
        assertEquals(2, ringBuffer.get().value.length);
        assertEquals(1, ringBuffer.get().value[0]);
        assertEquals(2, ringBuffer.get().value[1]);
        assertEquals(1, ringBuffer.get(0));
        assertEquals(2, ringBuffer.get(1));
        final Data hash2 = ringBuffer.hash();
        assertNotNull(hash2);
        assertFalse(hash1.equals(hash2));
        // Buffer = 2,3
        ringBuffer.push((byte) 3);
        assertEquals(2, ringBuffer.size());
        assertEquals(2, ringBuffer.get().value.length);
        assertEquals(2, ringBuffer.get().value[0]);
        assertEquals(3, ringBuffer.get().value[1]);
        assertEquals(2, ringBuffer.get(0));
        assertEquals(3, ringBuffer.get(1));
        final Data hash3 = ringBuffer.hash();
        assertNotNull(hash3);
        assertFalse(hash2.equals(hash3));
        // Buffer = 3,4
        ringBuffer.push((byte) 4);
        assertEquals(2, ringBuffer.size());
        assertEquals(2, ringBuffer.get().value.length);
        assertEquals(3, ringBuffer.get().value[0]);
        assertEquals(4, ringBuffer.get().value[1]);
        assertEquals(3, ringBuffer.get(0));
        assertEquals(4, ringBuffer.get(1));
        final Data hash4 = ringBuffer.hash();
        assertNotNull(hash4);
        assertFalse(hash3.equals(hash4));
        // Buffer = 4,5
        ringBuffer.push((byte) 5);
        assertEquals(2, ringBuffer.size());
        assertEquals(2, ringBuffer.get().value.length);
        assertEquals(4, ringBuffer.get().value[0]);
        assertEquals(5, ringBuffer.get().value[1]);
        assertEquals(4, ringBuffer.get(0));
        assertEquals(5, ringBuffer.get(1));
        final Data hash5 = ringBuffer.hash();
        assertNotNull(hash5);
        assertFalse(hash4.equals(hash5));
        // Buffer = empty
        ringBuffer.clear();
        assertEquals(0, ringBuffer.size());
        assertEquals(0, ringBuffer.get().value.length);
        assertEquals(0, ringBuffer.get(0));
        assertNull(ringBuffer.hash());
    }

    @Test
    public void testPushLong() throws Exception {
        final RingBuffer ringBuffer = new RingBuffer(8);
        // Buffer = Int64(1234567890123456789l)
        ringBuffer.push((long) 1234567890123456789l);
        final Data value1 = ringBuffer.get();
        assertEquals(1234567890123456789l, value1.int64(0).value);
        // Buffer = Int64(987654321098765432l)
        ringBuffer.push((long) 1987654321098765432l);
        final Data value2 = ringBuffer.get();
        assertEquals(1987654321098765432l, value2.int64(0).value);
    }

    @Test
    public void testPushData() throws Exception {
        final RingBuffer ringBuffer = new RingBuffer(8);
        final Data value1 = new Data();
        value1.append(new Int64(1234567890123456789l));
        // Buffer = Int64(1234567890123456789l)
        ringBuffer.push(value1);
        assertEquals(1234567890123456789l, ringBuffer.get().int64(0).value);
        // Buffer = Int64(987654321098765432l)
        final Data value2 = new Data();
        value2.append(new Int64(1987654321098765432l));
        ringBuffer.push(value2);
        assertEquals(1987654321098765432l, ringBuffer.get().int64(0).value);
    }
}