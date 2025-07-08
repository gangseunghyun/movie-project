package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.MovieDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieDetailRepository extends JpaRepository<MovieDetail, Long> {

    // 감독이 연출한 영화
    @Query("SELECT md FROM MovieDetail md WHERE md.director.id IN :directorIds")
    List<MovieDetail> findMoviesByDirectorIds(@Param("directorIds") List<Long> directorIds);

    // 배우가 주연으로 출연한 영화
    @Query("SELECT md FROM MovieDetail md JOIN md.casts c WHERE c.actor.id IN :actorIds AND c.roleType = :roleType")
    List<MovieDetail> findMoviesByMainActorIds(@Param("actorIds") List<Long> actorIds, @Param("roleType") com.movie.movie_backend.constant.RoleType roleType);

    @Query("SELECT md FROM MovieDetail md WHERE md.movieCd = :movieCd")
    MovieDetail findByMovieCd(@Param("movieCd") String movieCd);
} 