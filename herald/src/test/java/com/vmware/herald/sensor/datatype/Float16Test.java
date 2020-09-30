package com.vmware.herald.sensor.datatype;

import org.junit.Test;

public class Float16Test {

    @Test
    public void test() {
        System.out.println("value,float16");
        System.out.println("-65504," + new Float16(-65504).bigEndian.base64EncodedString());
        System.out.println("-0.0000000596046," + new Float16(-0.0000000596046f).bigEndian.base64EncodedString());
        System.out.println("0," + new Float16(0).bigEndian.base64EncodedString());
        System.out.println("0.0000000596046," + new Float16(0.0000000596046f).bigEndian.base64EncodedString());
        System.out.println("65504," + new Float16(65504).bigEndian.base64EncodedString());
    }
}
