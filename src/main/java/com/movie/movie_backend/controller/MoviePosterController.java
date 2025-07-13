package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class MoviePosterController {
    private final FileUploadService fileUploadService;
    private final PRDMovieListRepository movieListRepository;

    @PostMapping("/{movieCd}/poster")
    public ResponseEntity<Map<String, Object>> uploadPoster(
            @PathVariable String movieCd,
            @RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        MovieList movie = movieListRepository.findById(movieCd)
            .orElseThrow(() -> new IllegalArgumentException("영화 없음"));

        // 기존 포스터 삭제
        if (movie.getPosterUrl() != null) {
            fileUploadService.deleteImage(movie.getPosterUrl(), "posters");
        }
        // 새 포스터 업로드
        String imageUrl;
        try {
            imageUrl = fileUploadService.uploadImage(file, movieCd, "posters");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        movie.setPosterUrl(imageUrl);
        movieListRepository.save(movie);

        response.put("success", true);
        response.put("imageUrl", imageUrl);
        return ResponseEntity.ok(response);
    }
} 