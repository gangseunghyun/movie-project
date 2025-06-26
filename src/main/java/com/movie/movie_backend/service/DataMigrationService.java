package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.Cast;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.repository.CastRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.service.KobisApiService;
import com.movie.movie_backend.service.TmdbPopularMovieService;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.constant.RoleType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private final JdbcTemplate jdbcTemplate;
    private final CastRepository castRepository;
    private final PRDMovieRepository movieRepository;
    private final PRDMovieListRepository movieListRepository;
    private final PRDDirectorRepository directorRepository;
    private final PRDActorRepository actorRepository;
    private final KobisApiService kobisApiService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MovieDetailMapper movieDetailMapper;
    private final TmdbPopularMovieService tmdbPopularMovieService;

    @Transactional
    public void migrateMovieActorToCast() {
        log.info("Starting migration from movie_actor to casts table...");
        
        try {
            // 기존 movie_actor 테이블의 데이터 조회
            String sql = "SELECT movie_id, actor_id FROM movie_actor";
            List<Map<String, Object>> movieActors = jdbcTemplate.queryForList(sql);
            
            int migratedCount = 0;
            for (Map<String, Object> movieActor : movieActors) {
                Long movieId = ((Number) movieActor.get("movie_id")).longValue();
                Long actorId = ((Number) movieActor.get("actor_id")).longValue();
                
                // Cast 엔티티 생성 (기본값: 조연, 크레딧 순서는 순차적으로)
                Cast cast = Cast.builder()
                    .movieDetail(MovieDetail.builder().movieCd(String.valueOf(movieId)).build())
                    .actor(Actor.builder().id(actorId).build())
                    .roleType(RoleType.SUPPORTING) // 기본값을 조연으로 설정
                    .characterName(null) // 나중에 수동으로 입력
                    .orderInCredits(++migratedCount) // 순차적으로 번호 부여
                    .build();
                
                castRepository.save(cast);
            }
            
            log.info("Migration completed. {} records migrated.", migratedCount);
            
        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);
            throw new RuntimeException("Migration failed", e);
        }
    }

    @Transactional
    public void dropOldMovieActorTable() {
        log.info("Dropping old movie_actor table...");
        
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS movie_actor");
            log.info("Old movie_actor table dropped successfully.");
        } catch (Exception e) {
            log.error("Failed to drop movie_actor table: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to drop old table", e);
        }
    }

    /**
     * KOBIS API에서 영화 상세정보를 가져와서 DB에 동기화
     */
    @Transactional
    public void syncMovieDetailsFromKobis(List<String> movieCodes) {
        log.info("KOBIS API에서 영화 상세정보 동기화 시작: {}개 영화", movieCodes.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String movieCode : movieCodes) {
            try {
                MovieDetail movieDetail = kobisApiService.fetchAndSaveMovieDetail(movieCode);
                log.info("영화 상세정보 동기화 성공: {} - {}", movieCode, movieDetail.getMovieNm());
                successCount++;
                
                // API 호출 제한 고려 (초당 10회 제한)
                Thread.sleep(200);
                
            } catch (Exception e) {
                log.error("영화 상세정보 동기화 실패: {}", movieCode, e);
                failCount++;
            }
        }
        
        log.info("KOBIS API 상세정보 동기화 완료: 성공={}, 실패={}", successCount, failCount);
    }

    /**
     * 특정 영화 상세정보 업데이트
     */
    @Transactional
    public void updateMovieDetailFromKobis(String movieCode) {
        try {
            log.info("영화 상세정보 업데이트 시작: {}", movieCode);
            
            // 기존 영화 상세정보 삭제 (있는 경우)
            movieRepository.findByMovieCd(movieCode)
                    .ifPresent(movieDetail -> {
                        List<Cast> casts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(movieDetail.getMovieCd());
                        castRepository.deleteAll(casts);
                        movieRepository.delete(movieDetail);
                    });
            
            // 새 정보로 저장
            MovieDetail movieDetail = kobisApiService.fetchAndSaveMovieDetail(movieCode);
            log.info("영화 상세정보 업데이트 완료: {} - {}", movieCode, movieDetail.getMovieNm());
            
        } catch (Exception e) {
            log.error("영화 상세정보 업데이트 실패: {}", movieCode, e);
            throw new RuntimeException("영화 상세정보 업데이트 실패", e);
        }
    }

    /**
     * DB 정리 (테스트용)
     */
    @Transactional
    public void cleanupDatabase() {
        log.info("데이터베이스 정리 시작");
        
        castRepository.deleteAll();
        movieRepository.deleteAll();
        movieListRepository.deleteAll();
        directorRepository.deleteAll();
        actorRepository.deleteAll();
        
        log.info("데이터베이스 정리 완료");
    }

    /**
     * 현재 DB에 저장된 영화 상세정보 목록 조회
     */
    public List<MovieDetail> getAllMovieDetails() {
        return movieRepository.findAll();
    }

    /**
     * 현재 DB에 저장된 영화목록 조회
     */
    public List<MovieList> getAllMovieLists() {
        return movieListRepository.findAll();
    }

    /**
     * 영화 상세정보 개수 조회
     */
    public long getMovieDetailCount() {
        return movieRepository.count();
    }

    /**
     * 영화목록 개수 조회
     */
    public long getMovieListCount() {
        return movieListRepository.count();
    }

    /**
     * MovieList는 있지만 MovieDetail이 없는 영화들 찾기
     */
    public List<String> findMovieListWithoutDetail() {
        List<String> movieCdsWithoutDetail = new ArrayList<>();
        
        List<MovieList> allMovieLists = movieListRepository.findAll();
        for (MovieList movieList : allMovieLists) {
            if (!movieRepository.existsById(movieList.getMovieCd())) {
                movieCdsWithoutDetail.add(movieList.getMovieCd());
            }
        }
        
        log.info("MovieDetail이 없는 MovieList: {}개", movieCdsWithoutDetail.size());
        return movieCdsWithoutDetail;
    }

    /**
     * 누락된 MovieDetail 채워넣기
     */
    @Transactional
    public void fillMissingMovieDetails() {
        List<String> missingMovieCds = findMovieListWithoutDetail();
        
        if (missingMovieCds.isEmpty()) {
            log.info("누락된 MovieDetail이 없습니다.");
            return;
        }
        
        log.info("누락된 MovieDetail {}개 채워넣기 시작", missingMovieCds.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String movieCd : missingMovieCds) {
            try {
                MovieList movieList = movieListRepository.findById(movieCd).orElse(null);
                if (movieList == null) {
                    log.warn("MovieList를 찾을 수 없음: {}", movieCd);
                    failCount++;
                    continue;
                }
                
                log.info("누락된 MovieDetail 채워넣기 시도: {} ({})", movieList.getMovieNm(), movieCd);
                
                // KOBIS API로 MovieDetail 가져오기
                MovieDetail movieDetail = kobisApiService.fetchAndSaveMovieDetail(movieCd);
                
                if (movieDetail != null) {
                    successCount++;
                    log.info("MovieDetail 채워넣기 성공: {} ({})", movieList.getMovieNm(), movieCd);
                } else {
                    failCount++;
                    log.warn("MovieDetail 채워넣기 실패: {} ({})", movieList.getMovieNm(), movieCd);
                }
                
                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);
                
            } catch (Exception e) {
                failCount++;
                log.error("MovieDetail 채워넣기 실패: {} - {}", movieCd, e.getMessage());
            }
        }
        
        log.info("누락된 MovieDetail 채워넣기 완료: 성공={}, 실패={}", successCount, failCount);
    }

    /**
     * 현재 데이터 상태 확인
     */
    public void checkDataStatus() {
        long movieListCount = movieListRepository.count();
        long movieDetailCount = movieRepository.count();
        List<String> missingMovieCds = findMovieListWithoutDetail();
        
        log.info("=== 데이터 상태 확인 ===");
        log.info("MovieList 개수: {}", movieListCount);
        log.info("MovieDetail 개수: {}", movieDetailCount);
        log.info("누락된 MovieDetail 개수: {}", missingMovieCds.size());
        
        if (!missingMovieCds.isEmpty()) {
            log.info("누락된 MovieDetail 목록:");
            for (String movieCd : missingMovieCds) {
                MovieList movieList = movieListRepository.findById(movieCd).orElse(null);
                if (movieList != null) {
                    log.info("  - {} ({})", movieList.getMovieNm(), movieCd);
                }
            }
        }
    }

    /**
     * 테스트 영화 상세정보 등록
     */
    @Transactional
    public MovieDetail registerTestMovieDetail(MovieDetail movieDetail) {
        return movieRepository.save(movieDetail);
    }

    /**
     * 테스트 영화목록 등록
     */
    @Transactional
    public MovieList registerTestMovieList(MovieList movieList) {
        return movieListRepository.save(movieList);
    }

    /**
     * 기존 영화 데이터에 인기 영화 50개 추가 (안전한 방식)
     */
    @Transactional
    public void replaceWithPopularMovies() {
        try {
            log.info("인기 영화 50개 추가 시작 (기존 데이터 보존)");
            
            // 1. TMDB에서 인기 영화 50개 가져오기 (이미 KOBIS 매칭 시도됨)
            List<MovieDetailDto> popularMovies = tmdbPopularMovieService.getPopularMovies(50);
            log.info("TMDB에서 인기 영화 {}개 가져오기 완료", popularMovies.size());
            
            // 2. MovieList와 MovieDetail에 저장 (기존 데이터는 건너뛰기)
            int successCount = 0;
            int skippedCount = 0;
            
            for (MovieDetailDto movieDto : popularMovies) {
                try {
                    // 기존 데이터가 있으면 건너뛰기
                    if (movieListRepository.findById(movieDto.getMovieCd()).isPresent()) {
                        skippedCount++;
                        log.info("기존 데이터 존재하여 건너뛰기: {} ({})", movieDto.getMovieNm(), movieDto.getMovieCd());
                        continue;
                    }
                    
                    // MovieList 엔티티로 변환하여 저장 (기본 정보)
                    MovieList movieList = MovieList.builder()
                            .movieCd(movieDto.getMovieCd())
                            .movieNm(movieDto.getMovieNm())
                            .movieNmEn(movieDto.getMovieNmEn())
                            .openDt(movieDto.getOpenDt())
                            .genreNm(movieDto.getGenreNm())
                            .nationNm(movieDto.getNationNm())
                            .status(MovieStatus.NOW_PLAYING)
                            .build();
                    
                    movieListRepository.save(movieList);
                    
                    // MovieDetail 엔티티로 변환하여 저장
                    MovieDetail movieDetail = MovieDetail.builder()
                            .movieCd(movieDto.getMovieCd())
                            .movieNm(movieDto.getMovieNm())
                            .movieNmEn(movieDto.getMovieNmEn())
                            .description(movieDto.getDescription())
                            .openDt(movieDto.getOpenDt())
                            .showTm(movieDto.getShowTm())
                            .genreNm(movieDto.getGenreNm())
                            .nationNm(movieDto.getNationNm())
                            .watchGradeNm(movieDto.getWatchGradeNm())
                            .companyNm(movieDto.getCompanyNm())
                            .totalAudience(movieDto.getTotalAudience())
                            .reservationRate(movieDto.getReservationRate())
                            .averageRating(movieDto.getAverageRating())
                            .status(MovieStatus.NOW_PLAYING)
                            .build();
                    
                    // 감독 정보가 있으면 저장
                    if (movieDto.getDirectorName() != null && !movieDto.getDirectorName().isEmpty()) {
                        Director director = saveDirector(movieDto.getDirectorName());
                        movieDetail.setDirector(director);
                    }
                    
                    movieRepository.save(movieDetail);
                    
                    successCount++;
                    log.info("영화 저장 완료: {} ({}) - MovieList, MovieDetail 모두 저장", 
                            movieList.getMovieNm(), movieList.getMovieCd());
                    
                } catch (Exception e) {
                    log.warn("영화 저장 실패: {} - {}", movieDto.getMovieNm(), e.getMessage());
                }
            }
            
            log.info("인기 영화 추가 완료: 새로 저장={}개, 건너뛴={}개 (기존 데이터)", successCount, skippedCount);
            
        } catch (Exception e) {
            log.error("인기 영화 추가 실패", e);
            throw new RuntimeException("인기 영화 추가 실패", e);
        }
    }

    /**
     * 감독 정보 저장
     */
    private Director saveDirector(String directorName) {
        // 기존 감독이 있는지 확인
        Optional<Director> existingDirector = directorRepository.findByName(directorName);
        
        if (existingDirector.isPresent()) {
            return existingDirector.get();
        }

        // 새 감독 생성
        Director director = Director.builder()
                .name(directorName)
                .build();
        
        return directorRepository.save(director);
    }

    /**
     * Cast 데이터 저장 테스트
     */
    @Transactional
    public void testCastDataInsertion() {
        log.info("Cast 데이터 저장 테스트 시작...");
        
        try {
            // 1. MovieDetail이 있는지 확인
            List<MovieDetail> movies = movieRepository.findAll();
            log.info("MovieDetail 개수: {}", movies.size());
            
            if (movies.isEmpty()) {
                log.warn("MovieDetail이 없습니다. 먼저 영화 데이터를 생성해주세요.");
                return;
            }
            
            // 2. Actor가 있는지 확인
            List<Actor> actors = actorRepository.findAll();
            log.info("Actor 개수: {}", actors.size());
            
            if (actors.isEmpty()) {
                log.warn("Actor가 없습니다. 먼저 배우 데이터를 생성해주세요.");
                return;
            }
            
            // 3. Cast 테스트 데이터 생성
            MovieDetail testMovie = movies.get(0);
            Actor testActor = actors.get(0);
            
            log.info("테스트 영화: {} (ID: {})", testMovie.getMovieNm(), testMovie.getMovieCd());
            log.info("테스트 배우: {} (ID: {})", testActor.getName(), testActor.getId());
            
            // 4. Cast 생성 및 저장
            Cast testCast = Cast.builder()
                .movieDetail(testMovie)
                .actor(testActor)
                .roleType(RoleType.LEAD)
                .characterName("테스트 캐릭터")
                .orderInCredits(1)
                .build();
            
            Cast savedCast = castRepository.save(testCast);
            log.info("Cast 저장 성공: ID={}, 영화={}, 배우={}", 
                savedCast.getId(), savedCast.getMovieDetail().getMovieNm(), savedCast.getActor().getName());
            
            // 5. 저장된 Cast 조회
            List<Cast> casts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(testMovie.getMovieCd());
            log.info("영화 {}의 Cast 개수: {}", testMovie.getMovieNm(), casts.size());
            
        } catch (Exception e) {
            log.error("Cast 데이터 저장 테스트 실패: {}", e.getMessage(), e);
        }
    }
} 
