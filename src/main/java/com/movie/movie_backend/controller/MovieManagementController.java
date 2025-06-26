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
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieManagementController {

    private final MovieManagementService movieManagementService;
    private final USRUserService userService;
    private final USRUserRepository userRepository;

    /**
     * 영화 등록 (관리자만)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMovie(@RequestBody MovieDetailDto movieDto, HttpSession session) {
        log.info("=== 영화 등록 API 호출 시작 ===");
        log.info("받은 데이터: {}", movieDto);
        
        try {
            // 세션에서 사용자 정보 가져오기
            User currentUser = (User) session.getAttribute("user");
            log.info("세션에서 가져온 사용자: {}", currentUser);
            
            if (currentUser == null) {
                log.error("로그인되지 않은 사용자");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }
            
            log.info("사용자 권한 확인: isAdmin={}", currentUser.isAdmin());
            if (!currentUser.isAdmin()) {
                log.error("관리자가 아닌 사용자: {}", currentUser.getLoginId());
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "관리자 권한이 필요합니다."
                ));
            }
            
            log.info("영화 등록 요청: {} (관리자: {})", movieDto.getMovieNm(), currentUser.getLoginId());
            log.info("MovieManagementService.createMovie() 호출 시작");
            
            MovieDetail savedMovie = movieManagementService.createMovie(movieDto);
            
            log.info("MovieManagementService.createMovie() 완료: {}", savedMovie.getMovieCd());
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "영화가 성공적으로 등록되었습니다.",
                "movieCd", savedMovie.getMovieCd()
            );
            
            log.info("응답 반환: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("영화 등록 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "영화 등록에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 수정 (관리자만)
     */
    @PutMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> updateMovie(
            @PathVariable String movieCd,
            @RequestBody MovieDetailDto movieDto) {
        try {
            // 관리자 권한 체크
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }
            
            User currentUser = userRepository.findByLoginId(authentication.getName()).orElse(null);
            if (currentUser == null || !currentUser.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "관리자 권한이 필요합니다."
                ));
            }
            
            log.info("영화 수정 요청: {} - {} (관리자: {})", movieCd, movieDto.getMovieNm(), currentUser.getLoginId());
            MovieDetail updatedMovie = movieManagementService.updateMovie(movieCd, movieDto);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "영화가 성공적으로 수정되었습니다.",
                "movieCd", updatedMovie.getMovieCd()
            ));
        } catch (Exception e) {
            log.error("영화 수정 실패: {}", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "영화 수정에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 삭제 (관리자만)
     */
    @DeleteMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> deleteMovie(@PathVariable String movieCd) {
        try {
            // 관리자 권한 체크
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
                ));
            }
            
            User currentUser = userRepository.findByLoginId(authentication.getName()).orElse(null);
            if (currentUser == null || !currentUser.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "관리자 권한이 필요합니다."
                ));
            }
            
            log.info("영화 삭제 요청: {} (관리자: {})", movieCd, currentUser.getLoginId());
            movieManagementService.deleteMovie(movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "영화가 성공적으로 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("영화 삭제 실패: {}", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "영화 삭제에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 좋아요 (일반 사용자)
     */
    @PostMapping("/{movieCd}/like")
    public ResponseEntity<Map<String, Object>> likeMovie(@PathVariable String movieCd) {
        try {
            log.info("영화 좋아요 요청: {}", movieCd);
            
            // 임시로 첫 번째 사용자를 사용 (실제로는 로그인된 사용자 사용)
            User currentUser = userService.getAllUsers().get(0);
            
            movieManagementService.likeMovie(movieCd, currentUser.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "좋아요가 추가되었습니다."
            ));
        } catch (Exception e) {
            log.error("영화 좋아요 실패: {}", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "좋아요 추가에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 상세 정보 조회 (모든 사용자)
     */
    @GetMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> getMovieDetail(@PathVariable String movieCd) {
        try {
            log.info("영화 상세 정보 조회: {}", movieCd);
            MovieDetailDto movieDetail = movieManagementService.getMovieDetail(movieCd);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", movieDetail
            ));
        } catch (Exception e) {
            log.error("영화 상세 정보 조회 실패: {}", movieCd, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "영화 정보 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }
} 
