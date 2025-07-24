package com.app.messenger.service.impl;

import com.app.messenger.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {
    private final String secret = "secret-asd-qwerty-asd-asd-asd-asd-asd-asd-asd";
    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());


    @Override
    public String generateToken(String phoneNumber) {
        return Jwts.builder()
                .setSubject(phoneNumber)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractPhoneNumber(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJwt(token)
                .getBody()
                .getSubject();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String phoneNumber = extractPhoneNumber(token);
        return userDetails.getUsername().equals(phoneNumber);
    }
}
