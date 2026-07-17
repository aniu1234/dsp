package com.qinyadan.system.dsp.protocol.pkg.auth;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MysqlNativePasswordTest {

    @Test
    public void verifiesNativePasswordChallengeResponse() {
        byte[] challenge = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        byte[] response = MysqlNativePassword.scramble("secret", challenge);

        assertTrue(MysqlNativePassword.matches("secret", challenge, response));
        assertFalse(MysqlNativePassword.matches("wrong", challenge, response));
    }
}
