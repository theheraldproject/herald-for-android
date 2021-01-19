//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.util.UUID;

/// Legacy payload data received from target
public class LegacyPayloadData extends PayloadData {
    public final UUID service;

    public LegacyPayloadData(final UUID service, final byte[] value) {
        super(value);
        this.service = service;
    }

    /// Suffix :L for legacy payloads
    @Override
    public String shortName() {
        return super.shortName() + ":L";
    }
}
