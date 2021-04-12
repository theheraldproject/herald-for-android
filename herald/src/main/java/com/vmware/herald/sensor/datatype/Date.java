//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/// UTC date time
public class Date extends java.util.Date {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Date() {
        super();
    }

    public Date(final java.util.Date date) {
        super(date.getTime());
    }

    public Date(final long secondsSinceUnixEpoch) {
        super(secondsSinceUnixEpoch * 1000);
    }

    public long secondsSinceUnixEpoch() {
        return getTime() / 1000;
    }

    public String toString() {
        return dateFormat.format(this);
    }
}
