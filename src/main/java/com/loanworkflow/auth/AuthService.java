package com.loanworkflow.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // Demo users only. Will be replaced by a real database later.
    private static final byte[] SALT = "demo-salt".getBytes(StandardCharsets.UTF_8);

    private final Map<String, byte[]> users = Map.of("alice", hash("password123"));

    public boolean checkLogin(String username, String password) {
        byte[] stored = users.get(username);
        if (stored == null || password == null) {
            return false;
        }
        return MessageDigest.isEqual(stored, hash(password));
    }

    private static byte[] hash(String password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, 100_000, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
