//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor.datatype;

/// Generic callback function
public interface Callback<T> {
    void accept(T value);
}
