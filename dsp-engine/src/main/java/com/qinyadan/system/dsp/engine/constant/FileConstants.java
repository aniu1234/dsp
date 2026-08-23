package com.qinyadan.system.dsp.engine.constant;

import com.qinyadan.system.dsp.core.config.DspConfiguration;

public class FileConstants {

    public static final String USER_HOME = System.getProperty("user.home");

    public static final String DEFAULT_TABLE_FILE_LOCATION = USER_HOME + "/test/sloth";

    private FileConstants() {
    }

    public static String getTableFileLocation() {
        return DspConfiguration.load().getDataDirectory().toString();
    }
}
