//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

/// Raw location data for estimating indirect exposure, e.g. WGS84 coordinates
public interface LocationReference {
    String description();
}
