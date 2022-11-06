package com.daml.wallet;

public class Util {
    private Util() {}
    public final static void waitIndefinitely() {
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
