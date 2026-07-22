package com.qinyadan.system.dsp.protocol.pkg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class MysqlNativePassword {

    private MysqlNativePassword() {
    }

    public static boolean matches(String password, byte[] challenge, byte[] response) {
        if (password == null || challenge == null || response == null) {
            return false;
        }
        return MessageDigest.isEqual(scramble(password, challenge), response);
    }

    public static byte[] scramble(String password, byte[] challenge) {
        if (password.isEmpty()) {
            return new byte[0];
        }

        MessageDigest sha1 = newSha1();
        byte[] passwordHash = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
        byte[] doubleHash = sha1.digest(passwordHash);
        sha1.update(challenge);
        byte[] challengeHash = sha1.digest(doubleHash);

        byte[] result = new byte[passwordHash.length];
        for (int i = 0; i < passwordHash.length; i++) {
            result[i] = (byte) (passwordHash[i] ^ challengeHash[i]);
        }
        return result;
    }

    private static MessageDigest newSha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required by mysql_native_password", e);
        }
    }
}
