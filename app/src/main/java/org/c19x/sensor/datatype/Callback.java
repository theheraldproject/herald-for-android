//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package org.c19x.sensor.datatype;

/// Generic callback function
public interface Callback<T> {
    void accept(T value);
}
