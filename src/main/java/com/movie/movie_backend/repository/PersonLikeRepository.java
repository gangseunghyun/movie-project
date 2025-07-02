package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.PersonLike;
import com.movie.movie_backend.constant.PersonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonLikeRepository extends JpaRepository<PersonLike, Long> {
    
    // 사용자가 특정 배우를 좋아요 했는지 확인
    Optional<PersonLike> findByUserIdAndActorId(Long userId, Long actorId);
    
    // 사용자가 특정 감독을 좋아요 했는지 확인
    Optional<PersonLike> findByUserIdAndDirectorId(Long userId, Long directorId);
    
    // 배우의 좋아요 개수
    long countByActorId(Long actorId);
    
    // 감독의 좋아요 개수
    long countByDirectorId(Long directorId);
    
    // 사용자가 좋아요한 배우 목록 (최신순)
    @Query("SELECT pl FROM PersonLike pl WHERE pl.user.id = :userId AND pl.personType = 'ACTOR' ORDER BY pl.createdAt DESC")
    List<PersonLike> findActorLikesByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 사용자가 좋아요한 감독 목록 (최신순)
    @Query("SELECT pl FROM PersonLike pl WHERE pl.user.id = :userId AND pl.personType = 'DIRECTOR' ORDER BY pl.createdAt DESC")
    List<PersonLike> findDirectorLikesByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 사용자가 좋아요한 인물 개수 (배우)
    long countByUserIdAndPersonType(Long userId, PersonType personType);
    
    // 특정 배우를 좋아요한 사용자 수
    @Query("SELECT COUNT(pl) FROM PersonLike pl WHERE pl.actor.id = :actorId")
    long countByActorIdQuery(@Param("actorId") Long actorId);
    
    // 특정 감독을 좋아요한 사용자 수
    @Query("SELECT COUNT(pl) FROM PersonLike pl WHERE pl.director.id = :directorId")
    long countByDirectorIdQuery(@Param("directorId") Long directorId);
    
    // 사용자가 좋아요한 인물 목록 (타입별, 최신순)
    List<PersonLike> findByUserIdAndPersonTypeOrderByCreatedAtDesc(Long userId, PersonType personType);
    
    // 특정 배우의 좋아요 개수 (타입별)
    @Query("SELECT COUNT(pl) FROM PersonLike pl WHERE pl.actor = :actor AND pl.personType = :personType")
    long countByActorAndPersonType(@Param("actor") com.movie.movie_backend.entity.Actor actor, @Param("personType") PersonType personType);
    
    // 특정 감독의 좋아요 개수 (타입별)
    @Query("SELECT COUNT(pl) FROM PersonLike pl WHERE pl.director = :director AND pl.personType = :personType")
    long countByDirectorAndPersonType(@Param("director") com.movie.movie_backend.entity.Director director, @Param("personType") PersonType personType);
} 