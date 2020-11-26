//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

/// Generic callback function
public interface Callback<T> {
    void accept(T value);
}
