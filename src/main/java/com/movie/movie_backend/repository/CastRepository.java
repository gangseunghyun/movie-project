package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Cast;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.constant.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CastRepository extends JpaRepository<Cast, Long> {
    
    // 특정 영화의 모든 배우 조회 (크레딧 순서대로)
    List<Cast> findByMovieDetailMovieCdOrderByOrderInCreditsAsc(String movieCd);
    
    // 특정 영화의 주연 배우만 조회
    List<Cast> findByMovieDetailMovieCdAndRoleTypeOrderByOrderInCreditsAsc(String movieCd, RoleType roleType);
    
    // 특정 영화의 주연 배우 조회
    @Query("SELECT c FROM Cast c WHERE c.movieDetail.movieCd = :movieCd AND c.roleType = :roleType ORDER BY c.orderInCredits ASC")
    List<Cast> findLeadActorsByMovieCd(@Param("movieCd") String movieCd, @Param("roleType") RoleType roleType);
    
    // 특정 영화의 조연 배우 조회
    @Query("SELECT c FROM Cast c WHERE c.movieDetail.movieCd = :movieCd AND c.roleType = :roleType ORDER BY c.orderInCredits ASC")
    List<Cast> findSupportingActorsByMovieCd(@Param("movieCd") String movieCd, @Param("roleType") RoleType roleType);
    
    // 특정 배우가 출연한 모든 영화 조회 (개봉일 순으로 내림차순)
    List<Cast> findByActorIdOrderByMovieDetailOpenDtDesc(Long actorId);
    
    // 특정 배우의 주연 작품만 조회 (개봉일 순으로 내림차순)
    List<Cast> findByActorIdAndRoleTypeOrderByMovieDetailOpenDtDesc(Long actorId, RoleType roleType);
    
    // 특정 영화와 배우로 Cast 조회
    Cast findByMovieDetailAndActor(MovieDetail movieDetail, Actor actor);
} 
