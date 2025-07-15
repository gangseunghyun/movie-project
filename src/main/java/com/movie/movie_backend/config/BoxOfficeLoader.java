package com.movie.movie_backend.config;

import com.movie.movie_backend.service.BoxOfficeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BoxOfficeLoader {

    private final BoxOfficeService boxOfficeService;

    @Bean
    @Order(2)
    public CommandLineRunner loadBoxOfficeData() {
        return args -> {
            log.info("=== 박스오피스 데이터 로드 시작 ===");
            try {
                // 일일 박스오피스 가져오기
                log.info("일일 박스오피스 가져오기 시작...");
                boxOfficeService.fetchDailyBoxOffice();
                log.info("일일 박스오피스 가져오기 완료");

                // 주간 박스오피스 가져오기
                log.info("주간 박스오피스 가져오기 시작...");
                boxOfficeService.fetchWeeklyBoxOffice();
                log.info("주간 박스오피스 가져오기 완료");

                log.info("=== 박스오피스 데이터 로드 완료 ===");
            } catch (Exception e) {
                log.error("박스오피스 데이터 로드 실패", e);
            }
        };
    }
    // 스케줄러: 박스오피스 데이터 업데이트 (매일 새벽 3시)
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledBoxOfficeUpdate() {
        log.info("=== 스케줄러: 박스오피스 데이터 업데이트 시작 ===");
        try {
            // 일일 박스오피스 업데이트
            log.info("스케줄러 - 일일 박스오피스 업데이트 시작");
            boxOfficeService.fetchDailyBoxOffice();
            log.info("스케줄러 - 일일 박스오피스 업데이트 완료");

            // 주간 박스오피스 업데이트
            log.info("스케줄러 - 주간 박스오피스 업데이트 시작");
            boxOfficeService.fetchWeeklyBoxOffice();
            log.info("스케줄러 - 주간 박스오피스 업데이트 완료");

            log.info("=== 스케줄러: 박스오피스 데이터 업데이트 완료 ===");
        } catch (Exception e) {
            log.error("스케줄러: 박스오피스 데이터 업데이트 실패", e);
        }
    }
}