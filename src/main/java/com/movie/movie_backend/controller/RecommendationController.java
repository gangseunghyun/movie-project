package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.RecommendationDto;
import com.movie.movie_backend.service.PersonalizedRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.repository.USRUserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations/personalized")
@RequiredArgsConstructor
public class RecommendationController {
    private final PersonalizedRecommendationService recommendationService;
    private final USRUserRepository userRepository;

    @GetMapping("/liked-people")
    public List<RecommendationDto> recommendByLikedPeople(
        @AuthenticationPrincipal Object principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = null;
        if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else if (principal instanceof DefaultOAuth2User) {
            // providerId 또는 sub, email 등에서 유저 매핑 (예시: email)
            Object email = ((DefaultOAuth2User) principal).getAttribute("email");
            // 실제 서비스에 맞게 email로 DB에서 userId 조회
            if (email != null) {
                User user = userRepository.findByEmail(email.toString()).orElse(null);
                if (user != null) userId = user.getId();
            }
        }
        if (userId == null) throw new RuntimeException("로그인 유저 식별 불가");
        return recommendationService.recommendByLikedPeople(userId, page, size);
    }

    @GetMapping("/clear-cache")
    public String clearCache(@AuthenticationPrincipal Object principal) {
        Long userId = null;
        if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else if (principal instanceof DefaultOAuth2User) {
            Object email = ((DefaultOAuth2User) principal).getAttribute("email");
            if (email != null) {
                User user = userRepository.findByEmail(email.toString()).orElse(null);
                if (user != null) userId = user.getId();
            }
        }
        if (userId == null) throw new RuntimeException("로그인 유저 식별 불가");
        
        recommendationService.evictUserRecommendations(userId);
        return "캐시가 비워졌습니다. userId=" + userId;
    }
} 