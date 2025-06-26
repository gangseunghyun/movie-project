package com.movie.movie_backend.config;

import com.movie.movie_backend.service.DataMigrationService;
import com.movie.movie_backend.service.KobisApiService;
import com.movie.movie_backend.service.PRDMovieListService;
import com.movie.movie_backend.service.TmdbPosterBatchService;
import com.movie.movie_backend.service.TmdbStillcutService;
import com.movie.movie_backend.service.TagDataService;
import com.movie.movie_backend.service.BoxOfficeService;
import com.movie.movie_backend.service.NaverMovieBatchService;
import com.movie.movie_backend.service.TmdbPopularMovieService;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.BoxOfficeRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.service.KobisPopularMovieService;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.constant.MovieStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataLoaderConfig {

    private final DataMigrationService dataMigrationService;
    private final KobisApiService kobisApiService;
    private final PRDMovieListService movieListService;
    private final TmdbPosterBatchService tmdbPosterBatchService;
    private final TmdbStillcutService tmdbStillcutService;
    private final TagDataService tagDataService;
    private final BoxOfficeService boxOfficeService;
    private final NaverMovieBatchService naverMovieBatchService;
    private final TmdbPopularMovieService tmdbPopularMovieService;
    private final PRDMovieListRepository prdMovieListRepository;
    private final PRDDirectorRepository directorRepository;
    private final KobisPopularMovieService kobisPopularMovieService;
    private final PRDMovieRepository movieRepository;
    private final BoxOfficeRepository boxOfficeRepository;

    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            log.info("=== 애플리케이션 시작 시 데이터 상태 확인 ===");
            
            try {
                // 1. 기존 데이터 개수 확인
                long existingMovieListCount = prdMovieListRepository.count();
                long existingBoxOfficeCount = boxOfficeRepository.count();
                long existingMovieDetailCount = movieRepository.count();
                log.info("기존 데이터 확인 - MovieList: {}개, MovieDetail: {}개, BoxOffice: {}개", 
                    existingMovieListCount, existingMovieDetailCount, existingBoxOfficeCount);
                
                // 2. MovieList와 MovieDetail 개수가 다르면 누락된 MovieDetail 채워넣기
                if (existingMovieListCount > existingMovieDetailCount) {
                    long missingCount = existingMovieListCount - existingMovieDetailCount;
                    log.info("MovieDetail이 {}개 누락되었습니다. 누락된 MovieDetail을 채워넣습니다.", missingCount);
                    
                    List<String> missingMovieCds = new ArrayList<>();
                    List<MovieList> allMovieLists = prdMovieListRepository.findAll();
                    for (MovieList movieList : allMovieLists) {
                        if (!movieRepository.existsById(movieList.getMovieCd())) {
                            missingMovieCds.add(movieList.getMovieCd());
                        }
                    }
                    
                    int successCount = 0;
                    int failCount = 0;
                    int attemptCount = 0;
                    int maxAttempts = 3;
                    
                    for (String movieCd : missingMovieCds) {
                        if (attemptCount >= maxAttempts) {
                            log.info("MovieDetail 채워넣기 시도 횟수 제한에 도달했습니다. (최대 {}회)", maxAttempts);
                            break;
                        }
                        
                        try {
                            MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
                            if (movieList == null) {
                                log.warn("MovieList를 찾을 수 없음: {}", movieCd);
                                failCount++;
                                continue;
                            }
                            
                            log.info("누락된 MovieDetail 채워넣기 시도: {} ({}) - 시도 {}/{}", 
                                movieList.getMovieNm(), movieCd, attemptCount + 1, maxAttempts);
                            
                            // KOBIS API로 MovieDetail 가져오기
                            MovieDetail movieDetail = kobisApiService.fetchAndSaveMovieDetail(movieCd);
                            
                            if (movieDetail != null) {
                                successCount++;
                                log.info("MovieDetail 채워넣기 성공: {} ({})", movieList.getMovieNm(), movieCd);
                            } else {
                                failCount++;
                                log.warn("MovieDetail 채워넣기 실패: {} ({})", movieList.getMovieNm(), movieCd);
                            }
                            
                            attemptCount++;
                            
                            // API 호출 제한을 위한 딜레이
                            Thread.sleep(100);
                            
                        } catch (Exception e) {
                            failCount++;
                            attemptCount++;
                            log.error("MovieDetail 채워넣기 실패: {} - {}", movieCd, e.getMessage());
                        }
                    }
                    
                    log.info("누락된 MovieDetail 채워넣기 완료: 성공={}, 실패={}", successCount, failCount);
                    
                    // 업데이트 후 개수 재확인
                    long updatedMovieDetailCount = movieRepository.count();
                    log.info("업데이트 후 MovieDetail 개수: {}개", updatedMovieDetailCount);
                }
                
                // 3. 충분한 데이터가 있으면 로딩 건너뛰기 (단, MovieDetail 채워넣기는 항상 실행)
                if (existingMovieListCount >= 200 && existingBoxOfficeCount >= 20) {
                    log.info("충분한 데이터가 있습니다 (MovieList: {}개, BoxOffice: {}개). 자동 로딩을 건너뜁니다.", 
                        existingMovieListCount, existingBoxOfficeCount);
                    log.info("필요시 수동으로 /api/admin/movies/replace-with-popular 엔드포인트를 호출하세요.");
                    
                    // 개봉예정작은 항상 확인하고 추가
                    loadComingSoonMovies();
                    
                    // 기존 개봉예정작 데이터 정리 (과거 개봉 영화들을 상영중으로 변경)
                    kobisApiService.cleanupComingSoonMovies();
                    
                    // MovieDetail 채워넣기는 항상 실행
                    fillMissingMovieDetails();
                    
                    log.info("=== 자동 데이터 로딩 완료 (충분한 데이터로 인한 건너뛰기) ===");
                    return;
                }
                
                // MovieDetail 채워넣기는 MovieList가 50개 이상이면 항상 실행
                if (existingMovieListCount >= 50) {
                    log.info("MovieList가 충분합니다 ({}개). MovieDetail 채워넣기를 실행합니다.", existingMovieListCount);
                    fillMissingMovieDetails();
                }
                
                // 4. 데이터가 부족한 경우에만 로딩 실행
                log.info("데이터가 부족합니다. 자동 로딩을 시작합니다.");
                
                // KOBIS 박스오피스에서 인기 영화 가져오기 (기존 데이터는 건너뛰기)
                List<MovieListDto> popularMovies = kobisPopularMovieService.getPopularMoviesFromBoxOffice(200);
                log.info("KOBIS 박스오피스에서 인기 영화 {}개 가져오기 완료", popularMovies.size());
                
                // MovieList에 저장 (기존 데이터는 건너뛰기)
                int successCount = 0;
                int skippedCount = 0;
                
                for (MovieListDto movieDto : popularMovies) {
                    try {
                        // 기존 데이터가 있으면 건너뛰기
                        if (prdMovieListRepository.findById(movieDto.getMovieCd()).isPresent()) {
                            skippedCount++;
                            continue;
                        }
                        
                        // MovieList 엔티티로 변환하여 저장
                        MovieList movieList = MovieList.builder()
                                .movieCd(movieDto.getMovieCd())
                                .movieNm(movieDto.getMovieNm())
                                .movieNmEn(movieDto.getMovieNmEn())
                                .openDt(movieDto.getOpenDt())
                                .genreNm(movieDto.getGenreNm())
                                .nationNm(movieDto.getNationNm())
                                .watchGradeNm(movieDto.getWatchGradeNm())
                                .posterUrl(movieDto.getPosterUrl())
                                .status(movieDto.getStatus())
                                .build();
                        
                        prdMovieListRepository.save(movieList);
                        successCount++;
                        
                        log.info("MovieList 저장 완료: {} ({})", movieDto.getMovieNm(), movieDto.getMovieCd());
                        
                    } catch (Exception e) {
                        log.warn("MovieList 저장 실패: {} - {}", movieDto.getMovieNm(), e.getMessage());
                    }
                }
                
                // 개봉예정작 가져오기
                loadComingSoonMovies();
                
                // MovieDetail 채워넣기
                fillMissingMovieDetails();
                
                log.info("=== 자동 데이터 로딩 완료 ===");
                log.info("새로 저장된 MovieList: {}개", successCount);
                log.info("건너뛴 MovieList: {}개 (기존 데이터)", skippedCount);
                
            } catch (Exception e) {
                log.error("자동 데이터 로딩 실패", e);
            }
        };
    }
    
    /**
     * 감독 정보 저장
     */
    private Director saveDirector(String directorName) {
        // 기존 감독이 있는지 확인
        java.util.Optional<Director> existingDirector = 
            dataMigrationService.getAllMovieDetails().stream()
                .filter(movie -> movie.getDirector() != null && movie.getDirector().getName().equals(directorName))
                .map(MovieDetail::getDirector)
                .findFirst();
        
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
     * 개봉예정작 자동 로딩
     */
    private void loadComingSoonMovies() {
        try {
            log.info("=== 개봉예정작 자동 로딩 시작 ===");
            
            // 먼저 기존 개봉예정작 데이터 정리 (과거 개봉 영화들을 상영중으로 변경)
            kobisApiService.cleanupComingSoonMovies();
            
            // 기존 개봉예정작 개수 확인
            long existingComingSoonCount = prdMovieListRepository.findAll().stream()
                .filter(movie -> MovieStatus.COMING_SOON.equals(movie.getStatus()))
                .count();
            log.info("정리 후 기존 개봉예정작 개수: {}개", existingComingSoonCount);
            
            // 이미 충분한 개봉예정작이 있으면 추가 로딩 안 함
            if (existingComingSoonCount >= 50) {
                log.info("기존 개봉예정작이 충분합니다 ({}개). 추가 로딩을 건너뜁니다.", existingComingSoonCount);
                return;
            }
            
            // KOBIS에서 개봉예정작 가져오기
            List<MovieListDto> comingSoonMovies = kobisApiService.fetchComingSoonMovies(200);
            log.info("KOBIS에서 개봉예정작 {}개 가져오기 완료", comingSoonMovies.size());
            
            // KOBIS + 기존 데이터가 부족하면 TMDB에서 추가로 가져오기
            long totalComingSoon = existingComingSoonCount + comingSoonMovies.size();
            if (totalComingSoon < 50) {
                log.info("KOBIS + 기존 데이터가 부족하므로 TMDB에서 추가로 가져옵니다. (현재: {}개, 목표: 50개)", totalComingSoon);
                List<MovieListDto> tmdbMovies = kobisApiService.fetchComingSoonMoviesFromTmdb(200);
                log.info("TMDB에서 개봉예정작 {}개 가져오기 완료", tmdbMovies.size());
                
                // 중복 제거하면서 합치기
                for (MovieListDto tmdbMovie : tmdbMovies) {
                    boolean isDuplicate = comingSoonMovies.stream()
                        .anyMatch(kobisMovie -> kobisMovie.getMovieNm().equals(tmdbMovie.getMovieNm()));
                    
                    if (!isDuplicate) {
                        comingSoonMovies.add(tmdbMovie);
                    }
                }
                
                log.info("KOBIS + TMDB 합계: {}개", comingSoonMovies.size());
            } else {
                log.info("KOBIS + 기존 데이터가 충분합니다. TMDB 호출을 건너뜁니다. (현재: {}개)", totalComingSoon);
            }
            
            int successCount = 0;
            int skippedCount = 0;
            
            for (MovieListDto movieDto : comingSoonMovies) {
                try {
                    // 기존 데이터가 있으면 건너뛰기
                    if (prdMovieListRepository.findById(movieDto.getMovieCd()).isPresent()) {
                        skippedCount++;
                        continue;
                    }
                    
                    // MovieList 엔티티로 변환하여 저장
                    MovieList movieList = MovieList.builder()
                            .movieCd(movieDto.getMovieCd())
                            .movieNm(movieDto.getMovieNm())
                            .movieNmEn(movieDto.getMovieNmEn())
                            .openDt(movieDto.getOpenDt())
                            .genreNm(movieDto.getGenreNm())
                            .nationNm(movieDto.getNationNm())
                            .watchGradeNm(movieDto.getWatchGradeNm())
                            .posterUrl(movieDto.getPosterUrl())
                            .status(MovieStatus.COMING_SOON) // 개봉예정 상태로 설정
                            .build();
                    
                    prdMovieListRepository.save(movieList);
                    successCount++;
                    
                    log.info("개봉예정작 저장 완료: {} ({}) - 개봉일: {}", 
                        movieDto.getMovieNm(), movieDto.getMovieCd(), movieDto.getOpenDt());
                    
                } catch (Exception e) {
                    log.warn("개봉예정작 저장 실패: {} - {}", movieDto.getMovieNm(), e.getMessage());
                }
            }
            
            log.info("=== 개봉예정작 자동 로딩 완료 ===");
            log.info("새로 저장된 개봉예정작: {}개", successCount);
            log.info("건너뛴 개봉예정작: {}개 (기존 데이터)", skippedCount);
            
        } catch (Exception e) {
            log.error("개봉예정작 자동 로딩 실패", e);
        }
    }

    /**
     * MovieDetail 채워넣기
     */
    private void fillMissingMovieDetails() {
        try {
            log.info("=== MovieDetail 채워넣기 시작 ===");
            
            // 기존 데이터 개수 확인
            long existingMovieListCount = prdMovieListRepository.count();
            long existingMovieDetailCount = movieRepository.count();
            log.info("기존 데이터 확인 - MovieList: {}개, MovieDetail: {}개", 
                existingMovieListCount, existingMovieDetailCount);
            
            // 실제로 누락된 MovieDetail 찾기
            List<String> missingMovieCds = new ArrayList<>();
            List<MovieList> allMovieLists = prdMovieListRepository.findAll();
            
            for (MovieList movieList : allMovieLists) {
                if (!movieRepository.existsById(movieList.getMovieCd())) {
                    missingMovieCds.add(movieList.getMovieCd());
                }
            }
            
            log.info("실제 누락된 MovieDetail: {}개", missingMovieCds.size());
            
            if (!missingMovieCds.isEmpty()) {
                log.info("누락된 MovieDetail을 채워넣습니다.");
                
                int successCount = 0;
                int failCount = 0;
                int attemptCount = 0;
                int maxAttempts = Math.min(missingMovieCds.size(), 50); // 최대 50개까지만 시도
                
                log.info("누락된 MovieDetail {}개 중 최대 {}개를 처리합니다.", missingMovieCds.size(), maxAttempts);
                
                for (String movieCd : missingMovieCds) {
                    if (attemptCount >= maxAttempts) {
                        log.info("MovieDetail 채워넣기 시도 횟수 제한에 도달했습니다. (최대 {}회)", maxAttempts);
                        break;
                    }
                    
                    try {
                        MovieList movieList = prdMovieListRepository.findById(movieCd).orElse(null);
                        if (movieList == null) {
                            log.warn("MovieList를 찾을 수 없음: {}", movieCd);
                            failCount++;
                            attemptCount++;
                            continue;
                        }
                        
                        log.info("누락된 MovieDetail 채워넣기 시도: {} ({}) - 시도 {}/{}", 
                            movieList.getMovieNm(), movieCd, attemptCount + 1, maxAttempts);
                        
                        // KOBIS API로 MovieDetail 가져오기
                        MovieDetail movieDetail = kobisApiService.fetchAndSaveMovieDetail(movieCd);
                        
                        if (movieDetail != null) {
                            successCount++;
                            log.info("MovieDetail 채워넣기 성공: {} ({})", movieList.getMovieNm(), movieCd);
                        } else {
                            failCount++;
                            log.warn("MovieDetail 채워넣기 실패: {} ({})", movieList.getMovieNm(), movieCd);
                        }
                        
                        attemptCount++;
                        
                        // API 호출 제한을 위한 딜레이
                        Thread.sleep(200);
                        
                    } catch (Exception e) {
                        failCount++;
                        attemptCount++;
                        log.error("MovieDetail 채워넣기 실패: {} - {}", movieCd, e.getMessage());
                    }
                }
                
                log.info("KOBIS로 누락된 MovieDetail 채워넣기 완료: 성공={}, 실패={}", successCount, failCount);
                
                // 업데이트 후 개수 재확인
                long updatedMovieDetailCount = movieRepository.count();
                log.info("KOBIS 업데이트 후 MovieDetail 개수: {}개", updatedMovieDetailCount);
                
                // KOBIS로 실패한 영화들을 TMDB로 보완
                if (failCount > 0) {
                    log.info("KOBIS로 실패한 영화 {}개를 TMDB로 보완합니다.", failCount);
                    kobisApiService.fillMissingMovieDetailsFromTmdb();
                    
                    // 최종 개수 재확인
                    long finalMovieDetailCount = movieRepository.count();
                    log.info("TMDB 보완 후 최종 MovieDetail 개수: {}개", finalMovieDetailCount);
                }
            } else {
                log.info("MovieDetail이 충분합니다. 추가 채워넣기가 필요하지 않습니다.");
            }
            
            log.info("=== MovieDetail 채워넣기 완료 ===");
            
        } catch (Exception e) {
            log.error("MovieDetail 채워넣기 실패", e);
        }
    }
} 
