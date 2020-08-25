package org.c19x.sensor.datatype;

public class PayloadSharingData {
    public final RSSI rssi;
    public final Data data;

    /**
     * Payload sharing data
     *
     * @param rssi RSSI between self and peer.
     * @param data Payload data of devices being shared by self to peer.
     */
    public PayloadSharingData(final RSSI rssi, final Data data) {
        this.rssi = rssi;
        this.data = data;
    }
}
