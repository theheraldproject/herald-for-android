//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Objects;

/**
 * Time interval in seconds.
 */
public class TimeInterval implements DoubleValue {
    public final long value;
    public static final TimeInterval day = new TimeInterval(24*60*60);
    public static final TimeInterval hour = new TimeInterval(60*60);
    public static final TimeInterval minute = new TimeInterval(60);
    public static final TimeInterval zero = new TimeInterval(0);
    public static final TimeInterval never = new TimeInterval(Long.MAX_VALUE);

    public TimeInterval(final long seconds) {
        this.value = seconds;
    }

    public TimeInterval(@NonNull final Date date) {
        this.value = date.getTime() / 1000;
    }

    public TimeInterval(@NonNull final Date from, @NonNull final Date to) {
        this.value = (to.getTime() - from.getTime()) / 1000;
    }

    @NonNull
    public static TimeInterval days(final long days) {
        return new TimeInterval(day.value * days);
    }

    @NonNull
    public static TimeInterval hours(final long hours) {
        return new TimeInterval(hour.value * hours);
    }

    @NonNull
    public static TimeInterval minutes(final long minutes) {
        return new TimeInterval(minute.value * minutes);
    }

    @NonNull
    public static TimeInterval seconds(final long seconds) {
        return new TimeInterval(seconds);
    }

    public long millis() {
        return value * 1000;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        TimeInterval that = (TimeInterval) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        if (value == never.value) {
            return "never";
        }
        return Long.toString(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
