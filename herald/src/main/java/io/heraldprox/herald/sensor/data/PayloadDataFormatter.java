//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import io.heraldprox.herald.sensor.datatype.PayloadData;

public interface PayloadDataFormatter {
    String shortFormat(PayloadData payloadData);
}
