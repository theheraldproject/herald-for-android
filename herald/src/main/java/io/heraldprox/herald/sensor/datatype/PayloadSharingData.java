//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

public class PayloadSharingData {
    @NonNull
    public final RSSI rssi;
    @NonNull
    public final Data data;

    /**
     * Payload sharing data
     *
     * @param rssi RSSI between self and peer.
     * @param data Payload data of devices being shared by self to peer.
     */
    public PayloadSharingData(@NonNull final RSSI rssi, @NonNull final Data data) {
        this.rssi = rssi;
        this.data = data;
    }
}
