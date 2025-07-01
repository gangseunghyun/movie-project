package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.Like;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.service.MovieManagementService;
import com.movie.movie_backend.service.USRUserService;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieManagementController {

    private final MovieManagementService movieManagementService;
    private final USRUserService userService;
    private final USRUserRepository userRepository;

    /**
     * 현재 인증된 사용자 정보를 가져오는 헬퍼 메서드
     */
    private User getCurrentUser(HttpServletRequest request) {
        log.info("=== getCurrentUser 호출됨 ===");
        
        // 세션에서 직접 사용자 정보 확인
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionLoginId = (String) session.getAttribute("USER_LOGIN_ID");
            log.info("세션에서 USER_LOGIN_ID: {}", sessionLoginId);
            
            if (sessionLoginId != null) {
                User sessionUser = userRepository.findByLoginId(sessionLoginId).orElse(null);
                if (sessionUser != null) {
                    log.info("세션에서 사용자 조회 성공: {}", sessionUser.getLoginId());
                    return sessionUser;
                }
            }
        }
        
        // Spring Security Authentication에서 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authentication: {}", authentication);
        
        if (authentication == null) {
            log.error("Authentication이 null입니다.");
            return null;
        }
        
        log.info("Authentication Principal: {}", authentication.getPrincipal());
        log.info("Authentication Principal Type: {}", authentication.getPrincipal().getClass().getName());
        log.info("Authentication Name: {}", authentication.getName());
        log.info("Authentication isAuthenticated: {}", authentication.isAuthenticated());
        
        if (!authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            log.error("인증되지 않은 사용자입니다.");
            return null;
        }
        
        User user = null;
        
        // OAuth2 사용자인 경우
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
            log.info("OAuth2 사용자로 인식됨");
            org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = 
                (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
            
            String email = oauth2User.getAttribute("email");
            String provider = oauth2User.getAttribute("provider");
            String providerId = oauth2User.getAttribute("providerId");
            
            // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
            if (email == null && "KAKAO".equals(provider)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                }
            }
            
            log.info("OAuth2 속성 - email: {}, provider: {}, providerId: {}", email, provider, providerId);
            
            if (email != null && provider != null && providerId != null) {
                try {
                    com.movie.movie_backend.constant.Provider providerEnum = 
                        com.movie.movie_backend.constant.Provider.valueOf(provider.toUpperCase());
                    user = userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
                    log.info("OAuth2 사용자 조회 결과: {}", user);
                } catch (Exception e) {
                    log.error("OAuth2 사용자 조회 실패", e);
                }
            }
        }
        // Spring Security로 로그인한 사용자인 경우 (User 엔티티가 Principal)
        else if (authentication.getPrincipal() instanceof User) {
            log.info("Spring Security User 엔티티로 인식됨");
            user = (User) authentication.getPrincipal();
            log.info("Spring Security 사용자 조회 결과: {}", user);
        }
        // Spring Security의 UserDetails 구현체인 경우
        else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            log.info("Spring Security UserDetails로 인식됨");
            String loginId = authentication.getName();
            user = userRepository.findByLoginId(loginId).orElse(null);
            log.info("UserDetails로 사용자 조회 결과: {}", user);
        }
        // 기타 경우 (loginId로 조회)
        else {
            log.info("기타 타입으로 인식됨: {}", authentication.getPrincipal().getClass().getName());
            String loginId = authentication.getName();
            user = userRepository.findByLoginId(loginId).orElse(null);
            log.info("loginId로 사용자 조회 결과: {}", user);
        }
        
        if (user == null) {
            log.error("최종적으로 사용자를 찾을 수 없습니다.");
        } else {
            log.info("최종 사용자: id={}, loginId={}, role={}, isAdmin={}", 
                user.getId(), user.getLoginId(), user.getRole(), user.isAdmin());
        }
        
        return user;
    }

    /**
     * 영화 등록 (관리자만)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMovie(
            @RequestBody MovieDetailDto movieDto,
            HttpServletRequest request) {
        
        try {
            log.info("=== 사용자 인증 확인 시작 ===");
            User currentUser = getCurrentUser(request);
            
            if (currentUser == null) {
                log.error("로그인되지 않은 사용자");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            log.info("사용자 권한 확인: isAdmin={}", currentUser.isAdmin());
            if (!currentUser.isAdmin()) {
                log.error("관리자가 아닌 사용자: {}", currentUser.getLoginId());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(403).body(response);
            }
            
            log.info("=== 영화 등록 서비스 호출 시작 ===");
            log.info("영화 등록 요청: {} (관리자: {})", movieDto.getMovieNm(), currentUser.getLoginId());
            log.info("MovieManagementService.createMovie() 호출 시작");
            
            MovieDetail savedMovie = movieManagementService.createMovie(movieDto);
            
            log.info("MovieManagementService.createMovie() 완료: {}", savedMovie.getMovieCd());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "영화가 성공적으로 등록되었습니다.");
            response.put("movieCd", savedMovie.getMovieCd());
            
            log.info("응답 반환: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("영화 등록 데이터 검증 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "데이터 검증 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("영화 등록 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "영화 등록에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화 수정 (관리자만)
     */
    @PutMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> updateMovie(
            @PathVariable String movieCd,
            @RequestBody MovieDetailDto movieDto,
            HttpServletRequest request) {
        
        try {
            User currentUser = getCurrentUser(request);
            
            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            if (!currentUser.isAdmin()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(403).body(response);
            }
            
            log.info("영화 수정 요청: {} - {} (관리자: {})", movieCd, movieDto.getMovieNm(), currentUser.getLoginId());
            MovieDetail updatedMovie = movieManagementService.updateMovie(movieCd, movieDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "영화가 성공적으로 수정되었습니다.");
            response.put("movieCd", updatedMovie.getMovieCd());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("영화 수정 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "영화 수정에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화 삭제 (관리자만)
     */
    @DeleteMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> deleteMovie(
            @PathVariable String movieCd,
            HttpServletRequest request) {
        
        try {
            User currentUser = getCurrentUser(request);
            
            if (currentUser == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }
            
            if (!currentUser.isAdmin()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(403).body(response);
            }
            
            log.info("영화 삭제 요청: {} (관리자: {})", movieCd, currentUser.getLoginId());
            movieManagementService.deleteMovie(movieCd);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "영화가 성공적으로 삭제되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("영화 삭제 실패", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "영화 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 영화 찜 (일반 사용자)
     */
    @PostMapping("/{movieCd}/like")
    public ResponseEntity<Map<String, Object>> likeMovie(@PathVariable String movieCd, HttpServletRequest request) {
        try {
            log.info("영화 찜 요청: {}", movieCd);
            
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }
            
            movieManagementService.likeMovie(movieCd, currentUser.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "찜이 추가되었습니다."
            ));
        } catch (Exception e) {
            log.error("영화 찜 실패: {}", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "찜 추가에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 찜 취소 (일반 사용자)
     */
    @DeleteMapping("/{movieCd}/like")
    public ResponseEntity<Map<String, Object>> unlikeMovie(@PathVariable String movieCd, HttpServletRequest request) {
        try {
            log.info("영화 찜 취소 요청: {}", movieCd);
            
            User currentUser = getCurrentUser(request);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }
            
            movieManagementService.unlikeMovie(movieCd, currentUser.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "찜이 취소되었습니다."
            ));
        } catch (Exception e) {
            log.error("영화 찜 취소 실패: {}", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "찜 취소에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 상세 정보 조회 (모든 사용자)
     */
    @GetMapping("/{movieCd}")
    public ResponseEntity<?> getMovieDetail(@PathVariable String movieCd, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        return ResponseEntity.ok(movieManagementService.getMovieDetail(movieCd, currentUser));
    }

    @GetMapping("/detail-dto")
    public ResponseEntity<?> getMovieDetailDtos(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        return ResponseEntity.ok(movieManagementService.getMovieDetailDtos(currentUser));
    }
} 
