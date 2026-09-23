package com.example.siomanager.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    public static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    public record PasswordData(String hash, String salt, int iterations) { }

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordData hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        try {
            return new PasswordData(
                    Base64.getEncoder().encodeToString(hash),
                    Base64.getEncoder().encodeToString(salt),
                    ITERATIONS
            );
        } finally {
            Arrays.fill(hash, (byte) 0);
            Arrays.fill(salt, (byte) 0);
        }
    }

    public boolean verify(char[] password, String expectedHash, String encodedSalt, int iterations) {
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        byte[] expected = Base64.getDecoder().decode(expectedHash);
        byte[] actual = derive(password, salt, iterations);
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(expected, (byte) 0);
            Arrays.fill(actual, (byte) 0);
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Le système ne prend pas en charge le hachage sécurisé des mots de passe.", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
