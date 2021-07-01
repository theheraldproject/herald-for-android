//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Codec for signal characteristic data bundles
 */
public class SignalCharacteristicData {

    /**
     * Encode write RSSI data bundle
     * writeRSSI data format (byte number : use)
     *
     * 0-0 : actionCode
     *
     * 1-2 : rssi value (Int16)
     */
    @NonNull
    public static Data encodeWriteRssi(@NonNull final RSSI rssi) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWriteRSSI);
        byteBuffer.putShort(1, (short) rssi.value);
        return new Data(byteBuffer.array());
    }

    /**
     * Decode write RSSI data bundle
     *
     * @param data The Data instance to decode the RSSI from
     */
    @Nullable
    public static RSSI decodeWriteRSSI(@Nullable final Data data) {
        //noinspection ConstantConditions
        if (null == data || null == data.value) {
            return null;
        }
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWriteRSSI) {
            return null;
        }
        if (data.value.length != 3) {
            return null;
        }
        final Short rssiValue = int16(data.value, 1);
        if (null == rssiValue) {
            return null;
        }
        return new RSSI(rssiValue.intValue());
    }

    /**
     * Encode write payload data bundle
     * writePayload data format
     *     // 0-0 : actionCode
     *     // 1-2 : payload data count in bytes (Int16)
     *     // 3.. : payload data
     */
    @NonNull
    public static Data encodeWritePayload(@NonNull final PayloadData payloadData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3 + payloadData.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWritePayload);
        byteBuffer.putShort(1, (short) payloadData.value.length);
        byteBuffer.position(3);
        byteBuffer.put(payloadData.value);
        return new Data(byteBuffer.array());
    }

    /**
     * Decode write payload data bundle
     */
    @Nullable
    public static PayloadData decodeWritePayload(@Nullable final Data data) {
        //noinspection ConstantConditions
        if (null == data || null == data.value) {
            return null;
        }
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWritePayload) {
            return null;
        }
        if (data.value.length < 3) {
            return null;
        }
        final Short payloadDataCount = int16(data.value, 1);
        if (null == payloadDataCount) {
            return null;
        }
        if (0 == payloadDataCount) {
            return new PayloadData();
        }
        if (data.value.length != (3 + payloadDataCount.intValue())) {
            return null;
        }
        final Data payloadDataBytes = new Data(data.value).subdata(3);
        if (null == payloadDataBytes) {
            return null;
        }
        return new PayloadData(payloadDataBytes.value);
    }

    /**
     * Encode write payload sharing data bundle
     * writePayloadSharing data format
     * 0-0 : actionCode
     * 1-2 : rssi value (Int16)
     * 3-4 : payload sharing data count in bytes (Int16)
     * 5.. : payload sharing data
     *
     * @param payloadSharingData The data to share.
     */
    @NonNull
    public static Data encodeWritePayloadSharing(@NonNull final PayloadSharingData payloadSharingData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(5 + payloadSharingData.data.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWritePayloadSharing);
        byteBuffer.putShort(1, (short) payloadSharingData.rssi.value);
        byteBuffer.putShort(3, (short) payloadSharingData.data.value.length);
        byteBuffer.position(5);
        byteBuffer.put(payloadSharingData.data.value);
        return new Data(byteBuffer.array());
    }

    /**
     * Decode write payload data bundle
     *
     * @param data The raw received Data to decode into PayloadSharingData
     */
    @Nullable
    public static PayloadSharingData decodeWritePayloadSharing(@Nullable final Data data) {
        //noinspection ConstantConditions
        if (null == data || null == data.value) {
            return null;
        }
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWritePayloadSharing) {
            return null;
        }
        if (data.value.length < 5) {
            return null;
        }
        final Short rssiValue = int16(data.value, 1);
        if (null == rssiValue) {
            return null;
        }
        final Short payloadSharingDataCount = int16(data.value, 3);
        if (null == payloadSharingDataCount) {
            return null;
        }
        if (0 == payloadSharingDataCount) {
            return new PayloadSharingData(new RSSI(rssiValue.intValue()), new Data());
        }
        if (data.value.length != (5 + payloadSharingDataCount.intValue())) {
            return null;
        }
        final Data payloadSharingDataBytes = new Data(data.value).subdata(5);
        if (null == payloadSharingDataBytes) {
            return null;
        }
        return new PayloadSharingData(new RSSI(rssiValue.intValue()), payloadSharingDataBytes);
    }

    /**
     * Encode immediate send data bundle immediateSend data format
     * <br>0-0 : actionCode
     * <br>1-2 : payload data count in bytes (Int16)
     * <br>3.. : payload data
     * @param immediateSendData
     * @return
     */
    @NonNull
    public static Data encodeImmediateSend(@NonNull final ImmediateSendData immediateSendData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3 + immediateSendData.data.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, BLESensorConfiguration.signalCharacteristicActionWriteImmediate);
        byteBuffer.putShort(1, (short) immediateSendData.data.value.length);
        byteBuffer.position(3);
        byteBuffer.put(immediateSendData.data.value);
        return new Data(byteBuffer.array());
    }

    /**
     * Decode immediate send data bundle
     * @param data
     * @return
     */
    @Nullable
    public static ImmediateSendData decodeImmediateSend(@Nullable final Data data) {
        //noinspection ConstantConditions
        if (null == data || null == data.value) {
            return null;
        }
        if (signalDataActionCode(data.value) != BLESensorConfiguration.signalCharacteristicActionWriteImmediate) {
            return null;
        }
        if (data.value.length < 3) {
            return null;
        }
        final Short immediateSendDataCount = int16(data.value, 1);
        if (null == immediateSendDataCount) {
            return null;
        }
        if (0 == immediateSendDataCount) {
            return new ImmediateSendData(new Data());
        }
        if (data.value.length != (3 + immediateSendDataCount.intValue())) {
            return null;
        }
        final Data immediateSendDataBytes = new Data(data.value).subdata(3);
        if (null == immediateSendDataBytes) {
            return null;
        }
        return new ImmediateSendData(immediateSendDataBytes);
    }

    /**
     * Detect signal characteristic data bundle type.
     * @param data
     * @return
     */
    @NonNull
    public static SignalCharacteristicDataType detect(@NonNull final Data data) {
        switch (signalDataActionCode(data.value)) {
            case BLESensorConfiguration.signalCharacteristicActionWriteRSSI:
                return SignalCharacteristicDataType.rssi;
            case BLESensorConfiguration.signalCharacteristicActionWritePayload:
                return SignalCharacteristicDataType.payload;
            case BLESensorConfiguration.signalCharacteristicActionWritePayloadSharing:
                return SignalCharacteristicDataType.payloadSharing;
            case BLESensorConfiguration.signalCharacteristicActionWriteImmediate:
                return SignalCharacteristicDataType.immediateSend;
            default:
                return SignalCharacteristicDataType.unknown;
        }
    }

    private static byte signalDataActionCode(@Nullable final byte[] signalData) {
        if (null == signalData || 0 == signalData.length) {
            return 0;
        }
        return signalData[0];
    }

    @Nullable
    private static Short int16(@NonNull final byte[] data, final int index) {
        if (index < data.length - 1) {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getShort(index);
        } else {
            return null;
        }
    }
}
