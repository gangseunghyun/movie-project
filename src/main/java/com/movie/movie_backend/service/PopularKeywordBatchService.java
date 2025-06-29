package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.PopularKeyword;
import com.movie.movie_backend.repository.PopularKeywordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularKeywordBatchService {

    private final PopularKeywordRepository popularKeywordRepository;

    @PersistenceContext
    private EntityManager em;

    // 테스트용: 1분, 실제 운영시: 7일로 변경
    private static final int AGGREGATION_MINUTES = 180; // 3시간 = 180분

    // 3분마다 실행 (매 3분마다)
    @Scheduled(cron = "0 */3 * * * *")
    @Transactional
    @CacheEvict(value = "popularKeywords", allEntries = true)
    public void aggregatePopularKeywords() {
        log.info("인기검색어 슬라이딩 윈도우 집계 배치 시작 ({}분)", AGGREGATION_MINUTES);
        
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(AGGREGATION_MINUTES);
        LocalDateTime now = LocalDateTime.now();
        
        log.info("집계 기간: {} ~ {}", windowStart, now);

        // 슬라이딩 윈도우로 최근 N분간의 검색어만 집계
        List<Object[]> results = em.createNativeQuery(
            "SELECT keyword, COUNT(*) as cnt " +
            "FROM search_log " +
            "WHERE searched_at >= :windowStart " +
            "GROUP BY keyword " +
            "ORDER BY cnt DESC " +
            "LIMIT 10"
        )
        .setParameter("windowStart", windowStart)
        .getResultList();

        log.info("집계된 검색어 개수: {}개", results.size());

        // 기존 popular_keyword 전체 삭제 (슬라이딩 윈도우 방식)
        popularKeywordRepository.deleteAll();
        log.info("기존 인기검색어 삭제 완료");

        // 새로 저장
        int savedCount = 0;
        for (Object[] row : results) {
            String keyword = (String) row[0];
            int count = ((Number) row[1]).intValue();
            PopularKeyword pk = new PopularKeyword();
            pk.setKeyword(keyword);
            pk.setSearchCount(count);
            pk.setAggregatedAt(now);
            popularKeywordRepository.save(pk);
            savedCount++;
            log.debug("인기검색어 저장: {} ({}회)", keyword, count);
        }
        
        log.info("인기검색어 슬라이딩 윈도우 집계 배치 완료: {}개 저장", savedCount);
    }
} 