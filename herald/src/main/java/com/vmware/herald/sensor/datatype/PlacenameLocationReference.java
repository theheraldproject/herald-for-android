//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

/// Free text place name.
public class PlacenameLocationReference implements LocationReference {
    public final String name;

    public PlacenameLocationReference(String name) {
        this.name = name;
    }

    public String description() {
        return "PLACE(name=" + name + ")";
    }
}
