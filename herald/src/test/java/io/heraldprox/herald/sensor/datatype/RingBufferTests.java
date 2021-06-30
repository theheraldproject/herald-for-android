//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import io.heraldprox.herald.sensor.datatype.random.RingBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class RingBufferTests {

    @Test
    public void testEmpty() {
        final RingBuffer ringBuffer = new RingBuffer(3);
        assertEquals(0, ringBuffer.size());
        assertEquals(0, ringBuffer.get().value.length);
        assertEquals(0, ringBuffer.get(0));
        assertEquals(0, ringBuffer.pop());
        assertEquals(0, ringBuffer.size());
        assertNull(ringBuffer.hash());
    }

    @Test
    public void testPushPop() {
        final RingBuffer ringBuffer = new RingBuffer(2);
        // Buffer = 1
        ringBuffer.push((byte) 1);
        assertEquals(1, ringBuffer.get(0));
        // Buffer = empty
        assertEquals(1, ringBuffer.pop());
        assertEquals(0, ringBuffer.size());

        // Buffer = 1,2
        ringBuffer.push((byte) 1);
        ringBuffer.push((byte) 2);
        assertEquals(1, ringBuffer.get(0));
        assertEquals(2, ringBuffer.get(1));
        // Buffer = empty
        assertEquals(1, ringBuffer.pop());
        assertEquals(2, ringBuffer.pop());
        assertEquals(0, ringBuffer.size());

        // Buffer = [1,]2,3
        ringBuffer.push((byte) 1);
        ringBuffer.push((byte) 2);
        ringBuffer.push((byte) 3);
        assertEquals(2, ringBuffer.get(0));
        assertEquals(3, ringBuffer.get(1));
        // Buffer = empty
        assertEquals(2, ringBuffer.pop());
        assertEquals(3, ringBuffer.pop());
        assertEquals(0, ringBuffer.size());
    }

    @Test
    public void testPop() {
        final RingBuffer ringBuffer = new RingBuffer(4);
        // Buffer = 1,2,3,4
        ringBuffer.push((byte) 1);
        ringBuffer.push((byte) 2);
        ringBuffer.push((byte) 3);
        ringBuffer.push((byte) 4);
        assertArrayEquals(new byte[]{1}, ringBuffer.pop(1).value);
        assertArrayEquals(new byte[]{2,3}, ringBuffer.pop(2).value);
        assertArrayEquals(new byte[]{4}, ringBuffer.pop(3).value);
        assertEquals(0, ringBuffer.size());

        // Buffer = [1,]2,3,4,5
        ringBuffer.push((byte) 1);
        ringBuffer.push((byte) 2);
        ringBuffer.push((byte) 3);
        ringBuffer.push((byte) 4);
        ringBuffer.push((byte) 5);
        assertArrayEquals(new byte[]{2}, ringBuffer.pop(1).value);
        assertArrayEquals(new byte[]{3,4}, ringBuffer.pop(2).value);
        assertArrayEquals(new byte[]{5}, ringBuffer.pop(3).value);
        assertEquals(0, ringBuffer.size());
    }

    @Test
    public void testRing() {
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
        assertNotEquals(hash1, hash2);
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
        assertNotEquals(hash2, hash3);
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
        assertNotEquals(hash3, hash4);
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
        assertNotEquals(hash4, hash5);
        // Buffer = empty
        ringBuffer.clear();
        assertEquals(0, ringBuffer.size());
        assertEquals(0, ringBuffer.get().value.length);
        assertEquals(0, ringBuffer.get(0));
        assertNull(ringBuffer.hash());
    }

    @Test
    public void testPushLong() {
        final RingBuffer ringBuffer = new RingBuffer(8);
        // Buffer = Int64(1234567890123456789l)
        ringBuffer.push(1234567890123456789L);
        final Data value1 = ringBuffer.get();
        assertEquals(1234567890123456789L, value1.int64(0).value);
        // Buffer = Int64(987654321098765432l)
        ringBuffer.push(1987654321098765432L);
        final Data value2 = ringBuffer.get();
        assertEquals(1987654321098765432L, value2.int64(0).value);
    }

    @Test
    public void testPushData() {
        final RingBuffer ringBuffer = new RingBuffer(8);
        final Data value1 = new Data();
        value1.append(new Int64(1234567890123456789L));
        // Buffer = Int64(1234567890123456789l)
        ringBuffer.push(value1);
        assertEquals(1234567890123456789L, ringBuffer.get().int64(0).value);
        // Buffer = Int64(987654321098765432l)
        final Data value2 = new Data();
        value2.append(new Int64(1987654321098765432L));
        ringBuffer.push(value2);
        assertEquals(1987654321098765432L, ringBuffer.get().int64(0).value);
    }
}