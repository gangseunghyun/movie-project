package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.Like;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.service.MovieManagementService;
import com.movie.movie_backend.service.USRUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieManagementController {

    private final MovieManagementService movieManagementService;
    private final USRUserService userService;

    /**
     * 영화 등록
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMovie(@RequestBody MovieDetailDto movieDto) {
        try {
            log.info("영화 등록 요청: {}", movieDto.getMovieNm());
            MovieDetail savedMovie = movieManagementService.createMovie(movieDto);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "영화가 성공적으로 등록되었습니다.",
                "movieCd", savedMovie.getMovieCd()
            ));
        } catch (Exception e) {
            log.error("영화 등록 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "영화 등록에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 수정
     */
    @PutMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> updateMovie(
            @PathVariable String movieCd,
            @RequestBody MovieDetailDto movieDto) {
        try {
            log.info("영화 수정 요청: {} - {}", movieCd, movieDto.getMovieNm());
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
     * 영화 삭제
     */
    @DeleteMapping("/{movieCd}")
    public ResponseEntity<Map<String, Object>> deleteMovie(@PathVariable String movieCd) {
        try {
            log.info("영화 삭제 요청: {}", movieCd);
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
     * 영화 좋아요
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
     * 영화 상세 정보 조회
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
