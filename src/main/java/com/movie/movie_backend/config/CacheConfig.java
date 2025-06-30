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
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 인기검색어 캐시만 유지: 2분 TTL, 최대 100개 항목
        cacheManager.registerCustomCache("popularKeywords", 
            Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(100)
                .build());
        
        // 평균 별점 캐시: 5분 TTL, 최대 1000개 항목
        cacheManager.registerCustomCache("averageRatings", 
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
        
        // 별점 개수 캐시: 5분 TTL, 최대 1000개 항목
        cacheManager.registerCustomCache("ratingCounts", 
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
        
        return cacheManager;
    }
} 
