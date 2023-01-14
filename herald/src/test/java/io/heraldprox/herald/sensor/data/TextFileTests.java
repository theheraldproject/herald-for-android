package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.annotation.NonNull;

public class TextFileTests {

    @Test
    public void writeNow() throws Exception {
        final File file = new File("TextFileTests.writeNow");
        file.delete();
        final TextFile textFile = new TextFile(file);
        textFile.writeNow("test");
        assertEquals("test\n", textFile.contentsOf());
        file.delete();
    }

    @Test
    public void write() throws Exception {
        final File file = new File("TextFileTests.write");
        file.delete();
        final TextFile textFile = new TextFile(file);
        textFile.write("test");
        // File shouldn't exist yet, as write has been buffered
        assertFalse(file.exists());
        assertEquals("", textFile.contentsOf());
        textFile.flush();
        assertEquals("test\n", textFile.contentsOf());
        file.delete();
    }

    @Test
    public void reset() throws Exception {
        final File file = new File("TextFileTests.reset");
        file.delete();
        final TextFile textFile = new TextFile(file);
        textFile.writeNow("test");
        assertEquals("test\n", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        textFile.reset();
        assertEquals("", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        file.delete();
    }

    @Test
    public void overwrite() throws Exception {
        final File file = new File("TextFileTests.overwrite");
        file.delete();
        final TextFile textFile = new TextFile(file);
        assertTrue(textFile.empty());
        textFile.writeNow("test");
        assertEquals("test\n", textFile.contentsOf());
        textFile.overwrite("overwrite");
        assertEquals("overwrite\n", textFile.contentsOf());
        file.delete();
    }

    @Test
    public void forEachLine() throws Exception {
        final File file = new File("TextFileTests.forEachLine");
        file.delete();
        final TextFile textFile = new TextFile(file);
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
        file.delete();
    }

}
