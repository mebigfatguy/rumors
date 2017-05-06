package com.mebigfatguy.rumors;

import java.io.IOException;

import org.junit.Test;

public class RumorsTest {

    private static final int NUM_PROCESSES = 5;

    @Test
    public void test() throws IOException, InterruptedException {

        Process[] processes = new Process[NUM_PROCESSES];

        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < NUM_PROCESSES; i++) {
            processes[i] = rt.exec("java com.mebigfatguy.rumors.RumorClient");
        }

        for (int i = 0; i < NUM_PROCESSES; i++) {
            processes[i].waitFor();
        }
    }
}
