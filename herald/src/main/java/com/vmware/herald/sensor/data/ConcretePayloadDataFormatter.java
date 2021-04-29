//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.data;

import com.vmware.herald.sensor.datatype.PayloadData;

public class ConcretePayloadDataFormatter implements PayloadDataFormatter {
    @Override
    public String shortFormat(PayloadData payloadData) {
        return payloadData.shortName();
    }
}
