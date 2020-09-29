package com.vmware.herald.sensor.datatype;

import org.junit.Test;

public class UInt8Test {

    @Test
    public void test() {
        System.out.println("value,uint8");
        for (int i=0; i<256; i++) {
            System.out.println(i + "," + new UInt8(i).bigEndian.base64EncodedString());
        }
    }
}
