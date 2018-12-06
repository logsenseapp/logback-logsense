package com.logsense;


import java.util.Random;

public class App {
    static Random r = new Random();

    public static int gaussianTemp(int mean, int sd) {
        return mean + (int) (r.nextGaussian()*sd);
    }

    public static void main( String[] args ) throws Exception
    {

        Wombat w = new Wombat();

        while (true) {
            w.setTemperature(gaussianTemp(10, 15));
            w.setTemperature(gaussianTemp(45, 10));
            w.setTemperature(gaussianTemp(100, 20));
            w.setTemperature(gaussianTemp(1000, 100));
            w.setTemperature(gaussianTemp(-200, 200));
            Thread.sleep(1000);
        }
    }
}
