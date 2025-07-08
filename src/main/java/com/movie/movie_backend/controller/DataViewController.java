package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.BoxOffice;
import com.movie.movie_backend.entity.SearchLog;
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
import com.movie.movie_backend.service.TmdbPosterService;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.BoxOfficeRepository;
import com.movie.movie_backend.repository.SearchLogRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.repository.REVLikeRepository;
import com.movie.movie_backend.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.movie.movie_backend.repository.USRUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;

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
    private final TmdbPosterService tmdbPosterService;
    private final TopRatedMovieMapper topRatedMovieMapper;
    private final PRDMovieService prdMovieService;
    private final PRDMovieListService prdMovieListService;
    private final SearchLogRepository searchLogRepository;
    private final REVLikeRepository likeRepository;
    private final USRUserRepository userRepository;
    private final PRDActorRepository actorRepository;
    private final PRDDirectorRepository directorRepository;

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
            
            List<MovieList> pagedList = new ArrayList<>(movieList.subList(start, end));
            
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
     * - 정렬 옵션: sort=date (개봉일순), sort=nameAsc (이름 오름차순), sort=nameDesc (이름 내림차순), sort=rating (별점순)
     * - 기본값: page=0, size=20, sort=date
     * 
     * 예시:
     * fetch('/data/api/movie-detail?page=0&size=10&sort=date')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 영화 상세정보 목록
     */
    @GetMapping("/api/movie-detail")
    @ResponseBody
    @Operation(summary = "MovieDetail 데이터 조회 API", 
               description = "영화 상세정보를 페이지네이션으로 조회합니다. 감독, 배우, 줄거리 등 포함. React에서 사용할 때: fetch('/data/api/movie-detail?page=0&size=10&sort=date')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MovieDetail 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "MovieDetail 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getMovieDetailData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sort) {
        
        try {
            List<MovieDetail> movieDetails;
            
            // 정렬 옵션에 따라 데이터 조회
            switch (sort) {
                case "date":
                    movieDetails = movieRepository.findAllByOrderByOpenDtDesc(); // 개봉일 최신순
                    break;
                case "nameAsc":
                    movieDetails = movieRepository.findAllByOrderByMovieNmAsc(); // 이름 오름차순
                    break;
                case "nameDesc":
                    movieDetails = movieRepository.findAllByOrderByMovieNmDesc(); // 이름 내림차순
                    break;
                case "rating":
                    movieDetails = movieRepository.findAll();
                    movieDetails.sort((m1, m2) -> Double.compare(m2.getAverageRating() != null ? m2.getAverageRating() : 0.0, m1.getAverageRating() != null ? m1.getAverageRating() : 0.0));
                    break;
                default:
                    movieDetails = movieRepository.findAllByOrderByOpenDtDesc(); // 기본값: 개봉일 최신순
                    break;
            }
            
            int total = movieDetails.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieDetail> pagedList = new ArrayList<>(movieDetails.subList(start, end));
            
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
            
            List<BoxOffice> pagedList = new ArrayList<>(boxOffices.subList(start, end));
            
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
            
            List<BoxOfficeDto> pagedList = new ArrayList<>(boxOfficeDtos.subList(start, end));
            
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
     * - 정렬 옵션: sort=date (개봉일순), sort=nameAsc (이름 오름차순), sort=nameDesc (이름 내림차순), sort=rating (별점순)
     * - 기본값: page=0, size=20, sort=date
     * 
     * 예시:
     * fetch('/data/api/movie-detail-dto?page=0&size=10&sort=date')
     *   .then(res => res.json())
     *   .then(data => console.log(data.content)); // 영화 상세정보 목록
     */
    @GetMapping("/api/movie-detail-dto")
    @ResponseBody
    @Operation(summary = "MovieDetail DTO 데이터 조회 API (왓챠피디아 스타일)", 
               description = "영화 상세정보를 왓챠피디아 스타일로 조회합니다. 포스터 URL, 감독명, 배우 목록, 줄거리 등 완전한 정보. React에서 사용할 때: fetch('/data/api/movie-detail-dto?page=0&size=10&sort=date') 또는 fetch('/data/api/movie-detail-dto?movieCd=20201234')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MovieDetail DTO 데이터 조회 성공"),
        @ApiResponse(responseCode = "400", description = "MovieDetail DTO 데이터 조회 실패")
    })
    public ResponseEntity<Map<String, Object>> getMovieDetailDtoData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(required = false) String movieCd,
            HttpServletRequest request) {
        
        try {
            List<MovieDetail> movieDetails;
            
            // movieCd가 있으면 특정 영화만 조회
            if (movieCd != null && !movieCd.trim().isEmpty()) {
                MovieDetail movieDetail = movieRepository.findByMovieCd(movieCd).orElse(null);
                if (movieDetail == null) {
                    return ResponseEntity.ok(Map.of(
                        "data", List.of(),
                        "total", 0,
                        "page", page,
                        "size", size,
                        "totalPages", 0
                    ));
                }
                movieDetails = List.of(movieDetail);
            } else {
                // 정렬 옵션에 따라 데이터 조회
                switch (sort) {
                    case "date":
                        movieDetails = movieRepository.findAllByOrderByOpenDtDesc(); // 개봉일 최신순
                        break;
                    case "nameAsc":
                        movieDetails = movieRepository.findAllByOrderByMovieNmAsc(); // 이름 오름차순
                        break;
                    case "nameDesc":
                        movieDetails = movieRepository.findAllByOrderByMovieNmDesc(); // 이름 내림차순
                        break;
                    case "rating":
                        movieDetails = movieRepository.findAll();
                        movieDetails.sort((m1, m2) -> Double.compare(m2.getAverageRating() != null ? m2.getAverageRating() : 0.0, m1.getAverageRating() != null ? m1.getAverageRating() : 0.0));
                        break;
                    default:
                        movieDetails = movieRepository.findAllByOrderByOpenDtDesc(); // 기본값: 개봉일 최신순
                        break;
                }
            }
            
            int total = movieDetails.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            
            List<MovieDetail> pagedList = new ArrayList<>(movieDetails.subList(start, end));
            User currentUser = getCurrentUser(request);
            List<MovieDetailDto> dtoList = pagedList.stream()
                .map(md -> movieDetailMapper.toDto(
                    md,
                    likeRepository.countByMovieDetail(md),
                    currentUser != null && likeRepository.existsByMovieDetailAndUser(md, currentUser)
                ))
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
     * - 영화 제목, 감독, 배우, 장르를 띄어쓰기 무시하고 통합 검색
     * - movie_detail → movie_list 순서로 검색하여 결과 합치기
     * - 검색 시 search_log에 자동 저장 (인기검색어 집계용)
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
               description = "영화 제목, 감독, 배우, 장르를 띄어쓰기 무시하고 통합 검색합니다. movie_detail → movie_list 순서로 검색하여 결과 합치기. React에서 사용할 때: fetch('/data/api/movie-detail-dto/search?keyword=아바타&page=0&size=10')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "영화 검색 성공"),
        @ApiResponse(responseCode = "400", description = "영화 검색 실패")
    })
    public ResponseEntity<Map<String, Object>> searchMovieDetailDto(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            log.info("영화 통합 검색 요청(띄어쓰기 무시): keyword={}, page={}, size={}", keyword, page, size);
            
            // 2. movie_detail에서 검색 (기존 통합 검색)
            List<MovieDetail> movieDetailResults = prdMovieService.searchMoviesIgnoreSpace(keyword);
            log.info("movie_detail 검색 결과: {}개", movieDetailResults.size());
            
            // 3. movie_detail에 없으면 movie_list에서 검색
            List<MovieDetail> movieListResults = new ArrayList<>();
            if (movieDetailResults.isEmpty()) {
                log.info("movie_detail에 결과가 없어 movie_list에서 검색 시도");
                List<MovieListDto> movieListDtos = prdMovieListService.searchMoviesIgnoreSpace(keyword);
                
                // MovieListDto를 MovieDetail로 변환 (기본 정보만)
                for (MovieListDto movieListDto : movieListDtos) {
                    MovieDetail movieDetail = new MovieDetail();
                    movieDetail.setMovieCd(movieListDto.getMovieCd());
                    movieDetail.setMovieNm(movieListDto.getMovieNm());
                    movieDetail.setMovieNmEn(movieListDto.getMovieNmEn());
                    movieDetail.setOpenDt(movieListDto.getOpenDt());
                    movieDetail.setGenreNm(movieListDto.getGenreNm());
                    movieDetail.setNationNm(movieListDto.getNationNm());
                    movieDetail.setWatchGradeNm(movieListDto.getWatchGradeNm());
//                    movieDetail.setStatus(movieListDto.getStatus());
                    movieListResults.add(movieDetail);
                }
                log.info("movie_list 검색 결과: {}개", movieListResults.size());
            }
            
            // 4. 두 결과 합치기 (중복 제거)
            List<MovieDetail> allResults = new ArrayList<>(movieDetailResults);
            
            // movie_list 결과 중 movie_detail에 없는 것만 추가
            Set<String> existingMovieCds = movieDetailResults.stream()
                    .map(MovieDetail::getMovieCd)
                    .collect(Collectors.toSet());
            
            for (MovieDetail movieListResult : movieListResults) {
                if (!existingMovieCds.contains(movieListResult.getMovieCd())) {
                    allResults.add(movieListResult);
                }
            }
            
            log.info("최종 검색 결과: movie_detail={}개, movie_list={}개, 합계={}개", 
                    movieDetailResults.size(), movieListResults.size(), allResults.size());
            
            // 5. 페이지네이션 처리
            int total = allResults.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<MovieDetail> pagedList = allResults.subList(start, end);
            
            // 6. DTO 변환
            User currentUser = getCurrentUser(request);
            List<MovieDetailDto> dtoList = pagedList.stream()
                .map(md -> movieDetailMapper.toDto(
                    md,
                    likeRepository.countByMovieDetail(md),
                    currentUser != null && likeRepository.existsByMovieDetailAndUser(md, currentUser)
                ))
                .collect(Collectors.toList());
            
            log.info("영화 통합 검색 결과: keyword={}, total={}, page={}, size={}", keyword, total, page, size);
            return ResponseEntity.ok(Map.of(
                "data", dtoList,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size),
                "keyword", keyword,
                "searchSource", Map.of(
                    "movieDetailCount", movieDetailResults.size(),
                    "movieListCount", movieListResults.size()
                )
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
            List<TopRatedMovieDto> topRatedDtos = tmdbPosterService.getTopRatedMoviesAsDto(limit);
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
                    MovieDetail movieDetail = movieRepository.findByMovieCd(movieList.getMovieCd()).orElse(null);
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
                .filter(movie -> !movieRepository.existsByMovieCd(movie.getMovieCd()))
                .map(movie -> {
                    Map<String, Object> movieInfo = new HashMap<>();
                    movieInfo.put("movieCd", movie.getMovieCd());
                    movieInfo.put("movieNm", movie.getMovieNm());
                    movieInfo.put("openDt", movie.getOpenDt());
                    movieInfo.put("status", movie.getStatus());
                    return movieInfo;
                })
                .collect(Collectors.toList());
            
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

    /**
     * API 응답 테스트용 엔드포인트
     */
    @GetMapping("/api/test-response")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testResponse(HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request);
            log.info("테스트 - 현재 사용자: {}", currentUser != null ? currentUser.getLoginId() : "null");
            
            // 첫 번째 영화 하나만 가져와서 테스트
            List<MovieDetail> movieDetails = movieRepository.findAll();
            if (movieDetails.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "영화 데이터가 없습니다."));
            }
            
            MovieDetail firstMovie = movieDetails.get(0);
            int likeCount = likeRepository.countByMovieDetail(firstMovie);
            boolean likedByMe = currentUser != null && likeRepository.existsByMovieDetailAndUser(firstMovie, currentUser);
            
            log.info("테스트 - 영화: {}, 좋아요 수: {}, 내가 좋아요: {}", 
                firstMovie.getMovieNm(), likeCount, likedByMe);
            
            MovieDetailDto dto = movieDetailMapper.toDto(firstMovie, likeCount, likedByMe);
            
            return ResponseEntity.ok(Map.of(
                "testMovie", dto,
                "currentUser", currentUser != null ? currentUser.getLoginId() : null,
                "likeCount", likeCount,
                "likedByMe", likedByMe
            ));
        } catch (Exception e) {
            log.error("테스트 응답 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 현재 로그인한 사용자 반환 (없으면 null)
    private User getCurrentUser(HttpServletRequest request) {
        log.info("=== getCurrentUser 호출됨 ===");
        
        // 세션에서 직접 사용자 정보 확인
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionLoginId = (String) session.getAttribute("USER_LOGIN_ID");
            log.info("세션에서 USER_LOGIN_ID: {}", sessionLoginId);
            
            if (sessionLoginId != null) {
                User sessionUser = userRepository.findByLoginId(sessionLoginId).orElse(null);
                if (sessionUser != null) {
                    log.info("세션에서 사용자 조회 성공: {}", sessionUser.getLoginId());
                    return sessionUser;
                }
            }
        }
        
        // Spring Security Authentication에서 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authentication: {}", authentication);
        
        if (authentication == null) {
            log.error("Authentication이 null입니다.");
            return null;
        }
        
        log.info("Authentication Principal: {}", authentication.getPrincipal());
        log.info("Authentication Principal Type: {}", authentication.getPrincipal().getClass().getName());
        log.info("Authentication Name: {}", authentication.getName());
        log.info("Authentication isAuthenticated: {}", authentication.isAuthenticated());
        
        if (!authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            log.error("인증되지 않은 사용자입니다.");
            return null;
        }
        
        User user = null;
        
        // OAuth2 사용자인 경우
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User) {
            log.info("OAuth2 사용자로 인식됨");
            org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User = 
                (org.springframework.security.oauth2.core.user.DefaultOAuth2User) authentication.getPrincipal();
            
            String email = oauth2User.getAttribute("email");
            String provider = oauth2User.getAttribute("provider");
            String providerId = oauth2User.getAttribute("providerId");
            
            // 카카오의 경우 email이 kakao_account 안에 있을 수 있음
            if (email == null && "KAKAO".equals(provider)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                }
            }
            
            log.info("OAuth2 속성 - email: {}, provider: {}, providerId: {}", email, provider, providerId);
            
            if (email != null && provider != null && providerId != null) {
                try {
                    com.movie.movie_backend.constant.Provider providerEnum = 
                        com.movie.movie_backend.constant.Provider.valueOf(provider.toUpperCase());
                    user = userRepository.findByProviderAndProviderId(providerEnum, providerId).orElse(null);
                    log.info("OAuth2 사용자 조회 결과: {}", user);
                } catch (Exception e) {
                    log.error("OAuth2 사용자 조회 실패", e);
                }
            }
        }
        // Spring Security로 로그인한 사용자인 경우 (User 엔티티가 Principal)
        else if (authentication.getPrincipal() instanceof User) {
            log.info("Spring Security User 엔티티로 인식됨");
            user = (User) authentication.getPrincipal();
            log.info("Spring Security 사용자 조회 결과: {}", user);
        }
        // Spring Security의 UserDetails 구현체인 경우
        else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            log.info("Spring Security UserDetails로 인식됨");
            String loginId = authentication.getName();
            user = userRepository.findByLoginId(loginId).orElse(null);
            log.info("UserDetails로 사용자 조회 결과: {}", user);
        }
        // 기타 경우 (loginId로 조회)
        else {
            log.info("기타 타입으로 인식됨: {}", authentication.getPrincipal().getClass().getName());
            String loginId = authentication.getName();
            user = userRepository.findByLoginId(loginId).orElse(null);
            log.info("loginId로 사용자 조회 결과: {}", user);
        }
        
        if (user == null) {
            log.error("최종적으로 사용자를 찾을 수 없습니다.");
        } else {
            log.info("최종 사용자: id={}, loginId={}, role={}, isAdmin={}", 
                user.getId(), user.getLoginId(), user.getRole(), user.isAdmin());
        }
        
        return user;
    }

    @GetMapping("/api/search-person")
    @ResponseBody
    public Map<String, Object> searchPerson(@RequestParam String keyword) {
        List<Map<String, Object>> actors = actorRepository.findByNameContainingIgnoreCase(keyword).stream()
            .map(actor -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", actor.getId());
                map.put("name", actor.getName());
                map.put("photoUrl", actor.getPhotoUrl());
                return map;
            }).collect(Collectors.toList());
        List<Map<String, Object>> directors = directorRepository.findByNameContainingIgnoreCase(keyword).stream()
            .map(director -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", director.getId());
                map.put("name", director.getName());
                map.put("photoUrl", director.getPhotoUrl());
                return map;
            }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("actors", actors);
        result.put("directors", directors);
        return result;
    }
} 
