//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.MessageDigest;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Ring buffer for gathering (entropy) data indefinitely, discarding the oldest data to limit
 * memory usage.
 */
public class RingBuffer {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.RingBuffer");
    @NonNull
    private final byte[] data;
    private int oldestPosition, newestPosition;

    public RingBuffer(final int bytes) {
        this.data = new byte[bytes];
        this.oldestPosition = bytes;
        this.newestPosition = bytes;
    }

    /**
     * Append byte to end of ring buffer.
     * @param value
     */
    public synchronized void push(final byte value) {
        incrementNewest();
        data[newestPosition] = value;
    }

    /**
     * Append long value to end of ring buffer as 8 bytes.
     * @param value
     */
    public synchronized void push(final long value) {
        push((byte) (value & 0xFF));       // LSB
        push((byte) ((value >> 8) & 0xFF));
        push((byte) ((value >> 16) & 0xFF));
        push((byte) ((value >> 24) & 0xFF));
        push((byte) ((value >> 32) & 0xFF));
        push((byte) ((value >> 40) & 0xFF));
        push((byte) ((value >> 48) & 0xFF));
        push((byte) ((value >> 56)));      // MSB
    }

    /**
     * Append bytes to end of ring buffer.
     * @param data
     */
    public synchronized void push(@NonNull final Data data) {
        for (int i=0; i<data.value.length; i++) {
            push(data.value[i]);
        }
    }

    /**
     * Number of bytes in the ring buffer.
     * @return
     */
    public synchronized int size() {
        if (newestPosition == data.length) return 0;
        if (newestPosition >= oldestPosition) {
            // not overlapping the end
            return newestPosition - oldestPosition + 1;
        }
        // we've overlapped
        return (1 + newestPosition) + (data.length - oldestPosition);
    }

    /**
     * Get byte at index of ring buffer.
     * @param index
     * @return
     */
    public synchronized byte get(final int index) {
        if (index > size() - 1) {
            return 0;
        }
        if (newestPosition >= oldestPosition) {
            return data[index + oldestPosition];
        }
        if (index + oldestPosition >= data.length) {
            return data[index + oldestPosition - data.length];
        }
        return data[index + oldestPosition];
    }

    public synchronized byte pop() {
        if (0 == size()) {
            return 0;
        }
        final byte value = get(0);
        if (1 == size()) {
            clear();
        } else {
            oldestPosition++;
            if (data.length == oldestPosition) {
                oldestPosition = 0;
            }
        }
        return value;
    }

    @NonNull
    public synchronized Data pop(final int bytes) {
        final byte[] data = new byte[Math.min(bytes, size())];
        for (int i=0; i<data.length; i++) {
            data[i] = pop();
        }
        return new Data(data);
    }

    /**
     * Clear ring buffer data.
     */
    public synchronized void clear() {
        oldestPosition = data.length;
        newestPosition = data.length;
    }


    private synchronized void incrementNewest() {
        if (newestPosition == data.length) {
            newestPosition = 0;
            oldestPosition = 0;
        } else {
            if (newestPosition == (oldestPosition - 1)) {
                ++oldestPosition;
                if (oldestPosition == data.length) {
                    oldestPosition = 0;
                }
            }
            ++newestPosition;
        }
        if (newestPosition == data.length) {
            // just gone past the end of the container
            newestPosition = 0;
            if (0 == oldestPosition) {
                ++oldestPosition; // erases oldest if not already removed
            }
        }
    }

    /**
     * Get entire ring buffer as data.
     * @return
     */
    @NonNull
    public synchronized Data get() {
        final Data data = new Data(new byte[size()]);
        for (int i=data.value.length; i-->0;) {
            data.value[i] = get(i);
        }
        return data;
    }

    /**
     * Cryptographic hash (SHA256) of ring buffer data.
     * @return SHA256 hash, or null if buffer is empty or SHA256 is not available
     */
    @Nullable
    public synchronized Data hash() {
        if (0 == size()) {
            return null;
        }
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (int i=0, ie=size(); i<ie; i++) {
                sha.update(get(i));
            }
            final byte[] hash = sha.digest();
            return new Data(hash);
        } catch (Throwable e) {
            // This should never happen as every implementation of Java should
            // support MD5, SHA1, and SHA256 as a minimum.
            logger.fault("SHA-256 unavailable", e);
            return null;
        }
    }

}
