//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

public class ImmediateSendData {
    public final Data data;

    /**
     * Immediate Send data
     *
     * @param data Data being send using immediateSend (app specific format).
     */
    public ImmediateSendData(final Data data) {
        this.data = data;
    }
}
