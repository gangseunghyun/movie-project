package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.BoxOffice;
import com.movie.movie_backend.dto.BoxOfficeDto;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.dto.TopRatedMovieDto;
import com.movie.movie_backend.service.PRDMovieListService;
import com.movie.movie_backend.service.PRDMovieService;
import com.movie.movie_backend.service.BoxOfficeService;
import com.movie.movie_backend.service.TmdbPopularMovieService;
import com.movie.movie_backend.service.KobisApiService;
import com.movie.movie_backend.service.DataMigrationService;
import com.movie.movie_backend.service.NaverMovieService;
import com.movie.movie_backend.service.TmdbRatingService;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.BoxOfficeRepository;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.mapper.MovieListMapper;
import com.movie.movie_backend.mapper.TopRatedMovieMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import com.movie.movie_backend.constant.MovieStatus;

@Slf4j
@Controller
@RequestMapping("/data")
@RequiredArgsConstructor
@Tag(name = "Data View", description = "데이터 조회 및 관리 API")
public class DataViewController {

    private final PRDMovieListRepository movieListRepository;
    private final PRDMovieRepository movieRepository;
    private final BoxOfficeRepository boxOfficeRepository;
    private final BoxOfficeService boxOfficeService;
    private final TmdbPopularMovieService tmdbPopularMovieService;
    private final KobisApiService kobisApiService;
    private final MovieDetailMapper movieDetailMapper;
    private final MovieListMapper movieListMapper;
    private final DataMigrationService dataMigrationService;
    private final NaverMovieService naverMovieService;
    private final TmdbRatingService tmdbRatingService;
    private final TopRatedMovieMapper topRatedMovieMapper;
    private final PRDMovieService prdMovieService;

    /**
     * 데이터 조회 메인 페이지
     */
    @GetMapping
    public String dataViewMain() {
        return "data-view";
    }

    /**
     * 테스트 API - 연결 상태 확인용
     */
    @GetMapping("/api/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testApi() {
        try {
            log.info("테스트 API 호출됨");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API 연결 성공",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("테스트 API 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 영화 상태별 개수 확인 API
     */
    @GetMapping("/api/movie-status-counts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMovieStatusCounts() {
        try {
            List<MovieList> allMovies = movieListRepository.findAll();
            
            Map<String, Integer> statusCounts = new HashMap<>();
            for (MovieList movie : allMovies) {
                String status = movie.getStatus() != null ? movie.getStatus().name() : "NULL";
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
            }
            
            log.info("영화 상태별 개수: {}", statusCounts);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "statusCounts", statusCounts,
                "totalMovies", allMovies.size()
            ));
        } catch (Exception e) {
            log.error("영화 상태별 개수 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * MovieList 데이터 조회 API
     * 
     * React에서 사용법:
     * - 영화 목록을 페이지네이션으로 조회할 때 사용
     * - 기본값: page=0, size=20
     * - 응답: { content: [...], totalElements: 100, totalPages: 5, ... }
     * 
     * 예시:
     * fetch('/data/api/movie-list?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 영화 목록
     */
    @GetMapping("/api/movie-list")
    @ResponseBody
    @Operation(summary = "MovieList 데이터 조회 API", 
               description = "영화 목록을 페이지네이션으로 조회합니다. React에서 사용할 때: fetch('/data/api/movie-list?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MovieList 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "MovieList 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getMovieListData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<MovieList> movieList = movieListRepository.findAll();
            int total = movieList.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieList> pagedList = movieList.subList(start, end);
            
            return ResponseEntity.ok(Map.of(
                "data", pagedList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("MovieList 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * MovieDetail 데이터 조회 API
     * 
     * React에서 사용법:
     * - 영화 상세정보를 페이지네이션으로 조회할 때 사용
     * - 감독, 배우, 줄거리 등 상세 정보 포함
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movie-detail?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 영화 상세정보 목록
     */
    @GetMapping("/api/movie-detail")
    @ResponseBody
    @Operation(summary = "MovieDetail 데이터 조회 API", 
               description = "영화 상세정보를 페이지네이션으로 조회합니다. 감독, 배우, 줄거리 등 포함. React에서 사용할 때: fetch('/data/api/movie-detail?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MovieDetail 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "MovieDetail 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getMovieDetailData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<MovieDetail> movieDetails = movieRepository.findAll();
            int total = movieDetails.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieDetail> pagedList = movieDetails.subList(start, end);
            
            return ResponseEntity.ok(Map.of(
                "data", pagedList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("MovieDetail 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * BoxOffice 데이터 조회 API
     * 
     * React에서 사용법:
     * - 박스오피스 데이터를 페이지네이션으로 조회할 때 사용
     * - 순위, 매출액, 관객수 등 박스오피스 정보 포함
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/box-office?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 박스오피스 목록
     */
    @GetMapping("/api/box-office")
    @ResponseBody
    @Operation(summary = "BoxOffice 데이터 조회 API", 
               description = "박스오피스 데이터를 페이지네이션으로 조회합니다. 순위, 매출액, 관객수 포함. React에서 사용할 때: fetch('/data/api/box-office?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "BoxOffice 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "BoxOffice 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getBoxOfficeData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<BoxOffice> boxOffices = boxOfficeRepository.findAll();
            int total = boxOffices.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<BoxOffice> pagedList = boxOffices.subList(start, end);
            
            return ResponseEntity.ok(Map.of(
                "data", pagedList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("BoxOffice 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * BoxOffice DTO 데이터 조회 API (왓챠피디아 스타일)
     * 
     * React에서 사용법:
     * - 박스오피스 데이터를 왓챠피디아 스타일로 조회할 때 사용
     * - 포스터 URL, 감독명, 배우 목록 등이 포함된 완전한 정보
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/box-office-dto?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 박스오피스 DTO 목록
     */
    @GetMapping("/api/box-office-dto")
    @ResponseBody
    @Operation(summary = "BoxOffice DTO 데이터 조회 API (왓챠피디아 스타일)", 
               description = "박스오피스 데이터를 왓챠피디아 스타일로 조회합니다. 포스터 URL, 감독명, 배우 목록 등 완전한 정보. React에서 사용할 때: fetch('/data/api/box-office-dto?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "BoxOffice DTO 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "BoxOffice DTO 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getBoxOfficeDtoData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<BoxOfficeDto> boxOfficeDtos = boxOfficeService.getDailyBoxOfficeTop10AsDto();
            int total = boxOfficeDtos.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<BoxOfficeDto> pagedList = boxOfficeDtos.subList(start, end);
            
            return ResponseEntity.ok(Map.of(
                "data", pagedList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("BoxOffice DTO 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * MovieDetail DTO 데이터 조회 API (왓챠피디아 스타일)
     * 
     * React에서 사용법:
     * - 영화 상세정보를 왓챠피디아 스타일로 조회할 때 사용
     * - 포스터 URL, 감독명, 배우 목록, 줄거리 등 완전한 정보
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movie-detail-dto?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 영화 상세정보 목록
     */
    @GetMapping("/api/movie-detail-dto")
    @ResponseBody
    @Operation(summary = "MovieDetail DTO 데이터 조회 API (왓챠피디아 스타일)", 
               description = "영화 상세정보를 왓챠피디아 스타일로 조회합니다. 포스터 URL, 감독명, 배우 목록, 줄거리 등 완전한 정보. React에서 사용할 때: fetch('/data/api/movie-detail-dto?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MovieDetail DTO 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "MovieDetail DTO 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getMovieDetailDtoData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<MovieDetail> movieDetails = movieRepository.findAll();
            int total = movieDetails.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieDetail> pagedList = movieDetails.subList(start, end);
            List<MovieDetailDto> dtoList = pagedList.stream()
                    .map(movieDetailMapper::toDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "data", dtoList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("MovieDetail DTO 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * MovieDetail DTO 검색 API (띄어쓰기 무시 통합 검색)
     * 
     * React에서 사용법:
     * - 영화 제목, 감독, 배우, 장르를 띄어쓰기 무시하고 통합 검색할 때 사용
     * - 키워드가 포함된 영화 제목을 찾습니다
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movie-detail-dto/search?keyword=아바타&page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 검색된 영화 목록
     */
    @GetMapping("/api/movie-detail-dto/search")
    @ResponseBody
    @Operation(summary = "MovieDetail DTO 검색 API (띄어쓰기 무시 통합 검색)", 
               description = "영화 제목, 감독, 배우, 장르를 띄어쓰기 무시하고 통합 검색합니다. React에서 사용할 때: fetch('/data/api/movie-detail-dto/search?keyword=아바타&page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "영화 검색 성공"),
        @ApiResponse(responseCode = "400", description = "영화 검색 실패")
    })
    public ResponseEntity<Map<String, Object>> searchMovieDetailDto(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("영화 통합 검색 요청(띄어쓰기 무시): keyword={}, page={}, size={}", keyword, page, size);
            List<MovieDetail> filteredMovies = prdMovieService.searchMoviesIgnoreSpace(keyword);
            int total = filteredMovies.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<MovieDetail> pagedList = filteredMovies.subList(start, end);
            List<MovieDetailDto> dtoList = pagedList.stream()
                    .map(movieDetailMapper::toDto)
                    .collect(Collectors.toList());
            log.info("영화 통합 검색 결과: keyword={}, total={}, page={}, size={}", keyword, total, page, size);
            return ResponseEntity.ok(Map.of(
                "data", dtoList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size),
                "keyword", keyword
            ));
        } catch (Exception e) {
            log.error("영화 통합 검색 실패: keyword={}", keyword, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * MovieList DTO 데이터 조회 API (왓챠피디아 스타일)
     * 
     * React에서 사용법:
     * - 영화 목록을 왓챠피디아 스타일로 조회할 때 사용
     * - 포스터 URL이 포함된 기본 영화 정보
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movie-list-dto?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 영화 목록 DTO
     */
    @GetMapping("/api/movie-list-dto")
    @ResponseBody
    @Operation(summary = "MovieList DTO 데이터 조회 API (왓챠피디아 스타일)", 
               description = "영화 목록을 왓챠피디아 스타일로 조회합니다. 포스터 URL이 포함된 기본 영화 정보. React에서 사용할 때: fetch('/data/api/movie-list-dto?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MovieList DTO 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "MovieList DTO 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getMovieListDtoData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("MovieList DTO API 호출됨");
            List<MovieList> movieLists = movieListRepository.findAll();
            log.info("MovieList 데이터 조회 완료: {}개", movieLists.size());
            
            int total = movieLists.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieList> pagedList = movieLists.subList(start, end);
            List<MovieListDto> dtoList = movieListMapper.toDtoList(pagedList);
            log.info("MovieList DTO 변환 완료: {}개", dtoList.size());
            
            return ResponseEntity.ok(Map.of(
                "data", dtoList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("MovieList DTO 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 데이터 통계 API
     * 
     * React에서 사용법:
     * - 전체 데이터 개수를 조회할 때 사용
     * - MovieList, MovieDetail, BoxOffice 개수 반환
     * 
     * 예시:
     * fetch('/data/api/stats')
     *   .then(res => res.json())
     *   .then(data => {
     *     console.log('MovieList:', data.movieListCount);
     *     console.log('MovieDetail:', data.movieDetailCount);
     *     console.log('BoxOffice:', data.boxOfficeCount);
     *   });
     */
    @GetMapping("/api/stats")
    @ResponseBody
    @Operation(summary = "데이터 통계 API", 
               description = "전체 데이터 개수를 조회합니다. MovieList, MovieDetail, BoxOffice 개수 반환. React에서 사용할 때: fetch('/data/api/stats')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "데이터 통계 조회 성공"),
        @ApiResponse(responseCode = "400", description = "데이터 통계 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            long movieListCount = movieListRepository.count();
            long movieDetailCount = movieRepository.count();
            long boxOfficeCount = boxOfficeRepository.count();
            
            Map<String, Object> stats = Map.of(
                "movieListCount", movieListCount,
                "movieDetailCount", movieDetailCount,
                "boxOfficeCount", boxOfficeCount
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("통계 데이터 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 평균 별점이 높은 영화 TOP-N 조회 API (왓챠피디아 스타일)
     * 
     * React에서 사용법:
     * - 평균 별점이 높은 영화를 조회할 때 사용
     * - 포스터 URL, 감독명 등 완전한 정보 포함
     * - 기본값: limit=10 (TOP-10)
     * 
     * 예시:
     * fetch('/data/api/ratings/top-rated?limit=5')
     *   .then(res => res.json())
     *   .then(data => console.log(data)); // 평균 별점 높은 영화 5개
     */
    @GetMapping("/api/ratings/top-rated")
    @ResponseBody
    @Operation(summary = "평균 별점이 높은 영화 TOP-N 조회 API (왓챠피디아 스타일)", 
               description = "평균 별점이 높은 영화를 조회합니다. 포스터 URL, 감독명 등 완전한 정보 포함. 기본값: limit=10 (TOP-10). React에서 사용할 때: fetch('/data/api/ratings/top-rated?limit=5')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "평균 별점이 높은 영화 조회 성공"),
        @ApiResponse(responseCode = "400", description = "평균 별점이 높은 영화 조회 실패")
    })
    public ResponseEntity<List<TopRatedMovieDto>> getTopRatedMovies(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TopRatedMovieDto> topRatedDtos = tmdbRatingService.getTopRatedMoviesAsDto(limit);
            return ResponseEntity.ok(topRatedDtos);
        } catch (Exception e) {
            log.error("평균 별점이 높은 영화 조회 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * TMDB 인기 영화 조회 API
     * 
     * React에서 사용법:
     * - TMDB에서 인기 영화를 조회할 때 사용
     * - KOBIS 정보와 TMDB 정보가 결합된 완전한 영화 정보
     * - 기본값: limit=50
     * 
     * 예시:
     * fetch('/data/api/popular-movies?limit=50')
     *   .then(res => res.json())
     *   .then(data => {
     *     console.log('성공:', data.success);
     *     console.log('영화 목록:', data.data);
     *     console.log('개수:', data.count);
     *   });
     */
    @GetMapping("/api/popular-movies")
    @ResponseBody
    @Operation(summary = "TMDB 인기 영화 조회 API", 
               description = "TMDB에서 인기 영화를 조회합니다. KOBIS 정보와 TMDB 정보가 결합된 완전한 영화 정보. 기본값: limit=50. React에서 사용할 때: fetch('/data/api/popular-movies?limit=50')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "TMDB 인기 영화 조회 성공"),
        @ApiResponse(responseCode = "400", description = "TMDB 인기 영화 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getPopularMovies(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            // limit을 최대 50개로 제한
            int actualLimit = Math.min(limit, 50);
            
            log.info("TMDB 인기 영화 조회 시작: limit={} (실제: {})", limit, actualLimit);
            
            List<MovieDetailDto> popularMovies = tmdbPopularMovieService.getPopularMovies(actualLimit);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", popularMovies,
                "count", popularMovies.size(),
                "message", "인기 영화 " + popularMovies.size() + "개를 성공적으로 가져왔습니다."
            ));
        } catch (Exception e) {
            log.error("TMDB 인기 영화 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "인기 영화 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 개봉예정작 조회 API
     * 
     * React에서 사용법:
     * - 개봉예정작을 조회할 때 사용
     * - 포스터 URL, 감독명 등 완전한 정보 포함
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movies/coming-soon?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data)); // 개봉예정작 목록
     */
    @GetMapping("/api/movies/coming-soon")
    @ResponseBody
    @Operation(summary = "개봉예정작 조회 API", 
               description = "개봉예정작을 조회합니다. 포스터 URL, 감독명 등 완전한 정보 포함. React에서 사용할 때: fetch('/data/api/movies/coming-soon?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "개봉예정작 조회 성공"),
        @ApiResponse(responseCode = "400", description = "개봉예정작 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getComingSoonMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<MovieList> allMovies = movieListRepository.findAll();
            LocalDate today = LocalDate.now();
            
            List<MovieList> comingSoonMovies = allMovies.stream()
                    .filter(movie -> {
                        // status가 COMING_SOON이거나, 개봉일이 오늘보다 늦은 경우
                        return MovieStatus.COMING_SOON.equals(movie.getStatus()) ||
                               (movie.getOpenDt() != null && movie.getOpenDt().isAfter(today));
                    })
                    .sorted((m1, m2) -> {
                        if (m1.getOpenDt() == null) return 1;
                        if (m2.getOpenDt() == null) return -1;
                        return m1.getOpenDt().compareTo(m2.getOpenDt());
                    })
                    .toList();
            
            log.info("개봉예정작 필터링 결과: {}개 (전체: {}개)", comingSoonMovies.size(), allMovies.size());
            
            int total = comingSoonMovies.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieList> pagedList = comingSoonMovies.subList(start, end);
            
            // MovieDetail 정보도 함께 가져오기
            List<Map<String, Object>> enrichedData = new ArrayList<>();
            for (MovieList movieList : pagedList) {
                Map<String, Object> movieData = new HashMap<>();
                
                // MovieList 정보
                movieData.put("movieCd", movieList.getMovieCd());
                movieData.put("movieNm", movieList.getMovieNm());
                movieData.put("movieNmEn", movieList.getMovieNmEn());
                movieData.put("openDt", movieList.getOpenDt());
                movieData.put("genreNm", movieList.getGenreNm());
                movieData.put("nationNm", movieList.getNationNm());
                movieData.put("watchGradeNm", movieList.getWatchGradeNm());
                movieData.put("posterUrl", movieList.getPosterUrl());
                movieData.put("status", movieList.getStatus());
                
                // MovieDetail 정보 추가
                try {
                    MovieDetail movieDetail = movieRepository.findById(movieList.getMovieCd()).orElse(null);
                    if (movieDetail != null) {
                        movieData.put("description", movieDetail.getDescription());
                        movieData.put("showTm", movieDetail.getShowTm());
                        movieData.put("companyNm", movieDetail.getCompanyNm());
                        movieData.put("averageRating", movieDetail.getAverageRating());
                        
                        // 감독 정보
                        if (movieDetail.getDirector() != null) {
                            movieData.put("directorName", movieDetail.getDirector().getName());
                        } else {
                            movieData.put("directorName", "");
                        }
                    } else {
                        // MovieDetail이 없는 경우 기본값 설정
                        movieData.put("description", "");
                        movieData.put("showTm", 0);
                        movieData.put("companyNm", "");
                        movieData.put("averageRating", 0.0);
                        movieData.put("directorName", "");
                    }
                } catch (Exception e) {
                    log.warn("MovieDetail 조회 실패: {} - {}", movieList.getMovieCd(), e.getMessage());
                    movieData.put("description", "");
                    movieData.put("showTm", 0);
                    movieData.put("companyNm", "");
                    movieData.put("averageRating", 0.0);
                    movieData.put("directorName", "");
                }
                
                enrichedData.add(movieData);
            }
            
            return ResponseEntity.ok(Map.of(
                "data", enrichedData,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("개봉예정작 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 개봉중인 영화 조회 API
     * 
     * React에서 사용법:
     * - 개봉중인 영화를 조회할 때 사용
     * - 포스터 URL, 감독명 등 완전한 정보 포함
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movies/now-playing?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data)); // 개봉중인 영화 목록
     */
    @GetMapping("/api/movies/now-playing")
    @ResponseBody
    @Operation(summary = "개봉중인 영화 조회 API", 
               description = "개봉중인 영화를 조회합니다. 포스터 URL, 감독명 등 완전한 정보 포함. React에서 사용할 때: fetch('/data/api/movies/now-playing?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "개봉중인 영화 조회 성공"),
        @ApiResponse(responseCode = "400", description = "개봉중인 영화 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getNowPlayingMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<MovieList> allMovies = movieListRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalDate threeMonthsAgo = today.minusMonths(3);
            
            List<MovieList> nowPlayingMovies = allMovies.stream()
                    .filter(movie -> {
                        // status가 NOW_PLAYING이거나, 개봉일이 3개월 이내인 경우
                        return MovieStatus.NOW_PLAYING.equals(movie.getStatus()) ||
                               (movie.getOpenDt() != null && 
                                movie.getOpenDt().isBefore(today.plusDays(1)) && // 오늘까지 개봉
                                movie.getOpenDt().isAfter(threeMonthsAgo)); // 3개월 이내 개봉
                    })
                    .sorted((m1, m2) -> {
                        if (m1.getOpenDt() == null) return 1;
                        if (m2.getOpenDt() == null) return -1;
                        return m2.getOpenDt().compareTo(m1.getOpenDt());
                    })
                    .toList();
            
            log.info("개봉중인 영화 필터링 결과: {}개 (전체: {}개)", nowPlayingMovies.size(), allMovies.size());
            
            int total = nowPlayingMovies.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieList> pagedList = nowPlayingMovies.subList(start, end);
            List<MovieListDto> dtoList = movieListMapper.toDtoList(pagedList);
            
            return ResponseEntity.ok(Map.of(
                "data", dtoList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("개봉중인 영화 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 상영종료된 영화 조회 API
     * 
     * React에서 사용법:
     * - 상영종료된 영화를 조회할 때 사용
     * - 포스터 URL, 감독명 등 완전한 정보 포함
     * - 기본값: page=0, size=20
     * 
     * 예시:
     * fetch('/data/api/movies/ended?page=0&size=10')
     *   .then(res => res.json())
     *   .then(data => console.log(data)); // 상영종료된 영화 목록
     */
    @GetMapping("/api/movies/ended")
    @ResponseBody
    @Operation(summary = "상영종료된 영화 조회 API", 
               description = "상영종료된 영화를 조회합니다. 포스터 URL, 감독명 등 완전한 정보 포함. React에서 사용할 때: fetch('/data/api/movies/ended?page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상영종료된 영화 조회 성공"),
        @ApiResponse(responseCode = "400", description = "상영종료된 영화 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getEndedMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<MovieList> allMovies = movieListRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalDate threeMonthsAgo = today.minusMonths(3);
            
            List<MovieList> endedMovies = allMovies.stream()
                    .filter(movie -> {
                        // status가 ENDED이거나, 개봉일이 3개월보다 오래 전인 경우
                        return MovieStatus.ENDED.equals(movie.getStatus()) ||
                               (movie.getOpenDt() != null && movie.getOpenDt().isBefore(threeMonthsAgo));
                    })
                    .sorted((m1, m2) -> {
                        if (m1.getOpenDt() == null) return 1;
                        if (m2.getOpenDt() == null) return -1;
                        return m2.getOpenDt().compareTo(m1.getOpenDt());
                    })
                    .toList();
            
            log.info("상영종료된 영화 필터링 결과: {}개 (전체: {}개)", endedMovies.size(), allMovies.size());
            
            int total = endedMovies.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieList> pagedList = endedMovies.subList(start, end);
            List<MovieListDto> dtoList = movieListMapper.toDtoList(pagedList);
            
            return ResponseEntity.ok(Map.of(
                "data", dtoList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            log.error("상영종료된 영화 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * MovieDetail 상태 디버깅 API
     */
    @GetMapping("/api/debug/movie-details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugMovieDetails() {
        try {
            List<MovieList> allMovieLists = movieListRepository.findAll();
            List<MovieDetail> allMovieDetails = movieRepository.findAll();
            
            // MovieList의 movieCd들
            Set<String> movieListCds = allMovieLists.stream()
                .map(MovieList::getMovieCd)
                .collect(java.util.stream.Collectors.toSet());
            
            // MovieDetail의 movieCd들
            Set<String> movieDetailCds = allMovieDetails.stream()
                .map(MovieDetail::getMovieCd)
                .collect(java.util.stream.Collectors.toSet());
            
            // MovieList에만 있고 MovieDetail에 없는 것들
            List<String> missingInDetail = movieListCds.stream()
                .filter(cd -> !movieDetailCds.contains(cd))
                .collect(java.util.stream.Collectors.toList());
            
            // MovieDetail에만 있고 MovieList에 없는 것들
            List<String> missingInList = movieDetailCds.stream()
                .filter(cd -> !movieListCds.contains(cd))
                .collect(java.util.stream.Collectors.toList());
            
            // 개봉예정작 중 MovieDetail이 없는 것들
            List<Map<String, Object>> comingSoonWithoutDetail = allMovieLists.stream()
                .filter(movie -> MovieStatus.COMING_SOON.equals(movie.getStatus()))
                .filter(movie -> !movieRepository.existsById(movie.getMovieCd()))
                .map(movie -> {
                    Map<String, Object> movieInfo = new HashMap<>();
                    movieInfo.put("movieCd", movie.getMovieCd());
                    movieInfo.put("movieNm", movie.getMovieNm());
                    movieInfo.put("openDt", movie.getOpenDt());
                    movieInfo.put("status", movie.getStatus());
                    return movieInfo;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "totalMovieList", allMovieLists.size(),
                "totalMovieDetail", allMovieDetails.size(),
                "missingInDetail", missingInDetail,
                "missingInDetailCount", missingInDetail.size(),
                "missingInList", missingInList,
                "missingInListCount", missingInList.size(),
                "comingSoonWithoutDetail", comingSoonWithoutDetail,
                "comingSoonWithoutDetailCount", comingSoonWithoutDetail.size()
            ));
            
        } catch (Exception e) {
            log.error("MovieDetail 디버깅 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 
