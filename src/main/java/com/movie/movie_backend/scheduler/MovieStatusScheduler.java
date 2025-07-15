package com.movie.movie_backend.scheduler;

import com.movie.movie_backend.service.KobisApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieStatusScheduler {

    private final KobisApiService kobisApiService;

    /**
     * 영화 상태 자동 관리 스케줄러 (매일 새벽 2시)
     * 
     * 개봉일 기준으로 영화 상태를 자동으로 변경:
     * - 개봉일 미래: COMING_SOON (개봉예정)
     * - 개봉일 현재~3개월: NOW_PLAYING (개봉중)
     * - 개봉일 3개월 초과: ENDED (상영종료)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void syncMovieStatus() {
        log.info("=== 영화 상태 자동 관리 시작 ===");
        
        try {
            log.info("1. 개봉일 기준 영화 상태 자동 변경 시작...");
            
            // KobisApiService의 cleanupComingSoonMovies 메서드 호출
            kobisApiService.cleanupComingSoonMovies();
            
            log.info("2. 영화 상태 자동 변경 완료");
            log.info("=== 영화 상태 자동 관리 완료 ===");
            
        } catch (Exception e) {
            log.error("영화 상태 자동 관리 실패", e);
        }
    }
} 