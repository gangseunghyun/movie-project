package com.movie.movie_backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 영화 제목 관련 유틸리티 클래스
 */
@Slf4j
@Component
public class MovieTitleUtil {

    /**
     * 한글 제목에서 영문 제목 추출
     * 
     * @param movieNm 한글 영화 제목
     * @return 영문 제목 (추출 실패시 null)
     */
    public static String extractEnglishTitle(String movieNm) {
        if (movieNm == null || movieNm.isEmpty()) {
            return null;
        }
        
        // 패턴 1: "한글제목 (English Title)" 형태
        if (movieNm.contains("(") && movieNm.contains(")")) {
            int startIndex = movieNm.indexOf("(");
            int endIndex = movieNm.indexOf(")");
            if (startIndex < endIndex && startIndex >= 0 && endIndex >= 0) {
                String englishPart = movieNm.substring(startIndex + 1, endIndex).trim();
                // 영문인지 확인 (한글이 포함되지 않은 경우)
                if (!englishPart.matches(".*[가-힣].*") && englishPart.length() > 2) {
                    return englishPart;
                }
            }
        }
        
        // 패턴 2: "한글제목: English Title" 형태
        if (movieNm.contains(":")) {
            String[] parts = movieNm.split(":");
            if (parts.length >= 2) {
                String englishPart = parts[1].trim();
                // 영문인지 확인
                if (!englishPart.matches(".*[가-힣].*") && englishPart.length() > 2) {
                    return englishPart;
                }
            }
        }
        
        // 패턴 3: "한글제목 - English Title" 형태
        if (movieNm.contains(" - ")) {
            String[] parts = movieNm.split(" - ");
            if (parts.length >= 2) {
                String englishPart = parts[1].trim();
                // 영문인지 확인
                if (!englishPart.matches(".*[가-힣].*") && englishPart.length() > 2) {
                    return englishPart;
                }
            }
        }
        
        return null;
    }

    /**
     * 영화 제목 정규화 (특수문자 제거, 공백 정리)
     * 
     * @param title 원본 제목
     * @return 정규화된 제목
     */
    public static String normalizeTitle(String title) {
        if (title == null || title.isEmpty()) {
            return title;
        }
        
        return title.trim()
                .replaceAll("\\s+", " ")  // 연속된 공백을 하나로
                .replaceAll("[\\[\\]()]", ""); // 대괄호, 소괄호 제거
    }

    /**
     * 영화 제목이 영문인지 확인
     * 
     * @param title 제목
     * @return 영문이면 true
     */
    public static boolean isEnglishTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        
        // 한글이 포함되지 않고 영문/숫자/공백/특수문자만 포함된 경우
        return !title.matches(".*[가-힣].*") && title.matches(".*[a-zA-Z].*");
    }
} 