package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagDataService {

    private final PRDTagRepository tagRepository;
    private final PRDMovieRepository movieRepository;

    /**
     * 기본 태그들을 생성하고 영화별 태그 매핑을 수행
     */
    @Transactional
    public void setupTagData() {
        log.info("=== 태그 데이터 세팅 시작 ===");
        
        // 1. 기본 태그들 생성 (기존 태그는 그대로 두고 새로운 태그만 추가)
        createBasicTags();
        
        // 2. 영화별 태그 매핑
        mapTagsToMovies();
        
        log.info("=== 태그 데이터 세팅 완료 ===");
    }

    /**
     * 기본 태그들 생성 (실제 영화 데이터 기반)
     */
    private void createBasicTags() {
        log.info("기본 태그 생성 시작");
        
        // 실제 영화 데이터에서 장르 추출하여 태그 생성
        List<MovieDetail> allMovies = movieRepository.findAll();
        Set<String> actualGenres = new HashSet<>();
        
        log.info("전체 영화 수: {}", allMovies.size());
        
        for (MovieDetail movie : allMovies) {
            if (movie.getGenreNm() != null && !movie.getGenreNm().isEmpty()) {
                String[] genres = movie.getGenreNm().split(",");
                for (String genre : genres) {
                    String trimmedGenre = genre.trim();
                    if (!trimmedGenre.isEmpty()) {
                        actualGenres.add(trimmedGenre);
                        log.debug("장르 발견: {} (영화: {})", trimmedGenre, movie.getMovieNm());
                    }
                }
            } else {
                log.debug("장르가 없는 영화: {} (genreNm: {})", movie.getMovieNm(), movie.getGenreNm());
            }
        }
        
        log.info("발견된 장르들: {}", actualGenres);
        
        int createdCount = 0;
        for (String genreName : actualGenres) {
            if (!tagRepository.existsByName(genreName)) {
                Tag tag = new Tag();
                tag.setName(genreName);
                tagRepository.save(tag);
                createdCount++;
                log.info("태그 생성: {}", genreName);
            } else {
                log.debug("이미 존재하는 태그: {}", genreName);
            }
        }
        
        log.info("기본 태그 생성 완료: {}개 생성", createdCount);
    }

    /**
     * 영화별 태그 매핑 (장르 태그만)
     */
    private void mapTagsToMovies() {
        log.info("영화별 태그 매핑 시작");
        List<MovieDetail> allMovies = movieRepository.findAll();
        int mappedCount = 0;
        for (MovieDetail movie : allMovies) {
            try {
                // 기존 태그 제거하고 새로 매핑 (강제 업데이트)
                List<Tag> movieTags = generateTagsForMovie(movie);
                if (!movieTags.isEmpty()) {
                    // tags가 null이면 초기화
                    if (movie.getTags() == null) {
                        movie.setTags(new ArrayList<>());
                    }
                    movie.getTags().clear();
                    movie.getTags().addAll(movieTags);
                    movieRepository.save(movie);
                    mappedCount++;
                    log.debug("영화 태그 매핑: {} -> {}개 태그", movie.getMovieNm(), movieTags.size());
                } else {
                    log.debug("영화에 매핑할 태그 없음: {} (장르: {})", movie.getMovieNm(), movie.getGenreNm());
                }
            } catch (Exception e) {
                log.warn("영화 태그 매핑 실패: {} - {}", movie.getMovieNm(), e.getMessage());
            }
        }
        log.info("영화별 태그 매핑 완료: {}개 영화", mappedCount);
    }

    /**
     * 영화에 맞는 태그들을 생성 (실제 데이터 기반)
     */
    private List<Tag> generateTagsForMovie(MovieDetail movie) {
        List<Tag> tags = new ArrayList<>();
        
        // 장르 기반 태그 추가
        if (movie.getGenreNm() != null && !movie.getGenreNm().isEmpty()) {
            String[] genres = movie.getGenreNm().split(",");
            for (String genre : genres) {
                String genreName = genre.trim();
                // 실제 생성된 태그에서 찾기
                tagRepository.findByName(genreName).ifPresent(tags::add);
            }
        }
        
        return tags;
    }

    /**
     * 특정 영화의 태그들을 조회
     */
    public List<Tag> getTagsForMovie(String movieCd) {
        return tagRepository.findTagsByMovieCd(movieCd);
    }

    /**
     * 인기 태그들을 조회
     */
    public List<Tag> getPopularTags() {
        List<Object[]> results = tagRepository.findPopularTags();
        return results.stream()
                .limit(10) // 상위 10개만
                .map(result -> (Tag) result[0])
                .collect(Collectors.toList());
    }

    /**
     * 태그명으로 태그 검색
     */
    public List<Tag> searchTagsByName(String name) {
        return tagRepository.findByNameContainingIgnoreCase(name);
    }

    @PostConstruct
    public void init() {
        try {
            log.info("TagDataService 초기화 시작");
            setupTagData();
            log.info("TagDataService 초기화 완료");
        } catch (Exception e) {
            log.error("TagDataService 초기화 실패: {}", e.getMessage(), e);
        }
    }
} 
