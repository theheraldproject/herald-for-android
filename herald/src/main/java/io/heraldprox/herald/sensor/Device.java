package io.heraldprox.herald.sensor;

import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.Date;

public class Device {
    /// Device registration timestamp
    public final Date createdAt;
    /// Last time anything changed, e.g. attribute update
    public Date lastUpdatedAt = null;
    /// Ephemeral device identifier, e.g. peripheral identifier UUID
    public final TargetIdentifier identifier;

    public Device(TargetIdentifier identifier) {
        this.createdAt = new Date();
        this.lastUpdatedAt = this.createdAt;
        this.identifier = identifier;
    }

    public Device(Device device, TargetIdentifier identifier) {
        this.createdAt = device.createdAt;
        this.lastUpdatedAt = new Date();
        this.identifier = identifier;
    }
}
