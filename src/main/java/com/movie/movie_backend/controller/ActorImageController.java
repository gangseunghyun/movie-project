package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.repository.PRDActorRepository;
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
@RequestMapping("/api/admin/actors")
@RequiredArgsConstructor
public class ActorImageController {
    private final FileUploadService fileUploadService;
    private final PRDActorRepository actorRepository;

    @PostMapping("/{actorId}/photo")
    public ResponseEntity<Map<String, Object>> uploadActorPhoto(
            @PathVariable Long actorId,
            @RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        Actor actor = actorRepository.findById(actorId)
            .orElseThrow(() -> new IllegalArgumentException("배우 없음"));

        // 기존 이미지 삭제
        if (actor.getPhotoUrl() != null) {
            fileUploadService.deleteImage(actor.getPhotoUrl(), "actors");
        }
        // 새 이미지 업로드
        String imageUrl;
        try {
            imageUrl = fileUploadService.uploadImage(file, actorId.toString(), "actors");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        actor.setPhotoUrl(imageUrl);
        actorRepository.save(actor);

        response.put("success", true);
        response.put("imageUrl", imageUrl);
        return ResponseEntity.ok(response);
    }
} 