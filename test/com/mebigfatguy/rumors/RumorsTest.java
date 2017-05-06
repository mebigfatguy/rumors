package com.mebigfatguy.rumors;

import org.junit.Test;

public class RumorsTest {

    private static final int NUM_THREADS = 5;

    @Test
    public void test() throws InterruptedException {

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(new RumorRunnableTest());
            threads[i].start();
        }

        Thread.sleep(10000);

        for (Thread t : threads) {
            try {
                synchronized (t) {
                    t.interrupt();
                    t.join();
                }
            } catch (InterruptedException ie) {
            }
        }

    }

    static class RumorRunnableTest implements Runnable {
        @Override
        public void run() {
            Rumors r = RumorsFactory.getRumors();
            try {
                r.begin();
            } catch (RumorsException e) {

            } finally {
                r.end();
            }

        }
    }
}
