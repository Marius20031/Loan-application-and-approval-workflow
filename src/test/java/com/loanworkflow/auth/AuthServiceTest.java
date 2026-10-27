package com.loanworkflow.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private final AuthService service = new AuthService();

    @Test
    void validLogin() {
        assertTrue(service.checkLogin("alice", "password123"));
    }

    @Test
    void wrongPassword() {
        assertFalse(service.checkLogin("alice", "nope"));
    }

    @Test
    void unknownUser() {
        assertFalse(service.checkLogin("bob", "password123"));
    }
}
