package com.movie.movie_backend.scheduler;

import com.movie.movie_backend.service.BoxOfficeService;
import com.movie.movie_backend.service.KobisApiService;
import com.movie.movie_backend.service.TmdbPosterBatchService;
import com.movie.movie_backend.service.TmdbStillcutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieAutoSyncScheduler {

    private final BoxOfficeService boxOfficeService;
    private final KobisApiService kobisApiService;
    private final TmdbPosterBatchService tmdbPosterBatchService;
    private final TmdbStillcutService tmdbStillcutService;

    @Value("${kobis.api.key:}")
    private String kobisApiKey;
    
    @Value("${tmdb.api.key:}")
    private String tmdbApiKey;

    // 1. 박스오피스/신작 자동 동기화 (매일 오후 12시)
    @Scheduled(cron = "0 0 12 * * *")
    public void syncBoxOfficeAndNewMovies() {
        log.info("=== 박스오피스/신작 자동 동기화 시작 ===");
        
        // API 키 확인
        if (kobisApiKey == null || kobisApiKey.isEmpty()) {
            log.error("KOBIS API 키가 설정되지 않았습니다. application.yml을 확인해주세요.");
            return;
        }
        
        try {
            log.info("1. 박스오피스 데이터 동기화 시작...");
            boxOfficeService.fetchDailyBoxOffice();
            log.info("1. 박스오피스 데이터 동기화 완료");
            
            log.info("2. 신작 영화 데이터 동기화 시작...");
            kobisApiService.fetchComingSoonMovies(30);
            log.info("2. 신작 영화 데이터 동기화 완료");
            
            log.info("=== 박스오피스/신작 자동 동기화 완료 ===");
        } catch (Exception e) {
            log.error("박스오피스/신작 자동 동기화 실패", e);
        }
    }

    // 2. TMDB 포스터/스틸컷/줄거리 자동 보완 (매일 오후 12시 30분)
    @Scheduled(cron = "0 30 12 * * *")
    public void syncTmdbMedia() {
        log.info("=== TMDB 포스터/스틸컷/줄거리 자동 보완 시작 ===");
        
        // API 키 확인
        if (tmdbApiKey == null || tmdbApiKey.isEmpty()) {
            log.error("TMDB API 키가 설정되지 않았습니다. application.yml을 확인해주세요.");
            return;
        }
        
        try {
            log.info("1. TMDB 포스터 URL 업데이트 시작...");
            tmdbPosterBatchService.updatePosterUrlsForAllMovies();
            log.info("1. TMDB 포스터 URL 업데이트 완료");
            
            log.info("2. TMDB 스틸컷 업데이트 시작...");
            tmdbStillcutService.updateStillcutsForAllMovies();
            log.info("2. TMDB 스틸컷 업데이트 완료");
            
            log.info("3. TMDB 줄거리 보완 시작...");
            kobisApiService.fillMissingMovieDetailsFromTmdb();
            log.info("3. TMDB 줄거리 보완 완료");
            
            log.info("=== TMDB 포스터/스틸컷/줄거리 자동 보완 완료 ===");
        } catch (Exception e) {
            log.error("TMDB 포스터/스틸컷/줄거리 자동 보완 실패", e);
        }
    }

    // 3. 테스트용 스케줄러 (1분마다 실행 - 개발/테스트용)
    @Scheduled(cron = "0 */1 * * * *")
    public void testScheduler() {
        log.info("=== 스케줄러 테스트 실행 중... ===");
        log.info("KOBIS API 키 설정 여부: {}", kobisApiKey != null && !kobisApiKey.isEmpty());
        log.info("TMDB API 키 설정 여부: {}", tmdbApiKey != null && !tmdbApiKey.isEmpty());
        log.info("=== 스케줄러 테스트 완료 ===");
    }
} 