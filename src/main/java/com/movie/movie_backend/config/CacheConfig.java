package com.movie.movie_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("verificationCodes");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 캐시에 데이터가 쓰여진 후 3분이 지나면 자동으로 만료됩니다.
                .expireAfterWrite(3, TimeUnit.MINUTES)
                // 메모리에 저장할 인증 코드의 최대 개수를 1000개로 제한합니다.
                .maximumSize(1000));
        return cacheManager;
    }
} 