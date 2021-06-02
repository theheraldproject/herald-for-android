//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

public class ImmediateSendData {
    @NonNull
    public final Data data;

    /**
     * Immediate Send data
     *
     * @param data Data being send using immediateSend (app specific format).
     */
    public ImmediateSendData(@NonNull final Data data) {
        this.data = data;
    }
}
