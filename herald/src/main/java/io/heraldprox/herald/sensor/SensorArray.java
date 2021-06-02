//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;
import io.heraldprox.herald.sensor.ble.ConcreteBLESensor;
import io.heraldprox.herald.sensor.data.CalibrationLog;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.motion.ConcreteInertiaSensor;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private final List<Sensor> sensorArray = new ArrayList<>();
    @NonNull
    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";

    @NonNull
    private final ConcreteBLESensor concreteBleSensor;

    public SensorArray(@NonNull final Context context, @NonNull final PayloadDataSupplier payloadDataSupplier) {
        // Ensure logger has been initialised (should have happened in AppDelegate already)
        ConcreteSensorLogger.context(context);
        logger.debug("init");

        // Define sensor array
        concreteBleSensor = new ConcreteBLESensor(context, payloadDataSupplier);
        sensorArray.add(concreteBleSensor);
        // Inertia sensor configured for automated RSSI-distance calibration data capture
        if (BLESensorConfiguration.inertiaSensorEnabled) {
            logger.debug("Inertia sensor enabled");
            sensorArray.add(new ConcreteInertiaSensor(context));
            add(new CalibrationLog(context, "calibration.csv"));
        }
        payloadData = payloadDataSupplier.payload(new PayloadTimestamp(), null);
        logger.info("DEVICE (payload={},description={})", payloadData.shortName(), SensorArray.deviceDescription);

//        // Test Diffie-Hellman on hardware
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                test_dh();
//            }
//        }).start();
    }

    /// Immediate send data.
    public boolean immediateSend(@NonNull final Data data, @NonNull final TargetIdentifier targetIdentifier) {
        return concreteBleSensor.immediateSend(data,targetIdentifier);
    }

    /// Immediate send to all (connected / recent / nearby)
    public boolean immediateSendAll(@NonNull final Data data) {
        return concreteBleSensor.immediateSendAll(data);
    }

    @NonNull
    public final PayloadData payloadData() {
        return payloadData;
    }

    @Override
    public void add(@NonNull final SensorDelegate delegate) {
        for (Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        logger.debug("start");
        for (Sensor sensor : sensorArray) {
            sensor.start();
        }
    }

    @Override
    public void stop() {
        logger.debug("stop");
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
    }

//    // MARK: - Instrumented DH test
//
//    private void test_dh() {
//        // MODP Group 1 : First Oakley Group 768-bits, generator = 2
//        final String modpGroup1 = (
//                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
//                        "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
//                        "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
//                        "E485B576 625E7EC6 F44C42E9 A63A3620 FFFFFFFF FFFFFFFF")
//                .replaceAll(" ", "");
//        // MODP Group 2 : Second Oakley Group 1024-bits, generator = 2
//        final String modpGroup2 = (
//                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
//                        "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
//                        "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
//                        "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED" +
//                        "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE65381" +
//                        "FFFFFFFF FFFFFFFF")
//                .replaceAll(" ", "");
//        final String modpGroup5 = (
//                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
//                        "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
//                        "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
//                        "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED" +
//                        "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D" +
//                        "C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F" +
//                        "83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D" +
//                        "670C354E 4ABC9804 F1746C08 CA237327 FFFFFFFF FFFFFFFF")
//                .replaceAll(" ", "");
//        final String modpGroup14 = (
//                "FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1" +
//                        "29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD" +
//                        "EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245" +
//                        "E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED" +
//                        "EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D" +
//                        "C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F" +
//                        "83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D" +
//                        "670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B" +
//                        "E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9" +
//                        "DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510" +
//                        "15728E5A 8AACAA68 FFFFFFFF FFFFFFFF")
//                .replaceAll(" ", "");
//
//        final UIntBig p = new UIntBig(modpGroup1);
//        final UIntBig g = new UIntBig(2);
//        logger.debug("DHKA: p bits: {}", p.bitLength());
//        logger.debug("DHKA: g bits: {}", g.bitLength());
//        logger.debug("DHKA: p = {}", p.hexEncodedString());
//
//        final RandomSource secureRandom = new RandomSource(RandomSource.Method.SecureRandom);
//        final UIntBig alicePrivateKey = new UIntBig(p.bitLength()-2, secureRandom);
//        logger.debug("DHKA: alice private key bits: {}", alicePrivateKey.bitLength());
//        logger.debug("DHKA: alice private key = {}", alicePrivateKey.hexEncodedString());
//        final UIntBig alicePublicKey = g.modPow(alicePrivateKey, p);
//        logger.debug("DHKA: alice public key bits: {}", alicePublicKey.bitLength());
//        logger.debug("DHKA: alice public key = {}", alicePublicKey.hexEncodedString());
//
//        final UIntBig bobPrivateKey = new UIntBig(p.bitLength()-2, secureRandom);
//        logger.debug("DHKA: bob private key bits: {}", bobPrivateKey.bitLength());
//        logger.debug("DHKA: bob private key = {}", bobPrivateKey.hexEncodedString());
//        final UIntBig bobPublicKey = g.modPow(bobPrivateKey, p);
//        logger.debug("DHKA: bob public key bits: {}", bobPublicKey.bitLength());
//        logger.debug("DHKA: bob public key = {}", bobPublicKey.hexEncodedString());
//
//        final UIntBig aliceSharedKey = bobPublicKey.modPow(alicePrivateKey, p);
//        logger.debug("DHKA: alice shared key bits: {}", aliceSharedKey.bitLength());
//        logger.debug("DHKA: alice shared key = {}", aliceSharedKey.hexEncodedString());
//        final UIntBig bobSharedKey = alicePublicKey.modPow(bobPrivateKey, p);
//        logger.debug("DHKA: bob shared key bits: {}", bobSharedKey.bitLength());
//        logger.debug("DHKA: bob shared key = {}", bobSharedKey.hexEncodedString());
//        logger.debug("DHKA: alice and bob have same shared key = {}", aliceSharedKey.equals(bobSharedKey));
//    }
}
