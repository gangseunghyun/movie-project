package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.PersonLike;
import com.movie.movie_backend.entity.User;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.constant.PersonType;
import com.movie.movie_backend.repository.PersonLikeRepository;
import com.movie.movie_backend.repository.USRUserRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonLikeService {
    
    private final PersonLikeRepository personLikeRepository;
    private final USRUserRepository userRepository;
    private final PRDActorRepository actorRepository;
    private final PRDDirectorRepository directorRepository;

    /**
     * 배우 좋아요 추가
     */
    @Transactional
    public void likeActor(Long userId, Long actorId) {
        log.info("배우 좋아요 추가: 사용자={}, 배우={}", userId, actorId);
        
        // 이미 좋아요를 눌렀는지 확인
        Optional<PersonLike> existingLike = personLikeRepository.findByUserIdAndActorId(userId, actorId);
        if (existingLike.isPresent()) {
            throw new RuntimeException("이미 이 배우를 좋아요했습니다.");
        }

        // 사용자와 배우 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        Actor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new RuntimeException("배우를 찾을 수 없습니다: " + actorId));

        // 좋아요 생성
        PersonLike personLike = PersonLike.createActorLike(user, actor);
        personLikeRepository.save(personLike);
        
        log.info("배우 좋아요 추가 완료: ID={}", personLike.getId());
    }

    /**
     * 감독 좋아요 추가
     */
    @Transactional
    public void likeDirector(Long userId, Long directorId) {
        log.info("감독 좋아요 추가: 사용자={}, 감독={}", userId, directorId);
        
        // 이미 좋아요를 눌렀는지 확인
        Optional<PersonLike> existingLike = personLikeRepository.findByUserIdAndDirectorId(userId, directorId);
        if (existingLike.isPresent()) {
            throw new RuntimeException("이미 이 감독을 좋아요했습니다.");
        }

        // 사용자와 감독 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        Director director = directorRepository.findById(directorId)
                .orElseThrow(() -> new RuntimeException("감독을 찾을 수 없습니다: " + directorId));

        // 좋아요 생성
        PersonLike personLike = PersonLike.createDirectorLike(user, director);
        personLikeRepository.save(personLike);
        
        log.info("감독 좋아요 추가 완료: ID={}", personLike.getId());
    }

    /**
     * 배우 좋아요 취소
     */
    @Transactional
    public void unlikeActor(Long userId, Long actorId) {
        log.info("배우 좋아요 취소: 사용자={}, 배우={}", userId, actorId);
        
        PersonLike personLike = personLikeRepository.findByUserIdAndActorId(userId, actorId)
                .orElseThrow(() -> new RuntimeException("좋아요를 찾을 수 없습니다."));
        
        personLikeRepository.delete(personLike);
        log.info("배우 좋아요 취소 완료");
    }

    /**
     * 감독 좋아요 취소
     */
    @Transactional
    public void unlikeDirector(Long userId, Long directorId) {
        log.info("감독 좋아요 취소: 사용자={}, 감독={}", userId, directorId);
        
        PersonLike personLike = personLikeRepository.findByUserIdAndDirectorId(userId, directorId)
                .orElseThrow(() -> new RuntimeException("좋아요를 찾을 수 없습니다."));
        
        personLikeRepository.delete(personLike);
        log.info("감독 좋아요 취소 완료");
    }

    /**
     * 사용자가 배우를 좋아요 했는지 확인
     */
    public boolean hasUserLikedActor(Long userId, Long actorId) {
        return personLikeRepository.findByUserIdAndActorId(userId, actorId).isPresent();
    }

    /**
     * 사용자가 감독을 좋아요 했는지 확인
     */
    public boolean hasUserLikedDirector(Long userId, Long directorId) {
        return personLikeRepository.findByUserIdAndDirectorId(userId, directorId).isPresent();
    }

    /**
     * 배우의 좋아요 개수 조회
     */
    public long getActorLikeCount(Long actorId) {
        return personLikeRepository.countByActorId(actorId);
    }

    /**
     * 감독의 좋아요 개수 조회
     */
    public long getDirectorLikeCount(Long directorId) {
        return personLikeRepository.countByDirectorId(directorId);
    }

    /**
     * 사용자가 좋아요한 배우 목록 조회
     */
    public List<PersonLike> getUserActorLikes(Long userId) {
        return personLikeRepository.findActorLikesByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자가 좋아요한 감독 목록 조회
     */
    public List<PersonLike> getUserDirectorLikes(Long userId) {
        return personLikeRepository.findDirectorLikesByUserIdOrderByCreatedAtDesc(userId);
    }
} 