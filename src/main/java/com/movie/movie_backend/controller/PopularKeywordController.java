package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.PopularKeyword;
import com.movie.movie_backend.repository.PopularKeywordRepository;
import com.movie.movie_backend.service.PopularKeywordBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/popular-keywords")
@RequiredArgsConstructor
public class PopularKeywordController {

    private final PopularKeywordRepository popularKeywordRepository;
    private final PopularKeywordBatchService popularKeywordBatchService;

    @GetMapping
    @Cacheable(value = "popularKeywords", key = "'all'")
    public List<PopularKeyword> getPopularKeywords() {
        log.info("인기검색어 조회 요청");
        List<PopularKeyword> keywords = popularKeywordRepository.findTop10ByOrderBySearchCountDesc();
        log.info("인기검색어 조회 결과: {}개", keywords.size());
        return new ArrayList<>(keywords);
    }

    @PostMapping("/aggregate")
    @CacheEvict(value = "popularKeywords", allEntries = true)
    public String aggregatePopularKeywords() {
        log.info("인기검색어 수동 집계 요청");
        popularKeywordBatchService.aggregatePopularKeywords();
        return "인기검색어 집계 완료";
    }

    @PostMapping("/clear-cache")
    @CacheEvict(value = "popularKeywords", allEntries = true)
    public String clearPopularKeywordsCache() {
        log.info("인기검색어 캐시 무효화 요청");
        return "인기검색어 캐시 무효화 완료";
    }
} 