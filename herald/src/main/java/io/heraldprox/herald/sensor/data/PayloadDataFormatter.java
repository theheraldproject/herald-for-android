//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.PayloadData;

public interface PayloadDataFormatter {
    @NonNull
    String shortFormat(@NonNull final PayloadData payloadData);
}
