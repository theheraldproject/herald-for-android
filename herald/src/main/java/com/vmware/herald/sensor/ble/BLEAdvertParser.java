//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.datatype.UInt8;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ArrayList;

public class BLEAdvertParser {
    public static BLEScanResponseData parseScanResponse(byte[] raw, int offset) {
        // Multiple segments until end of binary data
        return new BLEScanResponseData(raw.length - offset, extractSegments(raw, offset));
    }

    public static List<BLEAdvertSegment> extractSegments(byte[] raw, int offset) {
        int position = offset;
        ArrayList<BLEAdvertSegment> segments = new ArrayList<BLEAdvertSegment>();
        int segmentLength;
        int segmentType;
        byte[] segmentData;
        int c;

        while (position < raw.length) {
            if ((position + 2) <= raw.length) {
                segmentLength = (byte)raw[position++] & 0xff;
                segmentType = (byte)raw[position++] & 0xff;
                // Note: Unsupported types are handled as 'unknown'
                // check reported length with actual remaining data length
                if ((position + segmentLength - 1) <= raw.length) {
                    segmentData = subDataBigEndian(raw, position, segmentLength - 1); // Note: type IS INCLUDED in length
                    position += segmentLength - 1;
                    segments.add(new BLEAdvertSegment(BLEAdvertSegmentType.typeFor(segmentType), segmentLength - 1, segmentData));
                } else {
                    // error in data length - advance to end
                    position = raw.length;
                }
            } else {
                // invalid segment - advance to end
                position = raw.length;
            }
        }

        return segments;
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static byte[] subDataBigEndian(byte[] raw, int offset, int length) {
        if (offset <= 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        byte[] data = new byte[length];
        int position = offset;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position++];
        }
        return data;
    }

    public static byte[] subDataLittleEndian(byte[] raw, int offset, int length) {
        if (offset <= 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        byte[] data = new byte[length];
        int position = offset + length - 1;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position--];
        }
        return data;
    }

    public static Integer extractTxPower(List<BLEAdvertSegment> segments) {
        // find the txPower code segment in the list
        for (BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.txPowerLevel) {
                return (new UInt8((int)segment.data[0])).value;
            }
        }
        return null;
    }

    public static List<BLEAdvertManufacturerData> extractManufacturerData(List<BLEAdvertSegment> segments) {
        // find the manufacturerData code segment in the list
        List<BLEAdvertManufacturerData> manufacturerData = new ArrayList<>();
        for (BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.manufacturerData) {
                // Ensure that the data area is long enough
                if (segment.data.length < 2) {
                    continue; // there may be a valid segment of same type... Happens for manufacturer data
                }
                // Create a manufacturer data segment
//                final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
//                byteBuffer.order(ByteOrder.BIG_ENDIAN);
//                byteBuffer.put(segment.data,0,2);
//                int intValue = byteBuffer.getInt();

                int intValue = ((segment.data[1]&0xff) << 8) | (segment.data[0]&0xff);
                manufacturerData.add(new BLEAdvertManufacturerData(intValue,subDataBigEndian(segment.data,2,segment.dataLength - 2)));
            }
        }
        return manufacturerData;
    }

    public static boolean isAppleTV(List<BLEAdvertSegment> segments) {
        List<BLEAdvertManufacturerData> manufacturerData = extractManufacturerData(segments);
        if (manufacturerData.size() == 0) {
            return false;
        }
        for (BLEAdvertManufacturerData manu : manufacturerData) {
            if (manu.data.length < 2) {
                continue;
            }
            if (16 == manu.data[0] && 7 == manu.data[1]) {
                return true;
            }
        }
        return false;
    }

}
