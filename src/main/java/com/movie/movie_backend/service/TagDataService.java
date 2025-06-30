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
        
        // 0. 기존 감독/배우 태그들 삭제
        deleteOldDirectorActorTags();
        
        // 1. 기본 태그들 생성
        createBasicTags();
        
        // 2. 영화별 태그 매핑
        mapTagsToMovies();
        
        log.info("=== 태그 데이터 세팅 완료 ===");
    }

    /**
     * 기존 감독/배우 태그들 삭제
     */
    private void deleteOldDirectorActorTags() {
        log.info("기존 감독/배우 태그 삭제 시작");
        
        // 감독/배우 관련 태그들 삭제
        List<String> directorActorTags = Arrays.asList(
            "감독", "배우", "주연", "조연", "출연"
        );
        
        for (String tagName : directorActorTags) {
            tagRepository.findByName(tagName).ifPresent(tag -> {
                tagRepository.delete(tag);
                log.debug("태그 삭제: {}", tagName);
            });
        }
        
        log.info("기존 감독/배우 태그 삭제 완료");
    }

    /**
     * 기본 태그들 생성 (장르 태그만)
     */
    private void createBasicTags() {
        log.info("기본 태그 생성 시작");
        
        // 장르 태그들만 생성
        List<String> genreTags = Arrays.asList(
            "액션", "드라마", "코미디", "스릴러", "로맨스", "호러", "SF", "판타지", 
            "모험", "범죄", "전쟁", "서부극", "뮤지컬", "애니메이션", "다큐멘터리", 
            "가족", "역사", "스포츠", "음악", "공포"
        );
        
        int createdCount = 0;
        for (String tagName : genreTags) {
            if (!tagRepository.existsByName(tagName)) {
                Tag tag = new Tag();
                tag.setName(tagName);
                tagRepository.save(tag);
                createdCount++;
                log.debug("태그 생성: {}", tagName);
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
                // 이미 태그가 있으면 건너뜀
                if (movie.getTags() != null && !movie.getTags().isEmpty()) continue;
                List<Tag> movieTags = generateTagsForMovie(movie);
                if (!movieTags.isEmpty()) {
                    movie.getTags().clear();
                    movie.getTags().addAll(movieTags);
                    movieRepository.save(movie);
                    mappedCount++;
                    log.debug("영화 태그 매핑: {} -> {}개 태그", movie.getMovieNm(), movieTags.size());
                }
            } catch (Exception e) {
                log.warn("영화 태그 매핑 실패: {} - {}", movie.getMovieNm(), e.getMessage());
            }
        }
        log.info("영화별 태그 매핑 완료: {}개 영화", mappedCount);
    }

    /**
     * 영화에 맞는 태그들을 생성 (장르 태그만)
     */
    private List<Tag> generateTagsForMovie(MovieDetail movie) {
        List<Tag> tags = new ArrayList<>();
        
        // 장르 기반 태그 추가
        if (movie.getGenreNm() != null && !movie.getGenreNm().isEmpty()) {
            String genreNm = movie.getGenreNm().toLowerCase();
            
            // 각 장르별로 태그 추가
            if (genreNm.contains("액션")) {
                tagRepository.findByName("액션").ifPresent(tags::add);
            }
            if (genreNm.contains("드라마")) {
                tagRepository.findByName("드라마").ifPresent(tags::add);
            }
            if (genreNm.contains("코미디")) {
                tagRepository.findByName("코미디").ifPresent(tags::add);
            }
            if (genreNm.contains("스릴러")) {
                tagRepository.findByName("스릴러").ifPresent(tags::add);
            }
            if (genreNm.contains("로맨스")) {
                tagRepository.findByName("로맨스").ifPresent(tags::add);
            }
            if (genreNm.contains("호러")) {
                tagRepository.findByName("호러").ifPresent(tags::add);
            }
            if (genreNm.contains("sf") || genreNm.contains("sci-fi")) {
                tagRepository.findByName("SF").ifPresent(tags::add);
            }
            if (genreNm.contains("판타지")) {
                tagRepository.findByName("판타지").ifPresent(tags::add);
            }
            if (genreNm.contains("모험")) {
                tagRepository.findByName("모험").ifPresent(tags::add);
            }
            if (genreNm.contains("범죄")) {
                tagRepository.findByName("범죄").ifPresent(tags::add);
            }
            if (genreNm.contains("전쟁")) {
                tagRepository.findByName("전쟁").ifPresent(tags::add);
            }
            if (genreNm.contains("서부")) {
                tagRepository.findByName("서부극").ifPresent(tags::add);
            }
            if (genreNm.contains("뮤지컬")) {
                tagRepository.findByName("뮤지컬").ifPresent(tags::add);
            }
            if (genreNm.contains("애니메이션")) {
                tagRepository.findByName("애니메이션").ifPresent(tags::add);
            }
            if (genreNm.contains("다큐멘터리")) {
                tagRepository.findByName("다큐멘터리").ifPresent(tags::add);
            }
            if (genreNm.contains("가족")) {
                tagRepository.findByName("가족").ifPresent(tags::add);
            }
            if (genreNm.contains("역사")) {
                tagRepository.findByName("역사").ifPresent(tags::add);
            }
            if (genreNm.contains("스포츠")) {
                tagRepository.findByName("스포츠").ifPresent(tags::add);
            }
            if (genreNm.contains("음악")) {
                tagRepository.findByName("음악").ifPresent(tags::add);
            }
            if (genreNm.contains("공포")) {
                tagRepository.findByName("공포").ifPresent(tags::add);
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
        setupTagData();
    }
} 
