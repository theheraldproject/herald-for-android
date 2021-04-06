//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VariantSet {
    private final int listSize;
    private final Map<Class<?>, ListManager<?>> map = new ConcurrentHashMap<>();

    public VariantSet(final int listSize) {
        this.listSize = listSize;
    }

    private final static <T extends DoubleValue> Class<T> variant(final ListManager<T> listManager) {
        return (Class<T>) ((ParameterizedType) listManager.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public <T extends DoubleValue> void add(final ListManager<T> listManager) {
        map.put(variant(listManager), listManager);
    }

    public <T extends DoubleValue> ListManager<T> get(final Class<T> variant) {
        ListManager<T> listManager = (ListManager<T>) map.get(variant);
        if (listManager == null) {
            listManager = new ListManager<T>(listSize);
            add(listManager);
        }
        return listManager;
    }

    public <T extends DoubleValue> void remove(final Class<T> variant) {
        map.remove(variant);
    }

    public int size() {
        return map.size();
    }
}
