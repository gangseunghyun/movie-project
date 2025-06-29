package com.movie.movie_backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchLogCleanupService {
    @PersistenceContext
    private EntityManager em;

    // 매일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldSearchLogs() {
        em.createNativeQuery(
            "DELETE FROM search_log WHERE searched_at < DATE_SUB(NOW(), INTERVAL 30 DAY)"
        ).executeUpdate();
    }
} 