package com.qinyadan.system.dsp.engine.constant;


public class FileConstants {

    public static final String USER_HOME = System.getProperty("user.home");

    public static final String DEFAULT_TABLE_FILE_LOCATION = USER_HOME + "/test/sloth";

    private FileConstants() {
    }

    public static String getTableFileLocation() {
        String configured = System.getProperty("dsp.data.dir");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("DSP_DATA_DIR");
        }
        return configured == null || configured.trim().isEmpty()
                ? DEFAULT_TABLE_FILE_LOCATION : configured;
    }
}
