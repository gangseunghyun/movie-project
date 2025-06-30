package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.dto.AdminMovieDto;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDTagRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.entity.MovieList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private final PRDMovieRepository movieRepository;
    private final PRDTagRepository tagRepository;
    private final TmdbRatingService tmdbRatingService;
    private final DataMigrationService dataMigrationService;
    private final TagDataService tagDataService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PRDMovieListRepository movieListRepository;
    
    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    /**
     * 영화 등록 (DTO 사용)
     */
    @Transactional
    public AdminMovieDto registerMovie(AdminMovieDto movieDto) {
        log.info("영화 등록 시작: {}", movieDto.getMovieNm());
        
        // DTO를 Entity로 변환
        MovieDetail movieDetail = convertToEntity(movieDto);
        
        // 영화 코드 중복 체크
        if (movieRepository.existsByMovieCd(movieDetail.getMovieCd())) {
            throw new RuntimeException("이미 존재하는 영화입니다: " + movieDetail.getMovieCd());
        }
        
        // 기본값 설정
        if (movieDetail.getStatus() == null) {
            movieDetail.setStatus(MovieStatus.COMING_SOON); // 상영예정
        }
        
        MovieDetail savedMovie = movieRepository.save(movieDetail);
        log.info("영화 등록 완료: {} ({})", savedMovie.getMovieNm(), savedMovie.getMovieCd());
        
        return convertToDto(savedMovie);
    }

    /**
     * 영화 정보 수정 (DTO 사용)
     */
    @Transactional
    public AdminMovieDto updateMovie(String movieCd, AdminMovieDto updateDto) {
        log.info("영화 정보 수정 시작: {}", movieCd);
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        
        // DTO에서 업데이트할 데이터 추출
        if (updateDto.getMovieNm() != null) {
            movie.setMovieNm(updateDto.getMovieNm());
        }
        if (updateDto.getMovieNmEn() != null) {
            movie.setMovieNmEn(updateDto.getMovieNmEn());
        }
        if (updateDto.getPrdtYear() != null) {
            movie.setPrdtYear(updateDto.getPrdtYear());
        }
        if (updateDto.getShowTm() > 0) {
            movie.setShowTm(updateDto.getShowTm());
        }
        if (updateDto.getOpenDt() != null) {
            movie.setOpenDt(updateDto.getOpenDt());
        }
        if (updateDto.getPrdtStatNm() != null) {
            movie.setPrdtStatNm(updateDto.getPrdtStatNm());
        }
        if (updateDto.getTypeNm() != null) {
            movie.setTypeNm(updateDto.getTypeNm());
        }
        if (updateDto.getGenreNm() != null) {
            movie.setGenreNm(updateDto.getGenreNm());
        }
        if (updateDto.getNationNm() != null) {
            movie.setNationNm(updateDto.getNationNm());
        }
        if (updateDto.getWatchGradeNm() != null) {
            movie.setWatchGradeNm(updateDto.getWatchGradeNm());
        }
        if (updateDto.getCompanyNm() != null) {
            movie.setCompanyNm(updateDto.getCompanyNm());
        }
        if (updateDto.getDescription() != null) {
            movie.setDescription(updateDto.getDescription());
        }
        if (updateDto.getStatus() != null) {
            movie.setStatus(updateDto.getStatus());
        }
        
        // 태그 업데이트
        if (updateDto.getTagNames() != null) {
            setMovieTags(movieCd, updateDto.getTagNames());
        }
        
        MovieDetail updatedMovie = movieRepository.save(movie);
        log.info("영화 정보 수정 완료: {} ({})", updatedMovie.getMovieNm(), movieCd);
        
        return convertToDto(updatedMovie);
    }

    /**
     * 영화 비활성화
     */
    @Transactional
    public MovieDetail deactivateMovie(String movieCd) {
        log.info("영화 비활성화 시작: {}", movieCd);
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        movie.setStatus(MovieStatus.ENDED); // 상영종료로 변경
        
        MovieDetail deactivatedMovie = movieRepository.save(movie);
        log.info("영화 비활성화 완료: {} ({})", deactivatedMovie.getMovieNm(), movieCd);
        
        return deactivatedMovie;
    }

    /**
     * 영화 활성화
     */
    @Transactional
    public MovieDetail activateMovie(String movieCd) {
        log.info("영화 활성화 시작: {}", movieCd);
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        movie.setStatus(MovieStatus.NOW_PLAYING); // 상영중으로 변경
        
        MovieDetail activatedMovie = movieRepository.save(movie);
        log.info("영화 활성화 완료: {} ({})", activatedMovie.getMovieNm(), movieCd);
        
        return activatedMovie;
    }

    /**
     * 영화 상태 업데이트
     */
    @Transactional
    public MovieDetail updateMovieStatus(String movieCd, MovieStatus newStatus) {
        log.info("영화 상태 업데이트 시작: {} -> {}", movieCd, newStatus);
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        movie.setStatus(newStatus);
        
        MovieDetail updatedMovie = movieRepository.save(movie);
        log.info("영화 상태 업데이트 완료: {} ({}) -> {}", 
            updatedMovie.getMovieNm(), movieCd, newStatus);
        
        return updatedMovie;
    }

    /**
     * 영화 태그 설정
     */
    @Transactional
    public MovieDetail setMovieTags(String movieCd, List<String> tagNames) {
        log.info("영화 태그 설정 시작: {} -> {}개 태그", movieCd, tagNames.size());
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        
        // 기존 태그 제거
        movie.getTags().clear();
        
        // 새로운 태그들 추가
        for (String tagName : tagNames) {
            Optional<Tag> tag = tagRepository.findByName(tagName);
            if (tag.isPresent()) {
                movie.getTags().add(tag.get());
            } else {
                // 태그가 없으면 새로 생성
                Tag newTag = new Tag();
                newTag.setName(tagName);
                Tag savedTag = tagRepository.save(newTag);
                movie.getTags().add(savedTag);
                log.info("새 태그 생성: {}", tagName);
            }
        }
        
        MovieDetail updatedMovie = movieRepository.save(movie);
        log.info("영화 태그 설정 완료: {} -> {}개 태그", movieCd, updatedMovie.getTags().size());
        
        return updatedMovie;
    }

    /**
     * 영화 태그 추가
     */
    @Transactional
    public MovieDetail addMovieTag(String movieCd, String tagName) {
        log.info("영화 태그 추가: {} -> {}", movieCd, tagName);
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        
        // 태그 찾기 또는 생성
        Optional<Tag> tag = tagRepository.findByName(tagName);
        Tag targetTag;
        if (tag.isPresent()) {
            targetTag = tag.get();
        } else {
            targetTag = new Tag();
            targetTag.setName(tagName);
            targetTag = tagRepository.save(targetTag);
            log.info("새 태그 생성: {}", tagName);
        }
        
        // 이미 있는 태그인지 확인
        boolean alreadyExists = movie.getTags().stream()
                .anyMatch(t -> t.getName().equals(tagName));
        
        if (!alreadyExists) {
            movie.getTags().add(targetTag);
            movieRepository.save(movie);
            log.info("영화 태그 추가 완료: {} -> {}", movieCd, tagName);
        } else {
            log.info("이미 존재하는 태그입니다: {} -> {}", movieCd, tagName);
        }
        
        return movie;
    }

    /**
     * 영화 태그 제거
     */
    @Transactional
    public MovieDetail removeMovieTag(String movieCd, String tagName) {
        log.info("영화 태그 제거: {} -> {}", movieCd, tagName);
        
        Optional<MovieDetail> existingMovie = movieRepository.findByMovieCd(movieCd);
        if (existingMovie.isEmpty()) {
            throw new RuntimeException("존재하지 않는 영화입니다: " + movieCd);
        }
        
        MovieDetail movie = existingMovie.get();
        
        // 태그 제거
        movie.setTags(movie.getTags().stream()
                .filter(tag -> !tag.getName().equals(tagName))
                .collect(Collectors.toList()));
        
        movieRepository.save(movie);
        log.info("영화 태그 제거 완료: {} -> {}", movieCd, tagName);
        
        return movie;
    }

    /**
     * 모든 영화 조회 (관리자용)
     */
    public List<MovieDetail> getAllMovies() {
        return movieRepository.findAll();
    }

    /**
     * 상태별 영화 조회
     */
    public List<MovieDetail> getMoviesByStatus(MovieStatus status) {
        return movieRepository.findByStatus(status);
    }

    /**
     * 영화명으로 검색
     */
    public List<MovieDetail> searchMoviesByName(String movieNm) {
        return movieRepository.findByMovieNmContainingIgnoreCase(movieNm);
    }

    /**
     * 모든 태그 조회
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    /**
     * 태그명으로 검색
     */
    public List<Tag> searchTagsByName(String name) {
        return tagRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * 기존 장르 태그들을 삭제하고 새로운 태그들로 교체
     */
    @Transactional
    public void resetTags() {
        log.info("태그 초기화 시작");
        
        // 1. 기존 장르 태그들 삭제
        List<String> oldGenreTags = Arrays.asList(
            "액션", "드라마", "코미디", "스릴러", "로맨스", "호러", "SF", "판타지", 
            "모험", "범죄", "전쟁", "서부극", "뮤지컬", "애니메이션", "다큐멘터리", 
            "가족", "역사", "스포츠", "음악", "공포"
        );
        
        int deletedCount = 0;
        for (String tagName : oldGenreTags) {
            Optional<Tag> tag = tagRepository.findByName(tagName);
            if (tag.isPresent()) {
                tagRepository.delete(tag.get());
                deletedCount++;
                log.debug("기존 태그 삭제: {}", tagName);
            }
        }
        log.info("기존 장르 태그 삭제 완료: {}개", deletedCount);
        
        // 2. 새로운 태그들 생성
        tagDataService.setupTagData();
        
        log.info("태그 초기화 완료");
    }

    /**
     * 장르 중복 현황 확인
     */
    public Map<String, Object> checkGenreDuplicates() {
        log.info("장르 중복 현황 확인 시작");
        
        List<Object[]> duplicateGenres = movieRepository.findDuplicateGenres();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalMovies", movieRepository.count());
        result.put("uniqueGenres", duplicateGenres.size());
        
        List<Map<String, Object>> genreStats = new ArrayList<>();
        for (Object[] row : duplicateGenres) {
            String genreNm = (String) row[0];
            Long count = (Long) row[1];
            
            Map<String, Object> genreInfo = new HashMap<>();
            genreInfo.put("genreNm", genreNm);
            genreInfo.put("count", count);
            
            // 해당 장르의 영화들 예시
            List<MovieDetail> movies = movieRepository.findByExactGenreNm(genreNm);
            List<String> movieExamples = movies.stream()
                    .limit(5) // 상위 5개만
                    .map(MovieDetail::getMovieNm)
                    .collect(Collectors.toList());
            genreInfo.put("examples", movieExamples);
            
            genreStats.add(genreInfo);
        }
        
        result.put("genreStats", genreStats);
        
        log.info("장르 중복 현황 확인 완료: {}개 장르", duplicateGenres.size());
        return result;
    }

    /**
     * 기존 영화들의 장르를 TMDB에서 가져와서 업데이트
     */
    @Transactional
    public Map<String, Object> updateMovieGenresFromTmdb() {
        log.info("TMDB에서 장르 정보 업데이트 시작");
        
        List<MovieDetail> allMovies = movieRepository.findAll();
        int updatedCount = 0;
        int failedCount = 0;
        
        for (MovieDetail movie : allMovies) {
            try {
                // 현재 장르가 단일 장르인 경우에만 업데이트
                if (movie.getGenreNm() != null && !movie.getGenreNm().contains(",")) {
                    // TMDB에서 영화 검색하여 장르 정보 가져오기
                    String newGenres = getGenreFromTmdb(movie.getMovieNm());
                    if (newGenres != null && !newGenres.isEmpty()) {
                        // 기존 장르와 새로운 장르를 합침
                        Set<String> allGenres = new HashSet<>();
                        
                        // 기존 장르 추가
                        allGenres.add(movie.getGenreNm().trim());
                        
                        // 새로운 장르들 추가
                        String[] newGenreArray = newGenres.split(",");
                        for (String genre : newGenreArray) {
                            allGenres.add(genre.trim());
                        }
                        
                        // 합쳐진 장르들을 쉼표로 구분하여 저장
                        String updatedGenreNm = String.join(",", allGenres);
                        movie.setGenreNm(updatedGenreNm);
                        movieRepository.save(movie);
                        updatedCount++;
                        log.debug("장르 업데이트: {} -> {}", movie.getMovieNm(), updatedGenreNm);
                    }
                }
            } catch (Exception e) {
                failedCount++;
                log.warn("장르 업데이트 실패: {} - {}", movie.getMovieNm(), e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalMovies", allMovies.size());
        result.put("updatedCount", updatedCount);
        result.put("failedCount", failedCount);
        
        log.info("TMDB에서 장르 정보 업데이트 완료: 업데이트={}, 실패={}", updatedCount, failedCount);
        return result;
    }

    /**
     * TMDB에서 영화명으로 장르 정보 가져오기
     */
    private String getGenreFromTmdb(String movieName) {
        // TMDB 검색 API 호출하여 장르 정보 가져오기
        // 실제 구현은 TMDB API 호출 로직 필요
        return null; // 임시 반환값
    }

    /**
     * 모든 영화 조회 (DTO 사용)
     */
    public List<AdminMovieDto> getAllMoviesAsDto() {
        return movieRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 영화 조회 (DTO 사용)
     */
    public List<AdminMovieDto> getMoviesByStatusAsDto(MovieStatus status) {
        return movieRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 영화명으로 검색 (DTO 사용)
     */
    public List<AdminMovieDto> searchMoviesByNameAsDto(String movieNm) {
        return movieRepository.findByMovieNmContainingIgnoreCase(movieNm).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * DTO를 Entity로 변환
     */
    private MovieDetail convertToEntity(AdminMovieDto dto) {
        MovieDetail movie = new MovieDetail();
        
        // 기본 정보 복사
        movie.setMovieCd(dto.getMovieCd());
        movie.setMovieNm(dto.getMovieNm());
        movie.setMovieNmEn(dto.getMovieNmEn());
        movie.setPrdtYear(dto.getPrdtYear());
        movie.setShowTm(dto.getShowTm());
        movie.setOpenDt(dto.getOpenDt());
        movie.setPrdtStatNm(dto.getPrdtStatNm());
        movie.setTypeNm(dto.getTypeNm());
        movie.setGenreNm(dto.getGenreNm());
        movie.setNationNm(dto.getNationNm());
        movie.setWatchGradeNm(dto.getWatchGradeNm());
        movie.setCompanyNm(dto.getCompanyNm());
        movie.setDescription(dto.getDescription());
        movie.setStatus(dto.getStatus());
        
        return movie;
    }

    /**
     * Entity를 DTO로 변환
     */
    public AdminMovieDto convertToDto(MovieDetail movie) {
        AdminMovieDto dto = new AdminMovieDto();
        
        // 기본 정보 복사
        dto.setMovieCd(movie.getMovieCd());
        dto.setMovieNm(movie.getMovieNm());
        dto.setMovieNmEn(movie.getMovieNmEn());
        dto.setPrdtYear(movie.getPrdtYear());
        dto.setShowTm(movie.getShowTm());
        dto.setOpenDt(movie.getOpenDt());
        dto.setPrdtStatNm(movie.getPrdtStatNm());
        dto.setTypeNm(movie.getTypeNm());
        dto.setGenreNm(movie.getGenreNm());
        dto.setNationNm(movie.getNationNm());
        dto.setWatchGradeNm(movie.getWatchGradeNm());
        dto.setCompanyNm(movie.getCompanyNm());
        dto.setDescription(movie.getDescription());
        dto.setStatus(movie.getStatus());
        
        // 태그 정보
        List<String> tagNames = movie.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        dto.setTagNames(tagNames);
        
        return dto;
    }

    /**
     * 평균 별점이 높은 영화 조회 (DTO 사용)
     */
    public List<AdminMovieDto> getTopRatedMoviesAsDto(int limit) {
        return tmdbRatingService.getTopRatedMovies(limit).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 기존 영화 데이터 정리 후 인기 영화 100개로 교체
     */
    @Transactional
    public void replaceWithPopularMovies() {
        log.info("기존 영화 데이터 정리 및 인기 영화 교체 시작");
        
        // DataMigrationService의 replaceWithPopularMovies 메서드 사용
        dataMigrationService.replaceWithPopularMovies();
        
        log.info("인기 영화 교체 완료");
    }

    /**
     * 기존 영화 데이터의 영어 제목과 장르 정보를 TMDB에서 보완
     */
    @Transactional
    public Map<String, Object> updateMovieEnglishTitlesAndGenres() {
        log.info("기존 영화 데이터의 영어 제목과 장르 정보 보완 시작");
        
        List<MovieDetail> allMovies = movieRepository.findAll();
        int updatedCount = 0;
        int failedCount = 0;
        
        for (MovieDetail movie : allMovies) {
            try {
                boolean needsUpdate = false;
                String movieNmEn = movie.getMovieNmEn();
                String genreNm = movie.getGenreNm();
                
                // 영어 제목이나 장르가 없으면 TMDB에서 보완
                if ((movieNmEn == null || movieNmEn.isEmpty() || genreNm == null || genreNm.isEmpty()) 
                    && movie.getOpenDt() != null) {
                    
                    String tmdbInfo = getTmdbMovieInfo(movie.getMovieNm(), movie.getOpenDt());
                    if (tmdbInfo != null) {
                        String[] tmdbData = tmdbInfo.split("\\|");
                        if (tmdbData.length >= 2) {
                            if (movieNmEn == null || movieNmEn.isEmpty()) {
                                movie.setMovieNmEn(tmdbData[0]);
                                needsUpdate = true;
                            }
                            if (genreNm == null || genreNm.isEmpty()) {
                                movie.setGenreNm(tmdbData[1]);
                                needsUpdate = true;
                            }
                        }
                    }
                }
                
                if (needsUpdate) {
                    movieRepository.save(movie);
                    updatedCount++;
                    log.info("영화 정보 업데이트: {} - 영문제목: {}, 장르: {}", 
                        movie.getMovieNm(), movie.getMovieNmEn(), movie.getGenreNm());
                }
                
            } catch (Exception e) {
                failedCount++;
                log.warn("영화 정보 업데이트 실패: {} - {}", movie.getMovieNm(), e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalMovies", allMovies.size());
        result.put("updatedCount", updatedCount);
        result.put("failedCount", failedCount);
        
        log.info("영화 영어 제목 및 장르 정보 보완 완료: 업데이트={}, 실패={}", updatedCount, failedCount);
        return result;
    }

    /**
     * TMDB에서 영화 정보 가져오기 (영어 제목, 장르)
     */
    private String getTmdbMovieInfo(String movieNm, LocalDate openDt) {
        try {
            String query = java.net.URLEncoder.encode(movieNm, java.nio.charset.StandardCharsets.UTF_8);
            String year = (openDt != null) ? String.valueOf(openDt.getYear()) : null;
            
            String url = String.format("https://api.themoviedb.org/3/search/movie?api_key=%s&query=%s&language=ko-KR%s", 
                tmdbApiKey, query, year != null ? "&year=" + year : "");
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode results = rootNode.get("results");
                
                if (results != null && results.size() > 0) {
                    JsonNode movie = results.get(0);
                    String originalTitle = movie.has("original_title") ? movie.get("original_title").asText() : "";
                    
                    // 장르 정보
                    String genres = "";
                    if (movie.has("genre_ids") && movie.get("genre_ids").isArray()) {
                        List<String> genreNames = new ArrayList<>();
                        for (JsonNode genreId : movie.get("genre_ids")) {
                            String genreName = getGenreNameById(genreId.asInt());
                            if (!genreName.isEmpty()) {
                                genreNames.add(genreName);
                            }
                        }
                        genres = String.join(", ", genreNames);
                    }
                    
                    return originalTitle + "|" + genres;
                }
            }
        } catch (Exception e) {
            log.warn("TMDB 영화 정보 조회 실패: {} - {}", movieNm, e.getMessage());
        }
        
        return null;
    }

    /**
     * TMDB 장르 ID를 한국어 장르명으로 변환
     */
    private String getGenreNameById(int genreId) {
        switch (genreId) {
            case 28: return "액션";
            case 12: return "모험";
            case 16: return "애니메이션";
            case 35: return "코미디";
            case 80: return "범죄";
            case 99: return "다큐멘터리";
            case 18: return "드라마";
            case 10751: return "가족";
            case 14: return "판타지";
            case 36: return "역사";
            case 27: return "공포";
            case 10402: return "음악";
            case 9648: return "미스터리";
            case 10749: return "로맨스";
            case 878: return "SF";
            case 10770: return "TV 영화";
            case 53: return "스릴러";
            case 10752: return "전쟁";
            case 37: return "서부";
            default: return "";
        }
    }

    /**
     * 애플리케이션 시작 시 기존 영화 데이터 자동 업데이트
     */
    @Transactional
    public void updateExistingMoviesOnStartup() {
        try {
            log.info("=== 애플리케이션 시작 시 기존 영화 데이터 자동 업데이트 시작 ===");
            
            List<MovieDetail> allMovies = movieRepository.findAll();
            int updatedCount = 0;
            int failedCount = 0;
            
            for (MovieDetail movie : allMovies) {
                try {
                    boolean needsUpdate = false;
                    String movieNmEn = movie.getMovieNmEn();
                    String genreNm = movie.getGenreNm();
                    
                    // 영어 제목이나 장르가 없으면 TMDB에서 보완
                    if ((movieNmEn == null || movieNmEn.isEmpty() || genreNm == null || genreNm.isEmpty()) 
                        && movie.getOpenDt() != null) {
                        
                        String tmdbInfo = getTmdbMovieInfo(movie.getMovieNm(), movie.getOpenDt());
                        if (tmdbInfo != null) {
                            String[] tmdbData = tmdbInfo.split("\\|");
                            if (tmdbData.length >= 2) {
                                if (movieNmEn == null || movieNmEn.isEmpty()) {
                                    movie.setMovieNmEn(tmdbData[0]);
                                    needsUpdate = true;
                                }
                                if (genreNm == null || genreNm.isEmpty()) {
                                    movie.setGenreNm(tmdbData[1]);
                                    needsUpdate = true;
                                }
                            }
                        }
                    }
                    
                    if (needsUpdate) {
                        movieRepository.save(movie);
                        updatedCount++;
                        log.info("자동 업데이트: {} - 영문제목: {}, 장르: {}", 
                            movie.getMovieNm(), movie.getMovieNmEn(), movie.getGenreNm());
                    }
                    
                } catch (Exception e) {
                    failedCount++;
                    log.warn("자동 업데이트 실패: {} - {}", movie.getMovieNm(), e.getMessage());
                }
            }
            
            log.info("=== 자동 업데이트 완료: 업데이트={}, 실패={} ===", updatedCount, failedCount);
            
        } catch (Exception e) {
            log.error("자동 업데이트 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * MovieList의 genreNm, movieNmEn을 MovieDetail에서 동기화
     */
    @Transactional
    public Map<String, Object> syncMovieListWithDetail() {
        List<MovieList> movieLists = movieListRepository.findAll();
        int updatedCount = 0;
        int failedCount = 0;
        for (MovieList movieList : movieLists) {
            try {
                MovieDetail detail = movieRepository.findByMovieCd(movieList.getMovieCd()).orElse(null);
                if (detail != null) {
                    boolean needsUpdate = false;
                    if (movieList.getMovieNmEn() == null || movieList.getMovieNmEn().isEmpty()) {
                        movieList.setMovieNmEn(detail.getMovieNmEn());
                        needsUpdate = true;
                    }
                    if (movieList.getGenreNm() == null || movieList.getGenreNm().isEmpty()) {
                        movieList.setGenreNm(detail.getGenreNm());
                        needsUpdate = true;
                    }
                    if (needsUpdate) {
                        movieListRepository.save(movieList);
                        updatedCount++;
                    }
                }
            } catch (Exception e) {
                failedCount++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalMovieList", movieLists.size());
        result.put("updatedCount", updatedCount);
        result.put("failedCount", failedCount);
        return result;
    }
} 
