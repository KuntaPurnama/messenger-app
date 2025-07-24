package com.app.messenger.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(String phoneNumber);
    String extractPhoneNumber(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
