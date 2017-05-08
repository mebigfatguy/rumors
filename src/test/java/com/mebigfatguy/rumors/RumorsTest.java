package com.mebigfatguy.rumors;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RumorsTest {

    private static final int NUM_THREADS = 5;

    @Test
    public void test() throws IOException, InterruptedException {

        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            RumorsClient client = new RumorsClient();
            threads[i] = new Thread(client);
            threads[i].start();
        }

        Thread.sleep(5000);

        for (Thread thread : threads) {
            try {
                thread.interrupt();
                thread.join();
            } catch (InterruptedException e) {
            }
        }

    }

    static class RumorsClient implements Runnable {

        private static Logger LOGGER = LoggerFactory.getLogger(RumorsClient.class);

        @Override
        public void run() {

            Rumors rumor = RumorsFactory.createRumors();

            try {
                rumor.begin();
                while (!Thread.interrupted()) {
                    Thread.sleep(100);
                }
            } catch (RumorsException e) {
                LOGGER.error("Failed with error: ", e);
            } catch (InterruptedException e) {
            } finally {
                rumor.end();
            }
        }
    }
}
