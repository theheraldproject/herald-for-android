//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor.datatype;

/// Raw location data for estimating indirect exposure, e.g. WGS84 coordinates
public interface LocationReference {
    String description();
}
