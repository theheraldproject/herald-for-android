//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.c19x;

import java.util.Date;

/// Timestamp has been abstracted to enable change from Date if required in the future.
public class Timestamp {
    public final Date value;

    public Timestamp(Date value) {
        this.value = value;
    }

    public Timestamp() {
        this(new Date());
    }

}
