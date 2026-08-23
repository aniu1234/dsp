package com.qinyadan.system.dsp.core.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DspConfigurationTest {

    @Test
    public void loadsValidatedTypedValues() {
        String previousPort = System.getProperty("dsp.server.port");
        String previousRows = System.getProperty("dsp.write.max-rows-per-insert");
        String previousMutationRows =
                System.getProperty("dsp.write.max-rows-per-mutation");
        try {
            System.setProperty("dsp.server.port", "4306");
            System.setProperty("dsp.write.max-rows-per-insert", "42");
            System.setProperty("dsp.write.max-rows-per-mutation", "21");
            DspConfiguration configuration = DspConfiguration.load();
            assertEquals(4306, configuration.getServerPort());
            assertEquals(42, configuration.getWriteMaxRowsPerInsert());
            assertEquals(21, configuration.getWriteMaxRowsPerMutation());
        } finally {
            restore("dsp.server.port", previousPort);
            restore("dsp.write.max-rows-per-insert", previousRows);
            restore("dsp.write.max-rows-per-mutation", previousMutationRows);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidBoolean() {
        String previous = System.getProperty("dsp.storage.sync-writes");
        try {
            System.setProperty("dsp.storage.sync-writes", "sometimes");
            DspConfiguration.load();
        } finally {
            restore("dsp.storage.sync-writes", previous);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
