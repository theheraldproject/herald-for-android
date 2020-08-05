package org.c19x.sensor.datatype;

/// Ephemeral identifier for detected target (e.g. smartphone, beacon, place). This is likely to be an UUID but using String for variable identifier length.
public class TargetIdentifier {
    public final String value;

    public TargetIdentifier(String value) {
        this.value = value;
    }
}
