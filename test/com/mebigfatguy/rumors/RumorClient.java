package com.mebigfatguy.rumors;

public class RumorClient {

    public static void main(String[] args) throws InterruptedException {
        Rumors r = RumorsFactory.getRumors();
        try {
            r.begin();

            Thread.sleep(10000);
        } catch (RumorsException e) {

        } finally {
            r.end();
        }

    }

}
