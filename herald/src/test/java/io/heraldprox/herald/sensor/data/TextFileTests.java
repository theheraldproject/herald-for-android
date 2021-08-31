package io.heraldprox.herald.sensor.data;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

}
