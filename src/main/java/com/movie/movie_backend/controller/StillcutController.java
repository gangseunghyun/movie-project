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
        
        try {
            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            if (movieDetail == null) {
                response.put("success", false);
                response.put("message", "영화를 찾을 수 없습니다: " + movieCd);
                return ResponseEntity.badRequest().body(response);
            }

            // 기존 스틸컷 개수 확인 (순서 번호 계산용)
            int existingCount = (int) stillcutRepository.countByMovieDetailId(movieDetail.getId());

            // 새 스틸컷들 저장
            List<String> imageUrls = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                try {
                    MultipartFile file = files.get(i);
                    String imageUrl = fileUploadService.uploadImage(file, movieCd, "stillcuts");
                    Stillcut stillcut = Stillcut.builder()
                            .imageUrl(imageUrl)
                            .orderInMovie(existingCount + i + 1)
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
            
        } catch (Exception e) {
            log.error("스틸컷 업로드 실패: {} - {}", movieCd, e.getMessage());
            response.put("success", false);
            response.put("message", "스틸컷 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
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

    /**
     * 스틸컷 URL 설정
     */
    @PutMapping("/movies/{movieCd}/stillcut-urls")
    public ResponseEntity<Map<String, Object>> setStillcutUrls(
            @PathVariable String movieCd,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) request.get("imageUrls");
            if (imageUrls == null || imageUrls.isEmpty()) {
                response.put("success", false);
                response.put("message", "스틸컷 URL이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            if (movieDetail == null) {
                response.put("success", false);
                response.put("message", "영화를 찾을 수 없습니다: " + movieCd);
                return ResponseEntity.badRequest().body(response);
            }

            // 기존 스틸컷 개수 확인 (순서 번호 계산용)
            int existingCount = (int) stillcutRepository.countByMovieDetailId(movieDetail.getId());

            // 새 스틸컷들 저장
            List<String> savedUrls = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                String imageUrl = imageUrls.get(i).trim();
                if (!imageUrl.isEmpty()) {
                    Stillcut stillcut = Stillcut.builder()
                            .imageUrl(imageUrl)
                            .orderInMovie(existingCount + i + 1)
                            .movieDetail(movieDetail)
                            .build();
                    stillcutRepository.save(stillcut);
                    savedUrls.add(imageUrl);
                }
            }

            response.put("success", true);
            response.put("imageUrls", savedUrls);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스틸컷 URL 설정 실패: {} - {}", movieCd, e.getMessage());
            response.put("success", false);
            response.put("message", "스틸컷 URL 설정 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 