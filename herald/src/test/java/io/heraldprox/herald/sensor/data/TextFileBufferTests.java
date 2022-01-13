package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

public class TextFileBufferTests {

    @Test
    public void writeNow() throws Exception {
        final TextFile textFile = new TextFileBuffer();
        textFile.writeNow("test");
        assertEquals("test\n", textFile.contentsOf());
    }

    @Test
    public void write() throws Exception {
        final TextFile textFile = new TextFileBuffer();
        textFile.write("test");
        // Content shouldn't exist yet, as write has been buffered
        assertEquals("", textFile.contentsOf());
        textFile.flush();
        assertEquals("test\n", textFile.contentsOf());
    }

    @Test
    public void reset() throws Exception {
        final TextFile textFile = new TextFileBuffer();
        textFile.writeNow("test");
        assertEquals("test\n", textFile.contentsOf());
        textFile.reset();
        assertEquals("", textFile.contentsOf());
    }

    @Test
    public void overwrite() throws Exception {
        final TextFile textFile = new TextFileBuffer();
        assertTrue(textFile.empty());
        textFile.writeNow("test");
        assertEquals("test\n", textFile.contentsOf());
        textFile.overwrite("overwrite");
        assertEquals("overwrite\n", textFile.contentsOf());
    }

    @Test
    public void forEachLine() throws Exception {
        final TextFile textFile = new TextFileBuffer();
        // Empty
        final StringBuilder fileEmpty = new StringBuilder();
        textFile.forEachLine(new TextFile.TextFileLineConsumer() {
            @Override
            public void apply(@NonNull String line) {
                fileEmpty.append(line);
                fileEmpty.append('\n');
            }
        });
        assertEquals("", fileEmpty.toString());
        // 1
        textFile.writeNow("1");
        final StringBuilder fileOne = new StringBuilder();
        textFile.forEachLine(new TextFile.TextFileLineConsumer() {
            @Override
            public void apply(@NonNull String line) {
                fileOne.append(line);
                fileOne.append('\n');
            }
        });
        assertEquals("1\n", fileOne.toString());
        // 1,2
        textFile.writeNow("2");
        final StringBuilder fileTwo = new StringBuilder();
        textFile.forEachLine(new TextFile.TextFileLineConsumer() {
            @Override
            public void apply(@NonNull String line) {
                fileTwo.append(line);
                fileTwo.append('\n');
            }
        });
        assertEquals("1\n2\n", fileTwo.toString());
    }
}
