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

    // 영화 특징 태그들 (실제 유용한 태그들)
    private static final List<String> FEATURE_TAGS = Arrays.asList(
        // 평점/인기도 기반
        "명작", "신작", "인기", "추천", "감동적인", "재미있는",
        
        // 영화 형태
        "애니메이션", "3D", "IMAX", "4DX", "시리즈물", "리메이크", 
        "프리퀄", "시퀄", "스핀오프",
        
        // 관람 대상
        "어린이영화", "가족영화", "청소년영화", "성인영화",
        
        // 러닝타임
        "장편영화", "단편영화", "미니시리즈",
        
        // 특별한 특징
        "아카데미 수상작", "평론가 추천", "박스오피스 1위",
        "독립영화", "할리우드", "한국영화", "유럽영화",
        
        // 장르 특징 (genreNm에 없는 추가 정보)
        "액션 블록버스터", "감성 드라마", "로맨틱 코미디",
        "스릴러", "호러", "다큐멘터리", "뮤지컬",
        "특수효과", "액션신", "로맨스", "사회비판",
        "역사물", "전쟁영화", "범죄물", "스포츠영화", 
        "음악영화", "공포영화", "미스터리", "판타지", "SF"
    );

    // 연도 태그들 (최근 10년)
    private static final List<String> YEAR_TAGS = Arrays.asList(
        "2024년", "2023년", "2022년", "2021년", "2020년", 
        "2019년", "2018년", "2017년", "2016년", "2015년"
    );

    // 국가 태그들
    private static final List<String> COUNTRY_TAGS = Arrays.asList(
        "한국", "미국", "일본", "중국", "영국", "프랑스", "독일", 
        "이탈리아", "스페인", "캐나다", "호주", "인도", "태국", "홍콩"
    );

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
        
        // 삭제할 감독/배우 태그들
        List<String> tagsToDelete = Arrays.asList(
            "봉준호", "박찬욱", "김지운", "류승완", "나홍진", "윤제균",
            "크리스토퍼 놀란", "스티븐 스필버그", "제임스 카메론", "마틴 스코세이지",
            "쿠엔틴 타란티노", "데이빗 핀처", "피터 잭슨", "조지 루카스",
            "송강호", "김윤석", "이병헌", "최민식", "한석규", "설경구", "류승룡",
            "레오나르도 디카프리오", "톰 행크스", "브래드 피트", "조니 뎁",
            "톰 크루즈", "윌 스미스", "로버트 다우니 주니어", "크리스 헴스워스"
        );
        
        int deletedCount = 0;
        for (String tagName : tagsToDelete) {
            Optional<Tag> tag = tagRepository.findByName(tagName);
            if (tag.isPresent()) {
                tagRepository.delete(tag.get());
                deletedCount++;
                log.debug("감독/배우 태그 삭제: {}", tagName);
            }
        }
        
        log.info("기존 감독/배우 태그 삭제 완료: {}개", deletedCount);
    }

    /**
     * 기본 태그들을 생성
     */
    private void createBasicTags() {
        log.info("기본 태그 생성 시작");
        
        List<String> allTags = new ArrayList<>();
        allTags.addAll(YEAR_TAGS);
        allTags.addAll(COUNTRY_TAGS);
        allTags.addAll(FEATURE_TAGS);
        
        int createdCount = 0;
        for (String tagName : allTags) {
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
     * 영화별 태그 매핑
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
     * 영화에 맞는 태그들을 생성
     */
    private List<Tag> generateTagsForMovie(MovieDetail movie) {
        List<Tag> tags = new ArrayList<>();
        
        // 1. 연도 태그 추가
        if (movie.getPrdtYear() != null && !movie.getPrdtYear().isEmpty()) {
            String yearTag = movie.getPrdtYear() + "년";
            Optional<Tag> tag = tagRepository.findByName(yearTag);
            tag.ifPresent(tags::add);
        }
        
        // 2. 영화 특징 기반 태그 추가
        addFeatureTags(movie, tags);
        
        return tags;
    }

    /**
     * 국가 문자열을 파싱
     */
    private List<String> parseCountries(String nationNm) {
        if (nationNm == null || nationNm.isEmpty()) {
            return new ArrayList<>();
        }
        
        // "한국,미국" -> ["한국", "미국"]
        return Arrays.stream(nationNm.split(","))
                .map(String::trim)
                .filter(country -> !country.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 영화 특징에 따른 태그 추가
     */
    private void addFeatureTags(MovieDetail movie, List<Tag> tags) {
        // 개봉일 기준으로 "신작" 태그 추가
        if (movie.getOpenDt() != null) {
            int currentYear = java.time.LocalDate.now().getYear();
            int movieYear = movie.getOpenDt().getYear();
            
            if (currentYear - movieYear <= 1) {
                tagRepository.findByName("신작").ifPresent(tags::add);
            }
        }
        
        // 평점이 높은 영화에 "명작" 태그 추가
        if (movie.getRatings() != null && !movie.getRatings().isEmpty()) {
            double avgRating = movie.getRatings().stream()
                    .mapToDouble(rating -> rating.getScore())
                    .average()
                    .orElse(0.0);
            
            if (avgRating >= 4.0) {
                tagRepository.findByName("명작").ifPresent(tags::add);
            }
        }
        
        // 관객수가 많은 영화에 "인기" 태그 추가
        if (movie.getTotalAudience() > 1000000) { // 100만명 이상
            tagRepository.findByName("인기").ifPresent(tags::add);
        }
        
        // 러닝타임 기반 태그
        if (movie.getShowTm() >= 120) { // 2시간 이상
            tagRepository.findByName("장편영화").ifPresent(tags::add);
        } else if (movie.getShowTm() <= 60) { // 1시간 이하
            tagRepository.findByName("단편영화").ifPresent(tags::add);
        }
        
        // 장르 기반 특징 태그 추가 (genreNm에 없는 추가 정보)
        if (movie.getGenreNm() != null && !movie.getGenreNm().isEmpty()) {
            String genreNm = movie.getGenreNm().toLowerCase();
            
            // 애니메이션
            if (genreNm.contains("애니메이션")) {
                tagRepository.findByName("애니메이션").ifPresent(tags::add);
                tagRepository.findByName("가족영화").ifPresent(tags::add);
            }
            
            // 가족 영화
            if (genreNm.contains("가족")) {
                tagRepository.findByName("가족영화").ifPresent(tags::add);
            }
            
            // 액션
            if (genreNm.contains("액션")) {
                tagRepository.findByName("액션 블록버스터").ifPresent(tags::add);
                tagRepository.findByName("액션신").ifPresent(tags::add);
            }
            
            // 드라마
            if (genreNm.contains("드라마")) {
                tagRepository.findByName("감성 드라마").ifPresent(tags::add);
            }
            
            // 코미디 + 로맨스
            if (genreNm.contains("코미디") && genreNm.contains("로맨스")) {
                tagRepository.findByName("로맨틱 코미디").ifPresent(tags::add);
            }
            
            // 로맨스
            if (genreNm.contains("로맨스")) {
                tagRepository.findByName("로맨스").ifPresent(tags::add);
            }
            
            // 스포츠
            if (genreNm.contains("스포츠")) {
                tagRepository.findByName("스포츠영화").ifPresent(tags::add);
            }
            
            // 음악/뮤지컬
            if (genreNm.contains("음악") || genreNm.contains("뮤지컬")) {
                tagRepository.findByName("음악영화").ifPresent(tags::add);
            }
            
            // 전쟁
            if (genreNm.contains("전쟁")) {
                tagRepository.findByName("전쟁영화").ifPresent(tags::add);
            }
            
            // 범죄
            if (genreNm.contains("범죄")) {
                tagRepository.findByName("범죄물").ifPresent(tags::add);
            }
            
            // 역사
            if (genreNm.contains("역사")) {
                tagRepository.findByName("역사물").ifPresent(tags::add);
            }
            
            // 호러/공포
            if (genreNm.contains("호러") || genreNm.contains("공포")) {
                tagRepository.findByName("공포영화").ifPresent(tags::add);
            }
            
            // 스릴러
            if (genreNm.contains("스릴러")) {
                tagRepository.findByName("스릴러").ifPresent(tags::add);
            }
            
            // 미스터리
            if (genreNm.contains("미스터리")) {
                tagRepository.findByName("미스터리").ifPresent(tags::add);
            }
            
            // 판타지
            if (genreNm.contains("판타지")) {
                tagRepository.findByName("판타지").ifPresent(tags::add);
            }
            
            // SF
            if (genreNm.contains("sf") || genreNm.contains("sci-fi")) {
                tagRepository.findByName("SF").ifPresent(tags::add);
            }
        }
        
        // 국가 기반 특징 태그 (nationNm에 없는 추가 정보)
        if (movie.getNationNm() != null && !movie.getNationNm().isEmpty()) {
            String nationNm = movie.getNationNm().toLowerCase();
            
            if (nationNm.contains("한국")) {
                tagRepository.findByName("한국영화").ifPresent(tags::add);
            }
            
            if (nationNm.contains("미국")) {
                tagRepository.findByName("할리우드").ifPresent(tags::add);
            }
            
            if (nationNm.contains("프랑스") || nationNm.contains("독일") || 
                nationNm.contains("이탈리아") || nationNm.contains("스페인")) {
                tagRepository.findByName("유럽영화").ifPresent(tags::add);
            }
        }
        
        // 관람등급 기반 태그
        if (movie.getWatchGradeNm() != null) {
            String grade = movie.getWatchGradeNm().toLowerCase();
            if (grade.contains("전체")) {
                tagRepository.findByName("가족영화").ifPresent(tags::add);
            } else if (grade.contains("청소년")) {
                tagRepository.findByName("청소년영화").ifPresent(tags::add);
            } else if (grade.contains("성인")) {
                tagRepository.findByName("성인영화").ifPresent(tags::add);
            }
        }
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
