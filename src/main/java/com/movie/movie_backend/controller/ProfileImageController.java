package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.service.FileUploadService;
import com.movie.movie_backend.repository.USRUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileImageController {

    private final FileUploadService fileUploadService;
    private final USRUserRepository userRepository;

    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("image") MultipartFile file,
            @AuthenticationPrincipal Object principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        User user = extractUserFromPrincipal(principal);
        if (user == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            log.info("프로필 이미지 업로드 요청: userId={}, filename={}", user.getId(), file.getOriginalFilename());
            
            // 기존 프로필 이미지 삭제
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                fileUploadService.deleteProfileImage(user.getProfileImageUrl());
            }
            
            // 새 이미지 업로드
            String imageUrl = fileUploadService.uploadProfileImage(file, user.getId());
            
            // 사용자 정보 업데이트
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);
            
            response.put("success", true);
            response.put("message", "프로필 이미지가 업로드되었습니다.");
            response.put("imageUrl", imageUrl);
            
            log.info("프로필 이미지 업로드 완료: userId={}, imageUrl={}", user.getId(), imageUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("프로필 이미지 업로드 실패 (유효성 검사): {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "프로필 이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 프로필 이미지 삭제
     */
    @DeleteMapping("/delete-image")
    public ResponseEntity<Map<String, Object>> deleteProfileImage(@AuthenticationPrincipal Object principal) {
        Map<String, Object> response = new HashMap<>();
        
        User user = extractUserFromPrincipal(principal);
        if (user == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            log.info("프로필 이미지 삭제 요청: userId={}", user.getId());
            
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                // 파일 삭제
                fileUploadService.deleteProfileImage(user.getProfileImageUrl());
                
                // 사용자 정보 업데이트
                user.setProfileImageUrl(null);
                userRepository.save(user);
                
                response.put("success", true);
                response.put("message", "프로필 이미지가 삭제되었습니다.");
                
                log.info("프로필 이미지 삭제 완료: userId={}", user.getId());
            } else {
                response.put("success", false);
                response.put("message", "삭제할 프로필 이미지가 없습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("프로필 이미지 삭제 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "프로필 이미지 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 프로필 이미지 파일 조회
     */
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String filename) {
        try {
            // 파일 경로 설정 (application.properties에서 설정한 경로 사용)
            Path filePath = Paths.get("/uploads/profile-images").resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // 또는 적절한 이미지 타입
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (MalformedURLException e) {
            log.error("프로필 이미지 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // User 추출 유틸리티
    private User extractUserFromPrincipal(Object principal) {
        if (principal == null) return null;
        if (principal instanceof User) return (User) principal;
        if (principal instanceof UserDetails) {
            Object userObj = ((UserDetails) principal).getUsername();
            // 필요시 userRepository에서 조회
            if (userObj instanceof String) {
                return userRepository.findByLoginId((String) userObj).orElse(null);
            }
        }
        if (principal instanceof OAuth2User) {
            String email = ((OAuth2User) principal).getAttribute("email");
            if (email != null) {
                return userRepository.findByEmail(email).orElse(null);
            }
        }
        return null;
    }
} 