package com.server.backend.auth.service;

public interface EmailCodeSender {
    void send(String email, String code, long validSeconds);
}
