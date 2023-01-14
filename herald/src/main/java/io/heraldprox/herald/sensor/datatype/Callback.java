//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

/**
 * Generic callback function
 * @param <T>
 */
public interface Callback<T> {
    void accept(final T value);
}
