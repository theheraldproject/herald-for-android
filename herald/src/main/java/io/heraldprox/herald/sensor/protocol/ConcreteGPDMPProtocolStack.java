//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

/**
 * Represents a configured and linked protocol stack.
 * Includes methods for swapping out layers without ending up with null pointer exceptions.
 * @created 22 May 2022
 * @since v2.2
 */
public class ConcreteGPDMPProtocolStack {
    public static ConcreteGPDMPProtocolStack createDefaultBluetoothLEStack() {
        return new ConcreteGPDMPProtocolStack(
                new DummyGPDMPLayer1BluetoothLEManager(),
                new ConcreteGPDMPLayer2BluetoothLEManager(),
                new ConcreteGPDMPLayer3Manager(),
                new ConcreteGPDMPLayer4Manager(),
                new ConcreteGPDMPLayer5Manager(),
                new ConcreteGPDMPLayer6Manager(),
                new ConcreteGPDMPLayer7Manager()
        );
    }

    private GPDMPLayer1BluetoothLEManager layer1; // NOTE only BLE supported today
    private GPDMPLayer2BluetoothLEManager layer2; // NOTE only BLE supported today
    private GPDMPLayer3Manager layer3;
    private GPDMPLayer4Manager layer4;
    private GPDMPLayer5Manager layer5;
    private GPDMPLayer6Manager layer6;
    private GPDMPLayer7Manager layer7;

    // TODO support multiple layer 1&2 (I.e. multiple bearers) and
    //      layer4-7s (I.e. multiple apps/message types) at the same time

    /**
     * Creates a protocol stack from a set of Manager instances.
     *
     * @param l1 The lowest level layer 1 (output only) (Dummy if app sends via Herald app)
     * @param l2 A mid level layer 2 (Normally BLE, but Dummy if app sends via Herald app)
     * @param l3 A mid level layer 3 (Normally managed by the Herald API App, or HeraldAppLayer3 interlink)
     * @param l4 A mid level layer 4 (Normally managed by the Herald API, or HeraldAppLayer4 interlink)
     * @param l5 A mid level layer 5 (Normally managed by the Herald API, or Dummy (if L4 is another app)
     * @param l6 A mid level layer 6 (Normally managed by the Herald API, or Dummy (if L4 is another app)
     * @param l7 A top level layer 7 (Normally managed by the Herald API, or Dummy (if L4 is another app)
     */
    public ConcreteGPDMPProtocolStack(
            GPDMPLayer1BluetoothLEManager l1,
            GPDMPLayer2BluetoothLEManager l2,
            GPDMPLayer3Manager l3,
            GPDMPLayer4Manager l4,
            GPDMPLayer5Manager l5,
            GPDMPLayer6Manager l6,
            GPDMPLayer7Manager l7
    ) {
        layer1 = l1;
        layer2 = l2;
        layer3 = l3;
        layer4 = l4;
        layer5 = l5;
        layer6 = l6;
        layer7 = l7;

        layer1.setIncoming(layer2.getIncomingInterface());

        layer2.setOutgoing(layer1.getOutgoingInterface());
        layer2.setIncoming(layer3.getIncomingInterface());

        layer3.setOutgoing(layer2.getOutgoingInterface());
        layer3.setIncoming(layer4.getIncomingInterface());

        layer4.setOutgoing(layer3.getOutgoingInterface());
        layer4.setIncoming(layer5.getIncomingInterface());

        layer5.setOutgoing(layer4.getOutgoingInterface());
        layer5.setIncoming(layer6.getIncomingInterface());

        layer6.setOutgoing(layer5.getOutgoingInterface());
        layer6.setIncoming(layer7.getIncomingInterface());

        layer7.setOutgoing(layer6.getOutgoingInterface());
    }

    // TODO make this generic for any Layer1, not just BLE
    public void replaceLayer1(GPDMPLayer1BluetoothLEManager l1) {
        layer1.setIncoming(null); // remove link to layer 2
        layer1 = l1;
        layer1.setIncoming(layer2.getIncomingInterface());
        layer2.setOutgoing(layer1.getOutgoingInterface());
    }

    // TODO make this generic for any Layer2, not just BLE
    public void replaceLayer2(GPDMPLayer2BluetoothLEManager l2) {
        layer2.setOutgoing(null); // remove link to layer 1
        layer2.setIncoming(null); // remove link to layer 3
        layer2 = l2;
        layer1.setIncoming(layer2.getIncomingInterface());
        layer2.setOutgoing(layer1.getOutgoingInterface());
        layer2.setIncoming(layer3.getIncomingInterface());
        layer3.setOutgoing(layer2.getOutgoingInterface());
    }

    public void replaceLayer3(GPDMPLayer3Manager l3) {
        layer3.setOutgoing(null); // remove link to layer 2
        layer3.setIncoming(null); // remove link to layer 4
        layer3 = l3;
        layer2.setIncoming(layer3.getIncomingInterface());
        layer3.setOutgoing(layer2.getOutgoingInterface());
        layer3.setIncoming(layer4.getIncomingInterface());
        layer4.setOutgoing(layer3.getOutgoingInterface());
    }

    public void replaceLayer4(GPDMPLayer4Manager l4) {
        layer4.setOutgoing(null); // remove link to layer 3
        layer4.setIncoming(null); // remove link to layer 5
        layer4 = l4;
        layer3.setIncoming(layer4.getIncomingInterface());
        layer4.setOutgoing(layer3.getOutgoingInterface());
        layer4.setIncoming(layer5.getIncomingInterface());
        layer5.setOutgoing(layer4.getOutgoingInterface());
    }

    public void replaceLayer5(GPDMPLayer5Manager l5) {
        layer5.setOutgoing(null); // remove link to layer 4
        layer5.setIncoming(null); // remove link to layer 6
        layer5 = l5;
        layer4.setIncoming(layer5.getIncomingInterface());
        layer5.setOutgoing(layer4.getOutgoingInterface());
        layer5.setIncoming(layer6.getIncomingInterface());
        layer6.setOutgoing(layer5.getOutgoingInterface());
    }

    public void replaceLayer6(GPDMPLayer6Manager l6) {
        layer6.setOutgoing(null); // remove link to layer 5
        layer6.setIncoming(null); // remove link to layer 7
        layer6 = l6;
        layer5.setIncoming(layer6.getIncomingInterface());
        layer6.setOutgoing(layer5.getOutgoingInterface());
        layer6.setIncoming(layer7.getIncomingInterface());
        layer7.setOutgoing(layer6.getOutgoingInterface());
    }

    public void replaceLayer7(GPDMPLayer7Manager l7) {
        layer7.setOutgoing(null); // remove link to layer 6
        layer7 = l7;
        layer6.setIncoming(layer7.getIncomingInterface());
        layer7.setOutgoing(layer6.getOutgoingInterface());
    }

    public GPDMPLayer1BluetoothLEManager getLayer1() {
        return layer1;
    }

    public GPDMPLayer2BluetoothLEManager getLayer2() {
        return layer2;
    }

    public GPDMPLayer3Manager getLayer3() {
        return layer3;
    }

    public GPDMPLayer4Manager getLayer4() {
        return layer4;
    }

    public GPDMPLayer5Manager getLayer5() {
        return layer5;
    }

    public GPDMPLayer6Manager getLayer6() {
        return layer6;
    }

    public GPDMPLayer7Manager getLayer7() {
        return layer7;
    }
}
