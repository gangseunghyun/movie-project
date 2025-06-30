package com.movie.movie_backend.repository;

import com.movie.movie_backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PRDTagRepository extends JpaRepository<Tag, Long> {
    
    // 태그명으로 태그 조회
    Optional<Tag> findByName(String name);
    
    // 태그명으로 존재 여부 확인
    boolean existsByName(String name);
    
    // 태그명으로 검색 (부분 일치)
    List<Tag> findByNameContainingIgnoreCase(String name);
    
    // 특정 영화에 달린 태그들 조회
    @Query("SELECT t FROM Tag t JOIN t.movieDetails md WHERE md.movieCd = :movieCd")
    List<Tag> findTagsByMovieCd(@Param("movieCd") String movieCd);
    
    // 가장 많이 사용된 태그들 조회 (인기 태그)
    @Query("SELECT t, COUNT(md) as usageCount FROM Tag t JOIN t.movieDetails md GROUP BY t ORDER BY usageCount DESC")
    List<Object[]> findPopularTags();
    
    // 장르 태그만 조회 (고정 장르명 리스트)
    @Query("SELECT t FROM Tag t WHERE t.name IN ('액션', '드라마', '코미디', '스릴러', '로맨스', '호러', 'SF', '판타지', '모험', '범죄', '전쟁', '서부극', '뮤지컬', '애니메이션', '다큐멘터리', '가족', '역사', '스포츠', '음악', '공포') ORDER BY t.name")
    List<Tag> findGenreTags();
    
    // 특징 태그 조회 (영화의 특징적인 요소들)
    @Query("SELECT t FROM Tag t WHERE t.name IN ('명작', '신작', '인기', '추천', '감동적인', '재미있는', '애니메이션', '3D', 'IMAX', '4DX', '시리즈물', '리메이크', '프리퀄', '시퀄', '스핀오프', '어린이영화', '가족영화', '청소년영화', '성인영화', '장편영화', '단편영화', '미니시리즈', '아카데미 수상작', '평론가 추천', '박스오피스 1위', '독립영화', '할리우드', '한국영화', '유럽영화', '액션 블록버스터', '감성 드라마', '로맨틱 코미디', '특수효과', '액션신', '사회비판', '역사물', '전쟁영화', '범죄물', '스포츠영화', '음악영화', '미스터리') ORDER BY t.name")
    List<Tag> findFeatureTags();
    
    // 연도 태그 조회
    @Query("SELECT t FROM Tag t WHERE t.name LIKE '%년' ORDER BY t.name DESC")
    List<Tag> findYearTags();
    
    // 국가 태그 조회
    @Query("SELECT t FROM Tag t WHERE t.name IN ('한국', '미국', '일본', '중국', '영국', '프랑스', '독일', '이탈리아', '스페인', '캐나다', '호주', '인도', '태국', '홍콩') ORDER BY t.name")
    List<Tag> findCountryTags();
    
    // 모든 카테고리 태그 조회 (장르 + 특징 + 연도 + 국가)
    @Query("SELECT t FROM Tag t WHERE t.name IN ('액션', '드라마', '코미디', '스릴러', '로맨스', '호러', 'SF', '판타지', '모험', '범죄', '전쟁', '서부극', '뮤지컬', '애니메이션', '다큐멘터리', '가족', '역사', '스포츠', '음악', '공포', '명작', '신작', '인기', '추천', '감동적인', '재미있는', '애니메이션', '3D', 'IMAX', '4DX', '시리즈물', '리메이크', '프리퀄', '시퀄', '스핀오프', '어린이영화', '가족영화', '청소년영화', '성인영화', '장편영화', '단편영화', '미니시리즈', '아카데미 수상작', '평론가 추천', '박스오피스 1위', '독립영화', '할리우드', '한국영화', '유럽영화', '액션 블록버스터', '감성 드라마', '로맨틱 코미디', '특수효과', '액션신', '사회비판', '역사물', '전쟁영화', '범죄물', '스포츠영화', '음악영화', '미스터리', '한국', '미국', '일본', '중국', '영국', '프랑스', '독일', '이탈리아', '스페인', '캐나다', '호주', '인도', '태국', '홍콩') OR t.name LIKE '%년' ORDER BY t.name")
    List<Tag> findAllCategoryTags();
} 
