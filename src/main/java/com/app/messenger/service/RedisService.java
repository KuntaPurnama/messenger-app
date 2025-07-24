package com.app.messenger.service;

import java.time.Duration;
import java.util.List;

public interface RedisService {
    void set(String key, String value, Duration ttl);
    void set(String key, String value);
    <T> T get(String key, Class<T> clazz);
    void delete(String key);
    <T> List<T> multiGet(List<String> keys, Class<T> clazz);
    List<String> multiGet(List<String> keys);
}
