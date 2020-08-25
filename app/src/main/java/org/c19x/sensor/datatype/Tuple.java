package org.c19x.sensor.datatype;

public class Tuple<A, B> {
    private final String labelA, labelB;
    public final A a;
    public final B b;

    public Tuple(A a, B b) {
        this("a", a, "b", b);
    }

    public Tuple(String labelA, A a, String labelB, B b) {
        this.labelA = labelA;
        this.labelB = labelB;
        this.a = a;
        this.b = b;
    }

    public A getA(String labelA) {
        return a;
    }

    public B getB(String labelB) {
        return b;
    }

    @Override
    public String toString() {
        return "(" + labelA + "=" + a + "," + labelB + "=" + b + ")";
    }
}
