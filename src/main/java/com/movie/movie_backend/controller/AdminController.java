package com.movie.movie_backend.controller;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Tag;
import com.movie.movie_backend.entity.BoxOffice;
import com.movie.movie_backend.dto.BoxOfficeDto;
import com.movie.movie_backend.constant.MovieStatus;
import com.movie.movie_backend.dto.AdminMovieDto;
import com.movie.movie_backend.service.AdminMovieService;
import com.movie.movie_backend.service.MovieManagementService;
import com.movie.movie_backend.service.BoxOfficeService;
import com.movie.movie_backend.service.TmdbPosterService;
import com.movie.movie_backend.service.KobisPopularMovieService;
import com.movie.movie_backend.service.DataMigrationService;
import com.movie.movie_backend.service.TmdbPosterBatchService;
import com.movie.movie_backend.service.NaverMovieBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.ArrayList;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.entity.Cast;
import com.movie.movie_backend.entity.Actor;
import com.movie.movie_backend.repository.MovieDetailRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.movie.movie_backend.repository.CastRepository;
import com.movie.movie_backend.repository.PRDActorRepository;
import com.movie.movie_backend.service.FileUploadService;


@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminMovieService adminMovieService;
    private final MovieManagementService movieManagementService;
    private final BoxOfficeService boxOfficeService;
    private final TmdbPosterService tmdbPosterService;
    private final KobisPopularMovieService kobisPopularMovieService;
    private final DataMigrationService dataMigrationService;
    private final TmdbPosterBatchService tmdbPosterBatchService;
    private final NaverMovieBatchService naverMovieBatchService;
    private final MovieDetailRepository movieDetailRepository;
    private final PRDDirectorRepository directorRepository;
    private final CastRepository castRepository;
    private final PRDActorRepository actorRepository;
    private final FileUploadService fileUploadService;

    // ===== 영화 관리 =====

    /**
     * 영화 등록
     */
    @PostMapping("/movies")
    public ResponseEntity<AdminMovieDto> registerMovie(@RequestBody AdminMovieDto movieDto) {
        try {
            AdminMovieDto savedMovie = adminMovieService.registerMovie(movieDto);
            return ResponseEntity.ok(savedMovie);
        } catch (Exception e) {
            log.error("영화 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 영화 정보 수정
     */
    @PutMapping("/movies/{movieCd}")
    public ResponseEntity<AdminMovieDto> updateMovie(
            @PathVariable String movieCd,
            @RequestBody AdminMovieDto updateDto) {
        try {
            AdminMovieDto updatedMovie = adminMovieService.updateMovie(movieCd, updateDto);
            return ResponseEntity.ok(updatedMovie);
        } catch (Exception e) {
            log.error("영화 수정 실패: {} - {}", movieCd, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 영화 비활성화
     */
    @PutMapping("/movies/{movieCd}/deactivate")
    public ResponseEntity<AdminMovieDto> deactivateMovie(@PathVariable String movieCd) {
        try {
            MovieDetail deactivatedMovie = adminMovieService.deactivateMovie(movieCd);
            AdminMovieDto dto = adminMovieService.convertToDto(deactivatedMovie);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("영화 비활성화 실패: {} - {}", movieCd, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 영화 활성화
     */
    @PutMapping("/movies/{movieCd}/activate")
    public ResponseEntity<AdminMovieDto> activateMovie(@PathVariable String movieCd) {
        try {
            MovieDetail activatedMovie = adminMovieService.activateMovie(movieCd);
            AdminMovieDto dto = adminMovieService.convertToDto(activatedMovie);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("영화 활성화 실패: {} - {}", movieCd, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 영화 삭제
     */
    @DeleteMapping("/movies/{movieCd}")
    public ResponseEntity<Map<String, Object>> deleteMovie(@PathVariable String movieCd) {
        try {
            log.info("영화 삭제 요청: {}", movieCd);
            
            // MovieManagementService의 deleteMovie 메서드 사용
            movieManagementService.deleteMovie(movieCd);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "영화가 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("영화 삭제 실패: {} - {}", movieCd, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "영화 삭제에 실패했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 모든 영화 조회
     */
    @GetMapping("/movies")
    public ResponseEntity<List<AdminMovieDto>> getAllMovies() {
        List<AdminMovieDto> movies = adminMovieService.getAllMoviesAsDto();
        return ResponseEntity.ok(movies);
    }

    /**
     * 상태별 영화 조회
     */
    @GetMapping("/movies/status/{status}")
    public ResponseEntity<List<AdminMovieDto>> getMoviesByStatus(@PathVariable MovieStatus status) {
        List<AdminMovieDto> movies = adminMovieService.getMoviesByStatusAsDto(status);
        return ResponseEntity.ok(movies);
    }

    /**
     * 영화명으로 검색
     */
    @GetMapping("/movies/search")
    public ResponseEntity<List<AdminMovieDto>> searchMoviesByName(@RequestParam String movieNm) {
        List<AdminMovieDto> movies = adminMovieService.searchMoviesByNameAsDto(movieNm);
        return ResponseEntity.ok(movies);
    }

    // ===== 박스오피스 관리 (왓챠피디아 스타일) =====

    /**
     * 일일 박스오피스 TOP-10 조회 (왓챠피디아 스타일 DTO)
     */
    @GetMapping("/boxoffice/daily")
    public ResponseEntity<List<BoxOfficeDto>> getDailyBoxOffice() {
        try {
            List<BoxOfficeDto> boxOfficeList = boxOfficeService.getDailyBoxOfficeTop10AsDto();
            return ResponseEntity.ok(boxOfficeList);
        } catch (Exception e) {
            log.error("일일 박스오피스 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 주간 박스오피스 TOP-10 조회 (왓챠피디아 스타일 DTO)
     */
    @GetMapping("/boxoffice/weekly")
    public ResponseEntity<List<BoxOfficeDto>> getWeeklyBoxOffice() {
        try {
            List<BoxOfficeDto> boxOfficeList = boxOfficeService.getWeeklyBoxOfficeTop10AsDto();
            return ResponseEntity.ok(boxOfficeList);
        } catch (Exception e) {
            log.error("주간 박스오피스 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 특정 날짜의 박스오피스 조회 (왓챠피디아 스타일 DTO)
     */
    @GetMapping("/boxoffice/date/{date}")
    public ResponseEntity<List<BoxOfficeDto>> getBoxOfficeByDate(
            @PathVariable String date,
            @RequestParam(defaultValue = "DAILY") String rankType) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            List<BoxOfficeDto> boxOfficeList = boxOfficeService.getBoxOfficeByDateAsDto(targetDate, rankType);
            return ResponseEntity.ok(boxOfficeList);
        } catch (Exception e) {
            log.error("박스오피스 조회 실패: {} - {}", date, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 박스오피스 데이터 가져오기 (일일)
     */
    @PostMapping("/boxoffice/daily")
    public ResponseEntity<Map<String, String>> fetchDailyBoxOffice() {
        try {
            boxOfficeService.fetchDailyBoxOffice();
            return ResponseEntity.ok(Map.of("message", "일일 박스오피스 데이터 가져오기 완료"));
        } catch (Exception e) {
            log.error("일일 박스오피스 데이터 가져오기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 박스오피스 데이터 가져오기 (주간)
     */
    @PostMapping("/boxoffice/weekly")
    public ResponseEntity<Map<String, String>> fetchWeeklyBoxOffice() {
        try {
            boxOfficeService.fetchWeeklyBoxOffice();
            return ResponseEntity.ok(Map.of("message", "주간 박스오피스 데이터 가져오기 완료"));
        } catch (Exception e) {
            log.error("주간 박스오피스 데이터 가져오기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * BoxOffice movie_detail_id 업데이트
     */
    @PostMapping("/boxoffice/update-movie-details")
    public ResponseEntity<Map<String, String>> updateBoxOfficeMovieDetails() {
        try {
            boxOfficeService.updateBoxOfficeMovieDetailIds();
            return ResponseEntity.ok(Map.of("message", "BoxOffice movie_detail_id 업데이트 완료"));
        } catch (Exception e) {
            log.error("BoxOffice movie_detail_id 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 태그 관리 =====

    /**
     * 영화 태그 설정 (전체 교체)
     */
    @PutMapping("/movies/{movieCd}/tags")
    public ResponseEntity<AdminMovieDto> setMovieTags(
            @PathVariable String movieCd,
            @RequestBody List<String> tagNames) {
        try {
            MovieDetail updatedMovie = adminMovieService.setMovieTags(movieCd, tagNames);
            AdminMovieDto dto = adminMovieService.convertToDto(updatedMovie);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("영화 태그 설정 실패: {} - {}", movieCd, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 영화 태그 추가
     */
    @PostMapping("/movies/{movieCd}/tags")
    public ResponseEntity<AdminMovieDto> addMovieTag(
            @PathVariable String movieCd,
            @RequestBody Map<String, String> request) {
        try {
            String tagName = request.get("tagName");
            if (tagName == null || tagName.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            MovieDetail updatedMovie = adminMovieService.addMovieTag(movieCd, tagName.trim());
            AdminMovieDto dto = adminMovieService.convertToDto(updatedMovie);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("영화 태그 추가 실패: {} - {}", movieCd, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 영화 태그 제거
     */
    @DeleteMapping("/movies/{movieCd}/tags/{tagName}")
    public ResponseEntity<AdminMovieDto> removeMovieTag(
            @PathVariable String movieCd,
            @PathVariable String tagName) {
        try {
            MovieDetail updatedMovie = adminMovieService.removeMovieTag(movieCd, tagName);
            AdminMovieDto dto = adminMovieService.convertToDto(updatedMovie);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("영화 태그 제거 실패: {} - {} - {}", movieCd, tagName, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 모든 태그 조회
     */
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = adminMovieService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * 태그명으로 검색
     */
    @GetMapping("/tags/search")
    public ResponseEntity<List<Tag>> searchTags(@RequestParam String name) {
        List<Tag> tags = adminMovieService.searchTagsByName(name);
        return ResponseEntity.ok(tags);
    }

    /**
     * 기존 장르 태그들을 삭제하고 새로운 태그들로 교체
     */
    @PostMapping("/tags/reset")
    public ResponseEntity<Map<String, String>> resetTags() {
        try {
            adminMovieService.resetTags();
            return ResponseEntity.ok(Map.of("message", "태그 초기화 완료"));
        } catch (Exception e) {
            log.error("태그 초기화 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * movie_detail의 genre_nm에서 장르 태그 생성
     */
    @PostMapping("/tags/generate-from-genres")
    public ResponseEntity<Map<String, Object>> generateTagsFromGenres() {
        try {
            Map<String, Object> result = adminMovieService.generateTagsFromMovieGenres();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("장르 태그 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 장르 중복 현황 확인
     */
    @GetMapping("/movies/genre-duplicates")
    public ResponseEntity<Map<String, Object>> checkGenreDuplicates() {
        try {
            Map<String, Object> result = adminMovieService.checkGenreDuplicates();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("장르 중복 확인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * TMDB에서 장르 정보 업데이트
     */
    @PostMapping("/movies/update-genres")
    public ResponseEntity<Map<String, Object>> updateMovieGenres() {
        try {
            Map<String, Object> result = adminMovieService.updateMovieGenresFromTmdb();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("장르 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ===== 공개 예정작 관리 (왓챠피디아 스타일) =====

    /**
     * 공개 예정작 조회 (D-1, D-2, D-3 등)
     */
    @GetMapping("/movies/coming-soon")
    public ResponseEntity<List<AdminMovieDto>> getComingSoonMovies() {
        try {
            List<AdminMovieDto> movies = adminMovieService.getMoviesByStatusAsDto(MovieStatus.COMING_SOON);
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            log.error("공개 예정작 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 상영중인 영화 조회
     */
    @GetMapping("/movies/now-playing")
    public ResponseEntity<List<AdminMovieDto>> getNowPlayingMovies() {
        try {
            List<AdminMovieDto> movies = adminMovieService.getMoviesByStatusAsDto(MovieStatus.NOW_PLAYING);
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            log.error("상영중인 영화 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 상영 종료된 영화 조회
     */
    @GetMapping("/movies/ended")
    public ResponseEntity<List<AdminMovieDto>> getEndedMovies() {
        try {
            List<AdminMovieDto> movies = adminMovieService.getMoviesByStatusAsDto(MovieStatus.ENDED);
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            log.error("상영 종료된 영화 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== 평점 관리 =====


    /**
     * 평균 별점이 높은 영화 TOP-10 조회
     */
    @GetMapping("/ratings/top-rated")
    public ResponseEntity<List<AdminMovieDto>> getTopRatedMovies(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<AdminMovieDto> topRatedMovies = adminMovieService.getTopRatedMoviesAsDto(limit);
            return ResponseEntity.ok(topRatedMovies);
        } catch (Exception e) {
            log.error("평균 별점이 높은 영화 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 기존 영화들의 캐릭터명을 한국어로 업데이트
     */
    @PostMapping("/movies/update-character-names")
    public ResponseEntity<Map<String, Object>> updateCharacterNamesToKorean() {
        try {
            log.info("캐릭터명 한국어 업데이트 시작");
            kobisPopularMovieService.updateExistingCharacterNamesToKorean();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캐릭터명을 한국어로 업데이트했습니다."
            ));
        } catch (Exception e) {
            log.error("캐릭터명 업데이트 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "캐릭터명 업데이트에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // ===== 이미지 업로드 관리 =====

    /**
     * 감독 이미지 업로드
     */
    @PostMapping("/movies/{movieCd}/director-image")
    public ResponseEntity<Map<String, Object>> uploadDirectorImage(
            @PathVariable String movieCd,
            @RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("감독 이미지 업로드 요청: movieCd={}, filename={}", movieCd, file.getOriginalFilename());
            
            // 영화 상세정보 조회
            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            if (movieDetail == null) {
                response.put("success", false);
                response.put("message", "영화를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 감독 정보 확인
            Director director = movieDetail.getDirector();
            if (director == null) {
                response.put("success", false);
                response.put("message", "감독 정보가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 기존 이미지 삭제
            if (director.getPhotoUrl() != null) {
                fileUploadService.deleteImage(director.getPhotoUrl(), "directors");
            }
            
            // 새 이미지 업로드
            String imageUrl = fileUploadService.uploadImage(file, director.getId().toString(), "directors");
            
            // 감독 정보 업데이트
            director.setPhotoUrl(imageUrl);
            directorRepository.save(director);
            
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "감독 이미지가 업로드되었습니다.");
            
            log.info("감독 이미지 업로드 완료: directorId={}, imageUrl={}", director.getId(), imageUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("감독 이미지 업로드 실패: movieCd={}, error={}", movieCd, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "감독 이미지 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 감독 이미지 URL 설정
     */
    @PutMapping("/movies/{movieCd}/director-image-url")
    public ResponseEntity<Map<String, Object>> setDirectorImageUrl(
            @PathVariable String movieCd,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "감독 이미지 URL이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            if (movieDetail == null) {
                response.put("success", false);
                response.put("message", "영화를 찾을 수 없습니다: " + movieCd);
                return ResponseEntity.badRequest().body(response);
            }

            if (movieDetail.getDirector() == null) {
                response.put("success", false);
                response.put("message", "감독 정보가 없습니다. 먼저 감독을 등록해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            movieDetail.getDirector().setPhotoUrl(imageUrl.trim());
            directorRepository.save(movieDetail.getDirector());

            response.put("success", true);
            response.put("imageUrl", imageUrl.trim());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("감독 이미지 URL 설정 실패: {} - {}", movieCd, e.getMessage());
            response.put("success", false);
            response.put("message", "감독 이미지 URL 설정 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 배우 이미지 업로드 (여러 명)
     */
    @PostMapping("/movies/{movieCd}/actor-images")
    public ResponseEntity<Map<String, Object>> uploadActorImages(
            @PathVariable String movieCd,
            @RequestParam("images") List<MultipartFile> files) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("배우 이미지 업로드 요청: movieCd={}, files={}개", movieCd, files.size());
            
            // 영화 상세정보 조회
            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            if (movieDetail == null) {
                response.put("success", false);
                response.put("message", "영화를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 배우 목록 조회 (Cast를 통해 해당 영화의 배우만 조회)
            List<Cast> casts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(movieCd);
            if (casts.isEmpty()) {
                response.put("success", false);
                response.put("message", "해당 영화의 배우 정보가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            List<Actor> actors = casts.stream()
                .map(Cast::getActor)
                .collect(Collectors.toList());
            
            List<String> imageUrls = new ArrayList<>();
            int successCount = 0;
            
            // 파일 개수와 배우 개수 중 작은 값만큼 처리
            int maxCount = Math.min(files.size(), actors.size());
            
            for (int i = 0; i < maxCount; i++) {
                try {
                    MultipartFile file = files.get(i);
                    Actor actor = actors.get(i);
                    
                    // 기존 이미지 삭제
                    if (actor.getPhotoUrl() != null) {
                        fileUploadService.deleteImage(actor.getPhotoUrl(), "actors");
                    }
                    
                    // 새 이미지 업로드
                    String imageUrl = fileUploadService.uploadImage(file, actor.getId().toString(), "actors");
                    
                    // 배우 정보 업데이트
                    actor.setPhotoUrl(imageUrl);
                    actorRepository.save(actor);
                    
                    imageUrls.add(imageUrl);
                    successCount++;
                    
                    log.info("배우 이미지 업로드 성공: actorId={}, name={}, imageUrl={}", 
                        actor.getId(), actor.getName(), imageUrl);
                    
                } catch (Exception e) {
                    log.error("배우 이미지 업로드 실패: index={}, error={}", i, e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("imageUrls", imageUrls);
            response.put("message", String.format("배우 이미지 %d개가 업로드되었습니다.", successCount));
            
            log.info("배우 이미지 업로드 완료: movieCd={}, success={}개", movieCd, successCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배우 이미지 업로드 실패: movieCd={}, error={}", movieCd, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "배우 이미지 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 배우 이미지 URL 설정
     */
    @PutMapping("/movies/{movieCd}/actor-image-urls")
    public ResponseEntity<Map<String, Object>> setActorImageUrls(
            @PathVariable String movieCd,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) request.get("imageUrls");
            if (imageUrls == null || imageUrls.isEmpty()) {
                response.put("success", false);
                response.put("message", "배우 이미지 URL이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            if (movieDetail == null) {
                response.put("success", false);
                response.put("message", "영화를 찾을 수 없습니다: " + movieCd);
                return ResponseEntity.badRequest().body(response);
            }

            // 배우 목록 조회 (Cast를 통해 해당 영화의 배우만 조회)
            List<Cast> casts = castRepository.findByMovieDetailMovieCdOrderByOrderInCreditsAsc(movieCd);
            if (casts.isEmpty()) {
                response.put("success", false);
                response.put("message", "해당 영화의 배우 정보가 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            List<Actor> actors = casts.stream()
                .map(Cast::getActor)
                .collect(Collectors.toList());

            List<String> savedUrls = new ArrayList<>();
            int successCount = 0;
            boolean hasError = false;
            String errorMessage = "";

            // URL 개수와 배우 개수 중 작은 값만큼 처리
            int maxCount = Math.min(imageUrls.size(), actors.size());

            for (int i = 0; i < maxCount; i++) {
                try {
                    String imageUrl = imageUrls.get(i).trim();
                    if (!imageUrl.isEmpty()) {
                        Actor actor = actors.get(i);
                        actor.setPhotoUrl(imageUrl);
                        actorRepository.save(actor);
                        savedUrls.add(imageUrl);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("배우 이미지 URL 설정 실패: index={}, error={}", i, e.getMessage());
                    hasError = true;
                    errorMessage = e.getMessage();
                    break; // 하나라도 실패하면 중단
                }
            }

            if (hasError) {
                response.put("success", false);
                response.put("message", "배우 이미지 URL 설정 실패: " + errorMessage);
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("imageUrls", savedUrls);
            response.put("message", String.format("배우 이미지 URL %d개가 설정되었습니다.", successCount));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배우 이미지 URL 설정 실패: {} - {}", movieCd, e.getMessage());
            response.put("success", false);
            response.put("message", "배우 이미지 URL 설정 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ===== 포스터 URL 관리 =====

    /**
     * TMDB에서 포스터 URL 가져오기
     */
    @PostMapping("/posters/fetch-tmdb")
    public ResponseEntity<Map<String, Object>> fetchPosterUrlsFromTmdb() {
        try {
            log.info("TMDB 포스터 URL 가져오기 시작");
            tmdbPosterBatchService.updatePosterUrlsForAllMovies();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TMDB에서 포스터 URL을 가져왔습니다."
            ));
        } catch (Exception e) {
            log.error("TMDB 포스터 URL 가져오기 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "TMDB 포스터 URL 가져오기에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 네이버에서 포스터 URL 가져오기
     */
    @PostMapping("/posters/fetch-naver")
    public ResponseEntity<Map<String, Object>> fetchPosterUrlsFromNaver() {
        try {
            log.info("네이버 포스터 URL 가져오기 시작");
            naverMovieBatchService.updatePosterAndDirectorFromNaver();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "네이버에서 포스터 URL을 가져왔습니다."
            ));
        } catch (Exception e) {
            log.error("네이버 포스터 URL 가져오기 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "네이버 포스터 URL 가져오기에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    // ===== 데이터 상태 확인 및 복구 =====

    /**
     * 현재 데이터 상태 확인
     */
    @GetMapping("/data/status")
    public ResponseEntity<Map<String, Object>> checkDataStatus() {
        try {
            dataMigrationService.checkDataStatus();
            
            long movieListCount = dataMigrationService.getMovieListCount();
            long movieDetailCount = dataMigrationService.getMovieDetailCount();
            List<String> missingMovieCds = dataMigrationService.findMovieListWithoutDetail();
            
            Map<String, Object> status = Map.of(
                "movieListCount", movieListCount,
                "movieDetailCount", movieDetailCount,
                "missingMovieDetailCount", missingMovieCds.size(),
                "missingMovieCds", missingMovieCds
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("데이터 상태 확인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 누락된 MovieDetail 채워넣기
     */
    @PostMapping("/data/fill-missing-details")
    public ResponseEntity<Map<String, Object>> fillMissingMovieDetails() {
        try {
            List<String> missingMovieCds = dataMigrationService.findMovieListWithoutDetail();
            
            if (missingMovieCds.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "누락된 MovieDetail이 없습니다.",
                    "missingCount", 0
                ));
            }
            
            dataMigrationService.fillMissingMovieDetails();
            
            // 업데이트 후 상태 재확인
            List<String> remainingMissing = dataMigrationService.findMovieListWithoutDetail();
            
            Map<String, Object> result = Map.of(
                "message", "누락된 MovieDetail 채워넣기 완료",
                "originalMissingCount", missingMovieCds.size(),
                "remainingMissingCount", remainingMissing.size(),
                "successCount", missingMovieCds.size() - remainingMissing.size()
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("누락된 MovieDetail 채워넣기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 2024년 영화 상태 정리 API
     * 2024년에 개봉한 영화들을 개봉예정작에서 상영중으로 변경
     */
    @PostMapping("/fix-2024-movies")
    public ResponseEntity<Map<String, Object>> fix2024Movies() {
        try {
            log.info("2024년 영화 상태 정리 시작");
            
            List<AdminMovieDto> comingSoonMovies = adminMovieService.getMoviesByStatusAsDto(MovieStatus.COMING_SOON);
            int updatedCount = 0;
            
            for (AdminMovieDto movie : comingSoonMovies) {
                if (movie.getOpenDt() != null && 
                    movie.getOpenDt().getYear() == 2024 && 
                    !movie.getOpenDt().isAfter(java.time.LocalDate.now())) {
                    
                    // 2024년에 개봉한 영화를 상영중으로 변경
                    adminMovieService.updateMovieStatus(movie.getMovieCd(), MovieStatus.NOW_PLAYING);
                    updatedCount++;
                    
                    log.info("2024년 영화 상태 변경: {} (개봉일: {}) → 상영중", 
                        movie.getMovieNm(), movie.getOpenDt());
                }
            }
            
            log.info("2024년 영화 상태 정리 완료: {}개 변경", updatedCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "2024년 영화 " + updatedCount + "개의 상태를 상영중으로 변경했습니다.",
                "updatedCount", updatedCount
            ));
            
        } catch (Exception e) {
            log.error("2024년 영화 상태 정리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "2024년 영화 상태 정리에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 영화 영어 제목과 장르 정보를 TMDB에서 보완
     */
    @PostMapping("/movies/update-english-titles-and-genres")
    public ResponseEntity<Map<String, Object>> updateMovieEnglishTitlesAndGenres() {
        try {
            log.info("영화 영어 제목과 장르 정보 보완 시작");
            Map<String, Object> result = adminMovieService.updateMovieEnglishTitlesAndGenres();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("영화 영어 제목과 장르 정보 보완 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 
