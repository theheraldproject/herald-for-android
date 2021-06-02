//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

public interface CallableForNewSample<T> {

    void newSample(@NonNull final SampledID sampled, @NonNull final Sample<T> item);
}
