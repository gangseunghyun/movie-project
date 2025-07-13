package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.repository.PRDDirectorRepository;
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
@RequestMapping("/api/admin/directors")
@RequiredArgsConstructor
public class DirectorImageController {
    private final FileUploadService fileUploadService;
    private final PRDDirectorRepository directorRepository;

    @PostMapping("/{directorId}/photo")
    public ResponseEntity<Map<String, Object>> uploadDirectorPhoto(
            @PathVariable Long directorId,
            @RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        Director director = directorRepository.findById(directorId)
            .orElseThrow(() -> new IllegalArgumentException("감독 없음"));

        // 기존 이미지 삭제
        if (director.getPhotoUrl() != null) {
            fileUploadService.deleteImage(director.getPhotoUrl(), "directors");
        }
        // 새 이미지 업로드
        String imageUrl;
        try {
            imageUrl = fileUploadService.uploadImage(file, directorId.toString(), "directors");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        director.setPhotoUrl(imageUrl);
        directorRepository.save(director);

        response.put("success", true);
        response.put("imageUrl", imageUrl);
        return ResponseEntity.ok(response);
    }
} 