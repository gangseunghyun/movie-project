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
import java.util.Map;
import java.util.stream.Collectors;

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
    private final TmdbPosterService tmdbPosterService;

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
                
                // 그 다음 BoxOffice 저장 (상위 10개만, 중복 체크 포함)
                int count = 0;
                int skipped = 0;
                int oldMovieSkipped = 0;
                for (JsonNode movie : dailyBoxOfficeList) {
                    if (count >= 10) break; // 상위 10개만 저장
                    
                    String movieCd = movie.get("movieCd").asText();
                    String movieNm = movie.get("movieNm").asText();
                    
                    // 중복 체크
                    if (boxOfficeRepository.existsByMovieCdAndTargetDateAndRankType(movieCd, yesterday, "DAILY")) {
                        log.info("이미 존재하는 일일 박스오피스 데이터 스킵: {} ({}위)", movieCd, movie.get("rank").asInt());
                        skipped++;
                        continue;
                    }
                    
                    // 최근 영화인지 확인 (5년 이내 개봉)
                    if (!isRecentMovie(movie)) {
                        log.info("오래된 영화 제외: {} ({}위) - 개봉일이 5년 이상됨", movieNm, movie.get("rank").asInt());
                        oldMovieSkipped++;
                        continue;
                    }
                    
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
                log.info("일일 박스오피스 데이터 처리 완료: 저장 {}개, 스킵 {}개, 오래된 영화 제외 {}개 (상위 10개)", count, skipped, oldMovieSkipped);
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
                
                // 그 다음 BoxOffice 저장 (상위 10개만, 중복 체크 포함)
                int count = 0;
                int skipped = 0;
                int oldMovieSkipped = 0;
                for (JsonNode movie : weeklyBoxOfficeList) {
                    if (count >= 10) break; // 상위 10개만 저장
                    
                    String movieCd = movie.get("movieCd").asText();
                    String movieNm = movie.get("movieNm").asText();
                    
                    // 중복 체크
                    if (boxOfficeRepository.existsByMovieCdAndTargetDateAndRankType(movieCd, lastWeek, "WEEKLY")) {
                        log.info("이미 존재하는 주간 박스오피스 데이터 스킵: {} ({}위)", movieCd, movie.get("rank").asInt());
                        skipped++;
                        continue;
                    }
                    
                    // 최근 영화인지 확인 (5년 이내 개봉)
                    if (!isRecentMovie(movie)) {
                        log.info("오래된 영화 제외: {} ({}위) - 개봉일이 5년 이상됨", movieNm, movie.get("rank").asInt());
                        oldMovieSkipped++;
                        continue;
                    }
                    
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
                log.info("주간 박스오피스 데이터 처리 완료: 저장 {}개, 스킵 {}개, 오래된 영화 제외 {}개 (상위 10개)", count, skipped, oldMovieSkipped);
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

    /**
     * 최근 영화인지 확인 (5년 이내 개봉)
     */
    private boolean isRecentMovie(JsonNode movie) {
        try {
            String openDt = movie.has("openDt") ? movie.get("openDt").asText() : "";
            if (openDt.isEmpty()) {
                // 개봉일이 없으면 포함 (나중에 상세정보에서 확인)
                return true;
            }
            
            LocalDate openDate = null;
            try {
                // yyyy-MM-dd 형식으로 파싱 시도
                openDate = LocalDate.parse(openDt, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e1) {
                try {
                    // yyyyMMdd 형식으로 파싱 시도
                    openDate = LocalDate.parse(openDt, DateTimeFormatter.ofPattern("yyyyMMdd"));
                } catch (Exception e2) {
                    log.warn("날짜 파싱 실패: {} - yyyy-MM-dd와 yyyyMMdd 모두 실패", openDt);
                    return true; // 파싱 실패 시 포함
                }
            }
            
            LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
            return !openDate.isBefore(fiveYearsAgo);
            
        } catch (Exception e) {
            log.warn("영화 개봉일 확인 실패: {}", movie.get("movieNm"), e);
            return true; // 에러 시 포함
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
     * 박스오피스 데이터 정리 및 순위 재정렬
     */
    @Transactional
    public void cleanupAndReorderBoxOffice() {
        log.info("박스오피스 데이터 정리 및 순위 재정렬 시작");
        
        // 1. 모든 박스오피스 데이터 조회
        List<BoxOffice> allBoxOffices = boxOfficeRepository.findAll();
        log.info("전체 박스오피스 데이터: {}개", allBoxOffices.size());
        
        // 2. 날짜별로 그룹화
        Map<LocalDate, List<BoxOffice>> dateGroups = allBoxOffices.stream()
            .collect(Collectors.groupingBy(BoxOffice::getTargetDate));
        
        // 3. 각 날짜별로 순위 재정렬
        for (Map.Entry<LocalDate, List<BoxOffice>> entry : dateGroups.entrySet()) {
            LocalDate targetDate = entry.getKey();
            List<BoxOffice> boxOffices = entry.getValue();
            
            log.info("날짜 {} 처리 중: {}개 영화", targetDate, boxOffices.size());
            
            // 일일 관객수(audiCnt) 기준으로 내림차순 정렬
            boxOffices.sort((a, b) -> Long.compare(b.getAudiCnt(), a.getAudiCnt()));
            
            // 순위 재할당
            for (int i = 0; i < boxOffices.size(); i++) {
                BoxOffice boxOffice = boxOffices.get(i);
                int newRank = i + 1;
                
                if (boxOffice.getRank() != newRank) {
                    log.info("순위 변경: {} ({}) {}위 -> {}위", 
                        boxOffice.getMovieNm(), boxOffice.getMovieCd(), 
                        boxOffice.getRank(), newRank);
                    boxOffice.setRank(newRank);
                    boxOfficeRepository.save(boxOffice);
                }
            }
        }
        
        log.info("박스오피스 데이터 정리 및 순위 재정렬 완료");
    }

    /**
     * 평균 별점이 높은 영화 TOP-N 조회
     */
    public List<MovieDetail> getTopRatedMovies(int limit) {
        return tmdbPosterService.getTopRatedMovies(limit);
    }
} 
