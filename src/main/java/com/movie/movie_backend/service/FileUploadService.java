package com.movie.movie_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    @Value("${app.upload.path:/uploads}")
    private String uploadPath;

    @Value("${app.upload.profile-images:/uploads/profile-images}")
    private String profileImagesPath;

    /**
     * 프로필 이미지 업로드
     * @param file 업로드할 이미지 파일
     * @param userId 사용자 ID (파일명에 포함)
     * @return 저장된 파일의 URL 경로
     */
    public String uploadProfileImage(MultipartFile file, Long userId) throws IOException {
        // 파일 유효성 검사
        validateImageFile(file);
        
        // 업로드 디렉토리 생성
        Path uploadDir = Paths.get(profileImagesPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // 파일명 생성 (중복 방지)
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = "profile_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;
        
        // 파일 저장
        Path filePath = uploadDir.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // URL 경로 반환 (프론트엔드에서 접근 가능한 경로)
        String fileUrl = "/api/profile/images/" + newFilename;
        
        log.info("프로필 이미지 업로드 완료: userId={}, filename={}, url={}", userId, newFilename, fileUrl);
        
        return fileUrl;
    }
    
    /**
     * 이미지 파일 유효성 검사
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        
        // 파일 크기 검사 (5MB 제한)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }
        
        // 파일 타입 검사
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        
        // 허용된 이미지 형식 검사
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
                throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
            }
        }
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // 기본 확장자
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 프로필 이미지 파일 삭제
     */
    public void deleteProfileImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            // URL에서 파일명 추출
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(profileImagesPath, filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("프로필 이미지 삭제 완료: {}", filename);
            }
        } catch (IOException e) {
            log.error("프로필 이미지 삭제 실패: {}", imageUrl, e);
        }
    }
} 