package com.movie.movie_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.movie_backend.entity.MovieDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverMovieService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.api.client-id}")
    private String naverClientId;

    @Value("${naver.api.client-secret}")
    private String naverClientSecret;

    private static final String NAVER_SEARCH_URL = "https://openapi.naver.com/v1/search/movie.json";

    /**
     * 네이버 영화 검색 API로 영화 정보 가져오기
     */
    public Map<String, Object> searchMovieInfo(String movieTitle) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String encodedTitle = java.net.URLEncoder.encode(movieTitle, java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format("%s?query=%s&display=1", NAVER_SEARCH_URL, encodedTitle);
            
            // 네이버 API 헤더 설정
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            String response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class).getBody();
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("items") && root.get("items").isArray() && root.get("items").size() > 0) {
                JsonNode movie = root.get("items").get(0);
                result.put("title", movie.get("title").asText().replaceAll("<[^>]*>", ""));
                result.put("director", movie.get("director").asText().replaceAll("<[^>]*>", ""));
                result.put("actors", movie.get("actor").asText().replaceAll("<[^>]*>", ""));
                result.put("description", movie.get("description").asText().replaceAll("<[^>]*>", ""));
                
                log.info("네이버 영화 검색 성공: {}", movieTitle);
            }
            
        } catch (Exception e) {
            log.warn("네이버 영화 검색 실패: {} - {}", movieTitle, e.getMessage());
        }
        
        return result;
    }

    /**
     * 네이버 검색으로 배우별 한국어 배역명 가져오기 (크롤링 방식)
     */
    public Map<String, String> getNaverCharacterNames(String movieTitle) {
        Map<String, String> characterNames = new HashMap<>();
        
        try {
            // 네이버 영화 검색으로 기본 정보 가져오기
            Map<String, Object> movieInfo = searchMovieInfo(movieTitle);
            
            if (movieInfo.containsKey("actors")) {
                String actors = (String) movieInfo.get("actors");
                String description = (String) movieInfo.get("description");
                
                // 배우 목록 파싱 (쉼표로 구분)
                String[] actorList = actors.split(",");
                
                for (String actor : actorList) {
                    actor = actor.trim();
                    if (!actor.isEmpty()) {
                        // 설명에서 배우 이름과 배역명 매칭 시도
                        String characterName = extractCharacterNameFromDescription(actor, description);
                        if (characterName != null && !characterName.isEmpty()) {
                            characterNames.put(actor, characterName);
                            log.info("네이버 배역명: {} - {}", actor, characterName);
                        }
                    }
                }
            }
            
            log.info("네이버 배역명 {}개 가져옴: 영화={}", characterNames.size(), movieTitle);
            
        } catch (Exception e) {
            log.warn("네이버 배역명 가져오기 실패: {} - {}", movieTitle, e.getMessage());
        }
        
        return characterNames;
    }

    /**
     * 설명에서 배우 이름과 배역명 매칭
     */
    private String extractCharacterNameFromDescription(String actorName, String description) {
        try {
            // 간단한 패턴 매칭 (실제로는 더 정교한 로직 필요)
            if (description.contains(actorName)) {
                // 배우 이름 주변 텍스트에서 배역명 추출 시도
                int index = description.indexOf(actorName);
                if (index > 0) {
                    // 앞뒤 텍스트에서 배역명 패턴 찾기
                    String beforeText = description.substring(Math.max(0, index - 50), index);
                    String afterText = description.substring(index + actorName.length(), 
                            Math.min(description.length(), index + actorName.length() + 50));
                    
                    // 배역명 패턴 찾기 (예: "역의", "역", "배역" 등)
                    String[] patterns = {"역의", "역", "배역", "역할"};
                    for (String pattern : patterns) {
                        if (beforeText.contains(pattern)) {
                            // 배역명 추출 로직
                            return extractRoleName(beforeText, pattern);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("배역명 추출 실패: {} - {}", actorName, e.getMessage());
        }
        
        return null;
    }

    /**
     * 텍스트에서 배역명 추출
     */
    private String extractRoleName(String text, String pattern) {
        try {
            int patternIndex = text.lastIndexOf(pattern);
            if (patternIndex > 0) {
                // 패턴 앞의 텍스트에서 배역명 추출
                String beforePattern = text.substring(0, patternIndex);
                String[] words = beforePattern.split("\\s+");
                if (words.length > 0) {
                    return words[words.length - 1] + pattern;
                }
            }
        } catch (Exception e) {
            log.debug("배역명 추출 실패: {}", e.getMessage());
        }
        
        return null;
    }
} 
