package io.heraldprox.herald.sensor.payload.extended;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Float16;
import io.heraldprox.herald.sensor.datatype.Int16;
import io.heraldprox.herald.sensor.datatype.Int32;
import io.heraldprox.herald.sensor.datatype.Int64;
import io.heraldprox.herald.sensor.datatype.Int8;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt32;
import io.heraldprox.herald.sensor.datatype.UInt64;
import io.heraldprox.herald.sensor.datatype.UInt8;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class ConcreteExtendedDataV1Tests {
    @Test
    public void testInitialExtendedDataHasNoData() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();
        assertFalse(extendedData.hasData());
    }

    @Test
    public void testExtendedDataWithPayloadHasData() {
        PayloadData payloadData = new PayloadData(new byte[] {0,1});
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1(payloadData);
        assertTrue(extendedData.hasData());
    }

    @Test
    public void testSectionWithUInt8() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        UInt8 sectionValue = new UInt8(3);
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.uint8(0));
    }

    @Test
    public void testSectionWithUInt16() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        UInt16 sectionValue = UInt16.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.uint16(0));
    }

    @Test
    public void testSectionWithUInt32() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        UInt32 sectionValue = UInt32.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.uint32(0));
    }

    @Test
    public void testSectionWithUInt64() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        UInt64 sectionValue = UInt64.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.uint64(0));
    }

    @Test
    public void testSectionWithInt8() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        Int8 sectionValue = Int8.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.int8(0));
    }

    @Test
    public void testSectionWithInt16() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        Int16 sectionValue = Int16.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.int16(0));
    }

    @Test
    public void testSectionWithInt32() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        Int32 sectionValue = Int32.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.int32(0));
    }

    @Test
    public void testSectionWithInt64() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        Int64 sectionValue = Int64.max;
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data.int64(0));
    }

    @Test
    public void testSectionWithString() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        String sectionValue = "some value";
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, new String(sections.get(0).data.value));
    }

    @Test
    public void testSectionWithFloat16() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        Float16 sectionValue = new Float16(6.55f);
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue.value, sections.get(0).data.float16(0).value, 1e-2);
    }

    @Test
    public void testSectionWithData() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 sectionCode = new UInt8(0x01);
        Data sectionValue = new Data(new byte[] {0,1});
        extendedData.addSection(sectionCode, sectionValue);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(1, sections.size());
        assertEquals(sectionCode, sections.get(0).code);
        assertEquals(sectionValue, sections.get(0).data);
    }

    @Test
    public void testMultipleSections() {
        ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();

        UInt8 section1Code = new UInt8(0x01);
        UInt8 section1Value = new UInt8(3);
        extendedData.addSection(section1Code, section1Value);

        UInt8 section2Code = new UInt8(0x02);
        UInt16 section2Value = new UInt16(25);
        extendedData.addSection(section2Code, section2Value);

        List<ConcreteExtendedDataSectionV1> sections = extendedData.getSections();
        assertEquals(2, sections.size());
    }
}