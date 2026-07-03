package com.qinyadan.system.dsp.register;


public class ConfigurationServerMain {

    public static void main(String[] args) {
        //some parameter
        int port = 3100;
        ConfigurationCenter configurationCenter = new ConfigurationCenter(port);
        configurationCenter.start();
    }
}
