package com.vmware.herald.sensor.datatype;

import org.junit.Test;

public class UInt16Test {

    @Test
    public void test() {
        System.out.println("value,uint16");
        for (int i=0; i<128; i++) {
            System.out.println(i + "," + new UInt16(i).bigEndian.base64EncodedString());
        }
        for (int i=65536-128; i<65536; i++) {
            System.out.println(i + "," + new UInt16(i).bigEndian.base64EncodedString());
        }
    }
}
