package com.vmware.herald.sensor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import static org.junit.Assert.*;

public class TestUtil {
    public final static File testDataFolder = new File("./test");
    public final static File androidTestDataFolder = new File(testDataFolder, "android");
    public final static File iosTestDataFolder = new File(testDataFolder, "ios");

    public final static File androidFile(final String filename) {
        if (!androidTestDataFolder.exists()) {
            androidTestDataFolder.mkdirs();
        }
        return new File(androidTestDataFolder, filename);
    }

    public final static File iosFile(final String filename) {
        if (!iosTestDataFolder.exists()) {
            iosTestDataFolder.mkdirs();
        }
        return new File(iosTestDataFolder, filename);
    }

    public final static PrintWriter androidPrintWriter(final String filename) throws Exception {
        return new PrintWriter(new BufferedWriter(new FileWriter(androidFile(filename))));
    }
    public final static void assertEqualsCrossPlatform(final String filename) throws Exception {
        final File androidFile = androidFile(filename);
        final File iosFile = iosFile(filename);

        assertTrue(androidFile.exists());
        assertTrue(androidFile.canRead());
        assertTrue(androidFile.isFile());
        assertTrue(iosFile.exists());
        assertTrue(iosFile.canRead());
        assertTrue(iosFile.isFile());

        int line = 0;
        final BufferedReader androidReader = new BufferedReader(new FileReader(androidFile));
        final BufferedReader iosReader = new BufferedReader(new FileReader(iosFile));
        String androidLine = null, iosLine = null;
        do {
            androidLine = androidReader.readLine();
            iosLine = iosReader.readLine();
            line++;
            assertEquals("Line " + line + " is different", androidLine, iosLine);
        } while (androidLine != null && iosLine != null);
        assertNull(androidLine);
        assertNull(iosLine);
    }

}
