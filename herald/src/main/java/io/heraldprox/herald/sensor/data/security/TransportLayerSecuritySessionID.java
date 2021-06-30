//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import io.heraldprox.herald.sensor.datatype.UInt32;

/**
 * Session identifier derived from SHA hash of public key.
 */
public class TransportLayerSecuritySessionID extends UInt32 {

    public TransportLayerSecuritySessionID(final UInt32 value) {
        super(value.value);
    }
}
