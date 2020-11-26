//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.c19x;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;

public class ConcreteDayCodes implements DayCodes {
	private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.ConcreteDayCodes");
	private final long epoch = epoch();
	private final DayCode[] values;

	public ConcreteDayCodes(final SharedSecret sharedSecret) {
		final int days = 365 * 5;
		values = dayCodes(sharedSecret, days);
	}

	/// Get epoch timestamp
	private long epoch() {
		try {
			final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			simpleDateFormat.setLenient(false);
			return simpleDateFormat.parse("2020-01-01 00:00").getTime();
		} catch (Throwable e) {
			logger.fault("Failed to get epoch", e);
			return 0;
		}
	}

	/// Generate forward secure day codes from shared secret for a number of days.
	private DayCode[] dayCodes(final SharedSecret sharedSecret, final int days) {
		final DayCode[] codes = new DayCode[days];
		try {
			final MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] hash = sha.digest(sharedSecret.value);
			for (int i = codes.length; i-- > 0; ) {
				codes[i] = new DayCode(JavaData.byteArrayToLong(hash));
				sha.reset();
				hash = sha.digest(hash);
			}
		} catch (Throwable e) {
			logger.fault("Failed to get day odes", e);
		}
		return codes;
	}

	/// Generate forward secure beacon code seed from day code.
	private BeaconCodeSeed beaconCodeSeed(final DayCode dayCode) {
		final ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
		byteBuffer.putLong(0, dayCode.value);
		// Reverse bytes
		final byte[] data = byteBuffer.array();
		final byte[] reversed = new byte[]{data[7], data[6], data[5], data[4], data[3], data[2], data[1], data[0]};
		// Hash of reversed
		try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha.digest(reversed);
            final long seed = JavaData.byteArrayToLong(hash);
            return new BeaconCodeSeed(seed);
        } catch (Throwable e) {
			logger.fault("Failed to transform day code to beacon code seed", e);
			return null;
		}
	}

	/// Get epoch day for timestamp (for selecting day code).
	private Day day(final Timestamp timestamp) {
		final Day day = new Day((int) ((timestamp.value.getTime() - epoch) / (24 * 60 * 60 * 1000)));
		if (day.value < 0 || day.value >= values.length) {
			logger.fault("Day out of range");
			return null;
		}
		return day;
	}

    @Override
    public BeaconCodeSeed seed(final Timestamp timestamp) {
        final Day day = day(timestamp);
        if (day == null) {
			logger.fault("Day out of range");
			return null;
		}
        try {
            final DayCode dayCode = values[day.value];
            final BeaconCodeSeed beaconCodeSeed = beaconCodeSeed(dayCode);
            beaconCodeSeed.day = day;
            return beaconCodeSeed;
        } catch (Throwable e) {
            logger.fault("Day out of range", e);
            return null;
        }
    }
}
