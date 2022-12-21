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
        // TODO process data and pass up as Layer4Incoming data
        // TODO fix dummy values in next line
        incomingInterface.incoming(from,
                new UUID(1,2),
                new Date(), new Date(), new UInt16(1), new UInt16(2), new UInt16(4),
                data);
    }

    /// MARK: Layer 3 Outgoing functions
    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl,
                         UInt16 minTransmissions, UInt16 maxTransmissions,
                         PayloadData encryptedL4DataFragment) {
        UUID messageID = UUID.randomUUID();
        // TODO generate outgoing PayloadData packet
        // TODO save message info metadata and payload data and message id in cache
        // TODO map SensorType+TargetIdentifier Tuples to outgoing message
        // TODO filter targets from all potential routes (i.e. filter the original sender)
        ArrayList targets = new ArrayList<TargetIdentifier>();
        for (Tuple<SensorType, TargetIdentifier> t: potentialRoutes) {
            targets.add(t.b);
        }
        outgoingInterface.outgoing(targets,encryptedL4DataFragment,messageID);
        return messageID;
    }

    /// MARK: Informational functions

    @Override
    public int getPotentialRecipientsCount() {
        return potentialRoutes.size();
    }
}
