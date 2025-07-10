package com.movie.movie_backend.config;

import com.movie.movie_backend.service.TagDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TagMapper {

    private final TagDataService tagDataService;

    @Bean
    @Order(6)  // KMDb ID 매핑 다음에 실행
    public CommandLineRunner autoMapMovieDetailTags() {
        return args -> {
            log.info("=== TagDataService.setupTagData() 자동 실행 ===");
            tagDataService.setupTagData();
        };
    }
} 