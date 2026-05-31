package com.lfms.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing helper. Uses BCrypt (work factor 12) for hashing and verification.
 * Never stores or compares plaintext passwords.
 */
public class HashUtil {

    private static final int WORK_FACTOR = 12;

    private HashUtil() {
    }

    /** Returns a BCrypt hash of the given plaintext password. */
    public static String hash(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash. Returns false for a
     * null/empty/malformed hash rather than throwing.
     */
    public static boolean verify(String plaintext, String hash) {
        if (plaintext == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
