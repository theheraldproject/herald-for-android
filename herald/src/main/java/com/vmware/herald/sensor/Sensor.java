//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor;

import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.TargetIdentifier;

/// Sensor for detecting and tracking various kinds of disease transmission vectors, e.g. contact with people, time at location.
public interface Sensor {
    /// Add delegate for responding to sensor events.
    void add(SensorDelegate delegate);

    /// Start sensing.
    void start();

    /// Stop sensing.
    void stop();
}
