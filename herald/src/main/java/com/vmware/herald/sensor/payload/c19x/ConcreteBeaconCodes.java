//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.c19x;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class ConcreteBeaconCodes implements BeaconCodes {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.ConcreteBeaconCodes");
    private final static String tag = ConcreteBeaconCodes.class.getName();
    private final static int codesPerDay = 240;
    private final DayCodes dayCodes;
    private BeaconCodeSeed beaconCodeSeed = null;
    private BeaconCode[] beaconCodes = null;

    public ConcreteBeaconCodes(final DayCodes dayCodes) {
        this.dayCodes = dayCodes;
    }

    @Override
    public BeaconCode get(final Timestamp timestamp) {
        final BeaconCodeSeed seed = dayCodes.seed(timestamp);
        if (seed == null) {
            logger.fault("No seed code available");
            return null;
        }
        if (beaconCodeSeed == null || seed.value != beaconCodeSeed.value) {
            beaconCodeSeed = seed;
            beaconCodes = beaconCodes(seed);
        }
        if (beaconCodes == null) {
            return null;
        }
        final long daySecond = timestamp.value.getTime() % (60*60*24);
        final long codeIndex = daySecond % beaconCodes.length;
        return beaconCodes[(int) codeIndex];
    }

    public static BeaconCode[] beaconCodes(final BeaconCodeSeed beaconCodeSeed) {
        return beaconCodes(beaconCodeSeed, codesPerDay);
    }

    private static BeaconCode[] beaconCodes(final BeaconCodeSeed beaconCodeSeed, final int count) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.putLong(0, beaconCodeSeed.value);
        final byte[] data = byteBuffer.array();
        final BeaconCode[] codes = new BeaconCode[count];
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(data);
            for (int i = codes.length; i-- > 0; ) {
                codes[i] = new BeaconCode(JavaData.byteArrayToLong(hash));
                sha.reset();
                hash = sha.digest(hash);
            }
        } catch (Throwable e) {
            // This will only happen if SHA-256 is unavailable
            // logger.fault("Failed to get codes", e);
        }
        return codes;
    }

}
