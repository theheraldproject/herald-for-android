//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

/**
 * Specifies the methods for a Layer2 BluetoothLE Provider (Manager is the impl)
 */
public interface GPDMPLayer2BluetoothLEManager {

    /**
     * Set the interface which this manager sends encapsulated data onto.
     * @param out
     */
    void setOutgoing(GPDMPLayer1BluetoothLEOutgoing out);

    /**
     * Set the interface which this manager passes parsed data onto.
     *
     * @param in
     */
    void setIncoming(GPDMPLayer3Incoming in);

    GPDMPLayer2BluetoothLEIncoming getIncomingInterface();

    GPDMPLayer2BluetoothLEOutgoing getOutgoingInterface();
}
