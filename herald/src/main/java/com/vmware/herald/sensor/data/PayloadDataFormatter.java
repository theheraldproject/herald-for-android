//  Copyright 2021 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.data;

import com.vmware.herald.sensor.datatype.PayloadData;

public interface PayloadDataFormatter {
    String shortFormat(PayloadData payloadData);
}
