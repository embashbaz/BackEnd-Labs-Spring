package com.example.Smart_E_Commerce_System.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's caching annotations (@Cacheable, @CacheEvict).
 *
 * No cache manager bean is defined here: Spring Boot creates a simple in-memory one
 * (ConcurrentMapCacheManager) from spring.cache.* in application.yml.
 * It lives inside the application, so it is cleared on every restart and is not
 * shared between multiple running instances. That is fine for this project.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}