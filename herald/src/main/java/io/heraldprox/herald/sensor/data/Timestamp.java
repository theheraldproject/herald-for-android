package io.heraldprox.herald.sensor.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import io.heraldprox.herald.sensor.datatype.Date;

/**
 * Timestamp generator for use across all loggers.
 */
public class Timestamp {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
    static {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Get given time as formatted timestamp "yyyy-MM-dd HH:mm:ss.SSSZ"
     * @return Formatted timestamp for given time.
     */
    @NonNull
    public final static synchronized String timestamp(@NonNull final Date date) {
        return dateFormatter.format(date);
    }

    /**
     * Get current time as formatted timestamp "yyyy-MM-dd HH:mm:ss.SSSZ"
     * @return Formatted timestamp for current time.
     */
    @NonNull
    public final static synchronized String timestamp() {
        return timestamp(new Date());
    }

    /**
     * Parse string as formatted timestamp "yyyy-MM-dd HH:mm:ss.SSSZ"
     * @param string Formatted timestamp for given time.
     * @return Parsed timestamp.
     */
    @Nullable
    public final static synchronized Date timestamp(@Nullable final String string) {
        if (null == string) {
            return null;
        }
        try {
            final java.util.Date parsedDate = dateFormatter.parse(string);
            if (null == parsedDate) {
                return null;
            }
            return new Date(parsedDate);
        } catch (Throwable e) {
            return null;
        }
    }

}
