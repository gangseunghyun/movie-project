package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.BoxOffice;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.dto.BoxOfficeDto;
import com.movie.movie_backend.mapper.BoxOfficeMapper;
import com.movie.movie_backend.repository.BoxOfficeRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoxOfficeService {

    private final BoxOfficeRepository boxOfficeRepository;
    private final PRDMovieRepository movieRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KobisApiService kobisApiService;
    private final BoxOfficeMapper boxOfficeMapper;
    private final TmdbRatingService tmdbRatingService;

    @Value("${kobis.api.key}")
    private String apiKey;

    private static final String BASE_URL = "http://www.kobis.or.kr/kobisopenapi/webservice/rest";
    private static final String DAILY_BOX_OFFICE_URL = BASE_URL + "/boxoffice/searchDailyBoxOfficeList.json";
    private static final String WEEKLY_BOX_OFFICE_URL = BASE_URL + "/boxoffice/searchWeeklyBoxOfficeList.json";
    private static final String WEEKEND_BOX_OFFICE_URL = BASE_URL + "/boxoffice/searchWeekendBoxOfficeList.json";

    /**
     * 일일 박스오피스 TOP-10 가져오기
     */
    @Transactional
    public void fetchDailyBoxOffice() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String targetDate = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = String.format("%s?key=%s&targetDt=%s", DAILY_BOX_OFFICE_URL, apiKey, targetDate);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode boxOfficeResult = root.get("boxOfficeResult");
            if (boxOfficeResult != null && boxOfficeResult.get("dailyBoxOfficeList") != null) {
                JsonNode dailyBoxOfficeList = boxOfficeResult.get("dailyBoxOfficeList");
                
                // 기존 데이터 삭제 (항상 최신 데이터로 덮어쓰기)
                boxOfficeRepository.deleteByTargetDateAndRankType(yesterday, "DAILY");
                log.info("기존 일일 박스오피스 데이터 삭제 완료: {}", yesterday);
                
                // 먼저 모든 MovieDetail을 저장
                for (JsonNode movie : dailyBoxOfficeList) {
                    String movieCd = movie.get("movieCd").asText();
                    if (movieRepository.findByMovieCd(movieCd).isEmpty()) {
                        try {
                            log.info("MovieDetail 저장 시작: {}", movieCd);
                            kobisApiService.fetchAndSaveMovieDetail(movieCd);
                            log.info("MovieDetail 저장 완료: {}", movieCd);
                        } catch (Exception e) {
                            log.warn("MovieDetail 저장 실패: {}", movieCd, e);
                        }
                    }
                }
                
                // 그 다음 BoxOffice 저장 (상위 10개만)
                int count = 0;
                for (JsonNode movie : dailyBoxOfficeList) {
                    if (count >= 10) break; // 상위 10개만 저장
                    
                    BoxOffice boxOffice = parseBoxOfficeData(movie, yesterday, "DAILY");
                    if (boxOffice != null) {
                        boxOfficeRepository.save(boxOffice);
                        count++;
                        log.info("BoxOffice 저장 완료: {} - movieDetail: {} ({}번째)", 
                                boxOffice.getMovieCd(), 
                                boxOffice.getMovieDetail() != null ? boxOffice.getMovieDetail().getMovieCd() : "null",
                                count);
                    }
                }
                log.info("일일 박스오피스 데이터 저장 완료: {}개 (상위 10개)", count);
            }
        } catch (Exception e) {
            log.error("일일 박스오피스 데이터 가져오기 실패", e);
        }
    }

    /**
     * 주간 박스오피스 TOP-10 가져오기
     */
    @Transactional
    public void fetchWeeklyBoxOffice() {
        try {
            LocalDate lastWeek = LocalDate.now().minusWeeks(1);
            String targetDate = lastWeek.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = String.format("%s?key=%s&targetDt=%s&weekGb=0", WEEKLY_BOX_OFFICE_URL, apiKey, targetDate);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode boxOfficeResult = root.get("boxOfficeResult");
            if (boxOfficeResult != null && boxOfficeResult.get("weeklyBoxOfficeList") != null) {
                JsonNode weeklyBoxOfficeList = boxOfficeResult.get("weeklyBoxOfficeList");
                
                // 기존 데이터 삭제 (항상 최신 데이터로 덮어쓰기)
                boxOfficeRepository.deleteByTargetDateAndRankType(lastWeek, "WEEKLY");
                log.info("기존 주간 박스오피스 데이터 삭제 완료: {}", lastWeek);
                
                // 먼저 모든 MovieDetail을 저장
                for (JsonNode movie : weeklyBoxOfficeList) {
                    String movieCd = movie.get("movieCd").asText();
                    if (movieRepository.findByMovieCd(movieCd).isEmpty()) {
                        try {
                            log.info("MovieDetail 저장 시작: {}", movieCd);
                            kobisApiService.fetchAndSaveMovieDetail(movieCd);
                            log.info("MovieDetail 저장 완료: {}", movieCd);
                        } catch (Exception e) {
                            log.warn("MovieDetail 저장 실패: {}", movieCd, e);
                        }
                    }
                }
                
                // 그 다음 BoxOffice 저장 (상위 10개만)
                int count = 0;
                for (JsonNode movie : weeklyBoxOfficeList) {
                    if (count >= 10) break; // 상위 10개만 저장
                    
                    BoxOffice boxOffice = parseBoxOfficeData(movie, lastWeek, "WEEKLY");
                    if (boxOffice != null) {
                        boxOfficeRepository.save(boxOffice);
                        count++;
                        log.info("BoxOffice 저장 완료: {} - movieDetail: {} ({}번째)", 
                                boxOffice.getMovieCd(), 
                                boxOffice.getMovieDetail() != null ? boxOffice.getMovieDetail().getMovieCd() : "null",
                                count);
                    }
                }
                log.info("주간 박스오피스 데이터 저장 완료: {}개 (상위 10개)", count);
            }
        } catch (Exception e) {
            log.error("주간 박스오피스 데이터 가져오기 실패", e);
        }
    }

    /**
     * 박스오피스 데이터 파싱
     */
    private BoxOffice parseBoxOfficeData(JsonNode movie, LocalDate targetDate, String rankType) {
        try {
            String movieCd = movie.get("movieCd").asText();
            String movieNm = movie.get("movieNm").asText();
            int rank = movie.get("rank").asInt();
            long salesAmt = Long.parseLong(movie.get("salesAmt").asText());
            long audiCnt = Long.parseLong(movie.get("audiCnt").asText());
            long audiAcc = Long.parseLong(movie.get("audiAcc").asText());
            
            // MovieDetail 찾기 또는 생성
            MovieDetail movieDetail = movieRepository.findByMovieCd(movieCd).orElse(null);
            
            log.info("박스오피스 파싱 - movieCd: {}, movieNm: {}, MovieDetail 존재: {}", 
                    movieCd, movieNm, movieDetail != null);
            
            // MovieDetail이 없으면 다시 한번 KOBIS API로 가져오기 시도
            if (movieDetail == null) {
                try {
                    log.info("MovieDetail이 없어서 KOBIS API로 다시 가져오기 시도: {}", movieCd);
                    movieDetail = kobisApiService.fetchAndSaveMovieDetail(movieCd);
                    // 저장 후 다시 조회
                    movieDetail = movieRepository.findByMovieCd(movieCd).orElse(null);
                    log.info("KOBIS API 재시도 후 MovieDetail 존재: {}", movieDetail != null);
                } catch (Exception e) {
                    log.warn("KOBIS API 재시도 실패: {}", movieCd, e);
                }
            }
            
            BoxOffice boxOffice = BoxOffice.builder()
                    .movieCd(movieCd)
                    .movieNm(movieNm)
                    .rank(rank)
                    .salesAmt(salesAmt)
                    .audiCnt(audiCnt)
                    .audiAcc(audiAcc)
                    .targetDate(targetDate)
                    .rankType(rankType)
                    .movieDetail(movieDetail)
                    .build();
            
            log.info("BoxOffice 생성 완료 - id: {}, movieDetail: {}", 
                    boxOffice.getId(), boxOffice.getMovieDetail() != null ? boxOffice.getMovieDetail().getMovieCd() : "null");
            
            return boxOffice;
            
        } catch (Exception e) {
            log.warn("박스오피스 데이터 파싱 실패: {}", movie, e);
            return null;
        }
    }

    // ===== DTO 변환 메서드들 (왓챠피디아 스타일) =====

    /**
     * 최신 일일 박스오피스 TOP-20 조회 (DTO)
     */
    public List<BoxOfficeDto> getDailyBoxOfficeTop10AsDto() {
        List<BoxOffice> boxOfficeList = boxOfficeRepository.findLatestBoxOfficeTop10("DAILY");
        // 상위 20개만 반환
        return boxOfficeMapper.toDtoList(boxOfficeList.stream().limit(20).toList());
    }

    /**
     * 최신 주간 박스오피스 TOP-20 조회 (DTO)
     */
    public List<BoxOfficeDto> getWeeklyBoxOfficeTop10AsDto() {
        List<BoxOffice> boxOfficeList = boxOfficeRepository.findLatestBoxOfficeTop10("WEEKLY");
        // 상위 20개만 반환
        return boxOfficeMapper.toDtoList(boxOfficeList.stream().limit(20).toList());
    }

    /**
     * 특정 날짜의 박스오피스 조회 (DTO)
     */
    public List<BoxOfficeDto> getBoxOfficeByDateAsDto(LocalDate date, String rankType) {
        List<BoxOffice> boxOfficeList = boxOfficeRepository.findByTargetDateAndRankTypeOrderByRankAscWithMovieDetail(date, rankType);
        return boxOfficeMapper.toDtoList(boxOfficeList);
    }

    // ===== 기존 엔티티 반환 메서드들 (하위 호환성) =====

    /**
     * 최신 일일 박스오피스 TOP-20 조회
     */
    public List<BoxOffice> getDailyBoxOfficeTop10() {
        List<BoxOffice> boxOfficeList = boxOfficeRepository.findLatestBoxOfficeTop10("DAILY");
        // 상위 20개만 반환
        return boxOfficeList.stream().limit(20).toList();
    }

    /**
     * 최신 주간 박스오피스 TOP-20 조회
     */
    public List<BoxOffice> getWeeklyBoxOfficeTop10() {
        List<BoxOffice> boxOfficeList = boxOfficeRepository.findLatestBoxOfficeTop10("WEEKLY");
        // 상위 20개만 반환
        return boxOfficeList.stream().limit(20).toList();
    }

    /**
     * 특정 날짜의 박스오피스 조회
     */
    public List<BoxOffice> getBoxOfficeByDate(LocalDate date, String rankType) {
        return boxOfficeRepository.findByTargetDateAndRankTypeOrderByRankAscWithMovieDetail(date, rankType);
    }

    /**
     * 기존 BoxOffice 데이터의 movie_detail_id 업데이트
     */
    @Transactional
    public void updateBoxOfficeMovieDetailIds() {
        log.info("BoxOffice movie_detail_id 업데이트 시작");
        
        List<BoxOffice> boxOffices = boxOfficeRepository.findAll();
        int updatedCount = 0;
        
        for (BoxOffice boxOffice : boxOffices) {
            if (boxOffice.getMovieDetail() == null) {
                String movieCd = boxOffice.getMovieCd();
                Optional<MovieDetail> movieDetailOpt = movieRepository.findByMovieCd(movieCd);
                
                if (movieDetailOpt.isPresent()) {
                    boxOffice.setMovieDetail(movieDetailOpt.get());
                    boxOfficeRepository.save(boxOffice);
                    updatedCount++;
                    log.info("BoxOffice 업데이트 완료: {} -> {}", movieCd, movieDetailOpt.get().getMovieCd());
                } else {
                    log.warn("MovieDetail을 찾을 수 없음: {}", movieCd);
                }
            }
        }
        
        log.info("BoxOffice movie_detail_id 업데이트 완료: {}개 업데이트", updatedCount);
    }

    /**
     * 평균 별점이 높은 영화 TOP-N 조회
     */
    public List<MovieDetail> getTopRatedMovies(int limit) {
        return tmdbRatingService.getTopRatedMovies(limit);
    }
} 
