package io.heraldprox.herald.sensor.data;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * In memory implementation of TextFile to enable automated testing of loggers.
 */
public class TextFileBuffer extends TextFile {
    private final StringBuilder buffer = new StringBuilder();

    public TextFileBuffer() {
        super(new File("./TextFileBuffer.txt"));
    }

    public TextFileBuffer(@NonNull final String content) {
        this();
        buffer.append(content);
    }

    @Override
    public synchronized void forEachLine(@NonNull TextFileLineConsumer consumer) {
        if (empty()) {
            return;
        }
        for (final String line : buffer.toString().split("\n")) {
            consumer.apply(line);
        }
    }

    @Override
    public synchronized boolean empty() {
        return 0 == buffer.length();
    }

    @Override
    public synchronized void writeNow(@NonNull String line) {
        buffer.append(line);
        buffer.append('\n');
    }

    @Override
    public synchronized void overwrite(@NonNull String content) {
        clearBuffer();
        buffer.delete(0, buffer.length());
        buffer.append(content);
    }
}
