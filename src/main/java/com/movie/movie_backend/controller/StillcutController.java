package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Stillcut;
import com.movie.movie_backend.repository.MovieDetailRepository;
import com.movie.movie_backend.repository.StillcutRepository;
import com.movie.movie_backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class StillcutController {
    private final FileUploadService fileUploadService;
    private final MovieDetailRepository movieDetailRepository;
    private final StillcutRepository stillcutRepository;

    // 여러 장 업로드
    @PostMapping("/movies/{movieCd}/stillcuts")
    public ResponseEntity<Map<String, Object>> uploadStillcuts(
            @PathVariable String movieCd,
            @RequestParam("images") List<MultipartFile> files) {
        Map<String, Object> response = new HashMap<>();
        MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
        if (movieDetail == null) {
            throw new IllegalArgumentException("영화 상세정보 없음");
        }
        List<String> imageUrls = new ArrayList<>();
        int order = (int) stillcutRepository.countByMovieDetailId(movieDetail.getId());
        for (MultipartFile file : files) {
            try {
                String imageUrl = fileUploadService.uploadImage(file, movieCd, "stillcuts");
                Stillcut stillcut = Stillcut.builder()
                        .imageUrl(imageUrl)
                        .orderInMovie(order++)
                        .movieDetail(movieDetail)
                        .build();
                stillcutRepository.save(stillcut);
                imageUrls.add(imageUrl);
            } catch (Exception e) {
                log.error("스틸컷 업로드 실패: {}", e.getMessage());
            }
        }
        response.put("success", true);
        response.put("imageUrls", imageUrls);
        return ResponseEntity.ok(response);
    }

    // 개별 삭제
    @DeleteMapping("/stillcuts/{stillcutId}")
    public ResponseEntity<Map<String, Object>> deleteStillcut(@PathVariable Long stillcutId) {
        Map<String, Object> response = new HashMap<>();
        Optional<Stillcut> stillcutOpt = stillcutRepository.findById(stillcutId);
        if (stillcutOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "스틸컷 없음");
            return ResponseEntity.badRequest().body(response);
        }
        Stillcut stillcut = stillcutOpt.get();
        fileUploadService.deleteImage(stillcut.getImageUrl(), "stillcuts");
        stillcutRepository.delete(stillcut);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
} 