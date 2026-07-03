package com.qinyadan.system.dsp.runtime.engine.block;

import java.util.Arrays;

public class IntBlockTest {

    public static void main(String[] args) {
        IntBlock block = new IntBlock(BlockConstants.BLOCK_SIZE);

        int[] value = new int[100];
        final int length = value.length;

        for (int i = 0; i < length; i++) {
            value[i] = i;
        }

        for (int i = 0; i < length; i++) {
            if (block.canAdd()) {
                block.add(value[i]);
            }
        }

        int[] v = new int[length];

        for (int i = 0; i < length; i++) {
            v[i] = block.read(i);
        }

        if (!Arrays.equals(v, value)) {
            throw new RuntimeException("v and value is not the same...");
        }

        block.free();
    }
}
