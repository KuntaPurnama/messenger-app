package com.app.messenger.service.impl;

import com.app.messenger.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return null;

        try {
            return objectMapper.readValue(value, clazz);
        } catch (JsonProcessingException e) {
            // Handle deserialization error
            log.error("error when transform value with key {} in redis: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public <T> List<T> multiGet(List<String> keys, Class<T> clazz) {
        List<String> rawList = redisTemplate.opsForValue().multiGet(keys);
        if (rawList == null) return Collections.emptyList();

        return rawList.stream()
                .filter(Objects::nonNull)
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj, clazz);
                    } catch (JsonProcessingException e) {
                        log.error("error when transform value of {}", obj);
                    }
                    return null;
                })
                .collect(Collectors.toList());
    }

    @Override
    public <T> Set<T> multiGetSet(List<String> keys, Class<T> clazz) {
        List<String> rawList = redisTemplate.opsForValue().multiGet(keys);
        if (rawList == null) return new HashSet<>();

        return rawList.stream()
                .filter(Objects::nonNull)
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj, clazz);
                    } catch (JsonProcessingException e) {
                        log.error("error when transform value of {}", obj);
                    }
                    return null;
                })
                .collect(Collectors.toSet());
    }
}
