//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ListManager<T extends DoubleValue> {
    private final int listSize;
    private final List<SampledID> lists = new ArrayList<>();
    private final Map<SampledID, SampleList<T>> map = new ConcurrentHashMap<>();

    public ListManager(final int listSize) {
        this.listSize = listSize;
    }

    public synchronized void add(final SampledID listFor, final SampleList<T> list) {
        lists.remove(listFor);
        map.put(listFor, list);
        lists.add(listFor);
    }

    public synchronized SampleList<T> list(final SampledID listFor) {
        SampleList<T> list = map.get(listFor);
        if (list == null) {
            list = new SampleList<>(listSize);
            add(listFor, list);
        }
        return list;
    }

    public synchronized List<SampledID> lists() {
        return lists;
    }

    public synchronized void remove(final SampledID listFor) {
        map.remove(listFor);
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized void clear() {
        lists.clear();
        map.clear();
    }
}
