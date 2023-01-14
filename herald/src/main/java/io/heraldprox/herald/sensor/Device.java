package io.heraldprox.herald.sensor;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.Date;

public class Device {
    /**
     * Device registration timestamp.
     */
    @NonNull
    public final Date createdAt;
    /**
     * Last time anything changed, e.g. attribute update.
     */
    @NonNull
    public Date lastUpdatedAt;
    /**
     * Ephemeral device identifier, e.g. peripheral identifier UUID.
     */
    @NonNull
    public final TargetIdentifier identifier;

    public Device(@NonNull final TargetIdentifier identifier) {
        this.createdAt = new Date();
        this.lastUpdatedAt = this.createdAt;
        this.identifier = identifier;
    }

    public Device(@NonNull final Device device, @NonNull final TargetIdentifier identifier) {
        this.createdAt = device.createdAt;
        this.lastUpdatedAt = new Date();
        this.identifier = identifier;
    }
}
