//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.PayloadData;

public class ConcretePayloadDataFormatter implements PayloadDataFormatter {

    @NonNull
    @Override
    public String shortFormat(@NonNull PayloadData payloadData) {
        return payloadData.shortName();
    }
}
