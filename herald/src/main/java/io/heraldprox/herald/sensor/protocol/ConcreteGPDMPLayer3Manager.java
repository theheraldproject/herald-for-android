//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import androidx.annotation.NonNull;

import java.lang.annotation.Target;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt32;

public class ConcreteGPDMPLayer3Manager implements SensorDelegate, GPDMPLayer3Manager, GPDMPLayer3Incoming, GPDMPLayer3Outgoing {
    private ArrayList<Tuple<SensorType,TargetIdentifier>> potentialRoutes = new ArrayList<>();

    // TODO support multiple layer2 outgoing interfaces (E.g. one each for BLE, BLMesh, UWB, etc.)
    private GPDMPLayer2Outgoing outgoingInterface = null;
    private GPDMPLayer4Incoming incomingInterface = null;

    /// MARK: Herald Sensor Delegate functions

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull TargetIdentifier didDetect) {
        // Do nothing if just detected, until we know it's a Herald device
    }


    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        // Unused
    }

    private void addRoute(@NonNull SensorType sensor, @NonNull TargetIdentifier fromTarget) {
        boolean found = false;
        for (Tuple t: potentialRoutes) {
            found = found || (t.a == sensor && t.b == fromTarget);
        }
        if (!found) {
            potentialRoutes.add(new Tuple(sensor, fromTarget));
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, boolean available, @NonNull TargetIdentifier didDeleteOrDetect) {
        if (available) {
            // in case it reappears (E.g. one way detection)
            addRoute(sensor,didDeleteOrDetect);
        } else {
            // remove from potential routes
            Iterator routeIter = potentialRoutes.iterator();
            while (routeIter.hasNext()) {
                Tuple potentialRoute = (Tuple<SensorType,TargetIdentifier>)routeIter.next();
                // Same TI may be available across multiple sensor types (wire protocols)
                if (sensor == potentialRoute.a && didDeleteOrDetect == potentialRoute.b) {
                    routeIter.remove();
                }
            }
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull ImmediateSendData didReceive, @NonNull TargetIdentifier fromTarget) {
        // Unused
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull List<PayloadData> didShare, @NonNull TargetIdentifier fromTarget) {
        // Unused
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget) {
        // TODO may consider relative proximity as a useful priority item in future
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Location didVisit) {
        // Unused
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget, @NonNull PayloadData withPayload) {
        // Unused
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        // TODO Stop sending outgoing to BLE (for example) if that SensorType enters the off state
    }

    /// MARK: Layer 3 Manager functions
    @Override
    public void setIncoming(GPDMPLayer4Incoming in) {
        incomingInterface = in;
    }

    @Override
    public void setOutgoing(GPDMPLayer2Outgoing out) {
        outgoingInterface = out;
    }

    @Override
    public GPDMPLayer3Incoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer3Outgoing getOutgoingInterface() {
        return this;
    }


    /// MARK: Layer 3 Incoming functions
    @Override
    public void incoming(TargetIdentifier from, PayloadData data) {
        // Interpret packet format
        if (data.size() < 14) {
            // Invalid packet
            // TODO log issue
            return;
        }
        UInt32 rawTimeToAccess = data.uint32(0);
        UInt32 rawTimeout = data.uint32(4);
        UInt16 ttl = data.uint16(8);
        UInt16 minTransmissions = data.uint16(10);
        UInt16 maxTransmissions = data.uint16(12);
        Date timeToAccess = new Date(new java.util.Date(rawTimeToAccess.value));
        Date timeout = new Date(new java.util.Date(rawTimeout.value));
        PayloadData encryptedL4DataFragment = new PayloadData();
        encryptedL4DataFragment.append(data.subdata(14));

        // TODO process data and pass up as Layer4Incoming data
        // TODO fix dummy values in next line
//        incomingInterface.incoming(from,
//                new UUID(1,2),
//                new Date(), new Date(), new UInt16(1), new UInt16(2), new UInt16(4),
//                data);
        // TODO determine why/how channel ID can be here when it's a L4 concern?
        incomingInterface.incoming(from,
                new UUID(1,2),
                timeToAccess, timeout, ttl, minTransmissions, maxTransmissions,
                encryptedL4DataFragment);
    }

    /// MARK: Layer 3 Outgoing functions
    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl,
                         UInt16 minTransmissions, UInt16 maxTransmissions,
                         PayloadData encryptedL4DataFragment) {
        // NOTE: UNENCRYPTED PACKET FORMAT FOLLOWS:-
        // 4 bytes: timeToAccess as unsigned long int 32 bit
        // 4 bytes: timeout as unsigned long int 32 bit
        // 2 bytes: ttl as unsigned int 16 bit
        // 2 bytes: minTransmissions
        // 2 bytes: maxTransmissions
        // 27+ bytes: l4 payload data
        // For a total of 41+ bytes of data
        PayloadData l3EncData = new PayloadData();
        l3EncData.append(new UInt32(timeToAccess.secondsSinceUnixEpoch()));
        l3EncData.append(new UInt32(timeout.secondsSinceUnixEpoch()));
        l3EncData.append(ttl);
        l3EncData.append(minTransmissions);
        l3EncData.append(maxTransmissions);
        l3EncData.append(encryptedL4DataFragment);

        // Now decide on the message ID, and where to send it
        UUID messageID = UUID.randomUUID();
        // TODO generate outgoing PayloadData packet
        // TODO save message info metadata and payload data and message id in cache
        // TODO map SensorType+TargetIdentifier Tuples to outgoing message
        // TODO filter targets from all potential routes (i.e. filter the original sender)
        ArrayList targets = new ArrayList<TargetIdentifier>();
        for (Tuple<SensorType, TargetIdentifier> t: potentialRoutes) {
            targets.add(t.b);
        }
        outgoingInterface.outgoing(targets,l3EncData,messageID);
        return messageID;
    }

    /// MARK: Informational functions

    @Override
    public int getPotentialRecipientsCount() {
        return potentialRoutes.size();
    }
}
