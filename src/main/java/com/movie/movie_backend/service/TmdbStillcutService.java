package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.Stillcut;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbStillcutService {

    private final PRDMovieListRepository movieListRepository;
    private final PRDMovieRepository movieRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/movie";
    private static final String TMDB_IMAGES_URL = "https://api.themoviedb.org/3/movie/{movie_id}/images";

    /**
     * MovieList를 기반으로 모든 영화의 스틸컷을 가져와서 MovieDetail에 저장
     */
    @Transactional
    public void updateStillcutsForAllMovies() {
        List<MovieList> movieLists = new ArrayList<>();
        int page = 0, size = 1000;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page++, size));
            movieLists.addAll(moviePage.getContent());
        } while (moviePage.hasNext());

        int updated = 0, failed = 0;

        for (MovieList movieList : movieLists) {
            try {
                Optional<MovieDetail> movieDetailOpt = movieRepository.findByMovieCd(movieList.getMovieCd());
                if (movieDetailOpt.isPresent()) {
                    MovieDetail movieDetail = movieDetailOpt.get();
                    // 이미 스틸컷이 있으면 건너뜀
                    if (movieDetail.getStillcuts() != null && !movieDetail.getStillcuts().isEmpty()) continue;
                    List<Stillcut> stillcuts = fetchStillcutsFromTmdb(movieList, movieDetail);
                    if (!stillcuts.isEmpty()) {
                        // 기존 스틸컷 제거
                        movieDetail.getStillcuts().clear();
                        
                        // 새 스틸컷들을 MovieDetail에 추가
                        for (Stillcut stillcut : stillcuts) {
                            stillcut.setMovieDetail(movieDetail);
                            movieDetail.getStillcuts().add(stillcut);
                        }
                        
                        movieRepository.save(movieDetail);
                        updated++;
                        log.info("스틸컷 매칭 성공: {} ({}) -> {}개", movieList.getMovieNm(), movieList.getOpenDt(), stillcuts.size());
                    } else {
                        failed++;
                        log.warn("스틸컷 매칭 실패: {} ({})", movieList.getMovieNm(), movieList.getOpenDt());
                    }
                } else {
                    failed++;
                    log.warn("MovieDetail을 찾을 수 없음: {} ({})", movieList.getMovieNm(), movieList.getMovieCd());
                }
            } catch (Exception e) {
                failed++;
                log.error("스틸컷 처리 오류: {} ({}) - {}", movieList.getMovieNm(), movieList.getOpenDt(), e.getMessage());
            }
        }
        log.info("TMDB 스틸컷 자동 매칭 완료: 성공 {}건, 실패 {}건", updated, failed);
    }

    /**
     * 특정 영화의 스틸컷을 가져와서 저장
     */
    @Transactional
    public List<Stillcut> fetchAndSaveStillcuts(String movieCd) {
        Optional<MovieList> movieListOpt = movieListRepository.findById(movieCd);
        if (movieListOpt.isEmpty()) {
            log.warn("MovieList를 찾을 수 없음: {}", movieCd);
            return new ArrayList<>();
        }

        MovieList movieList = movieListOpt.get();
        Optional<MovieDetail> movieDetailOpt = movieRepository.findByMovieCd(movieCd);
        
        if (movieDetailOpt.isEmpty()) {
            log.warn("MovieDetail을 찾을 수 없음: {}", movieCd);
            return new ArrayList<>();
        }

        MovieDetail movieDetail = movieDetailOpt.get();
        List<Stillcut> stillcuts = fetchStillcutsFromTmdb(movieList, movieDetail);
        
        if (!stillcuts.isEmpty()) {
            // 기존 스틸컷 제거
            movieDetail.getStillcuts().clear();
            
            // 새 스틸컷들을 MovieDetail에 추가
            for (Stillcut stillcut : stillcuts) {
                stillcut.setMovieDetail(movieDetail);
                movieDetail.getStillcuts().add(stillcut);
            }
            
            movieRepository.save(movieDetail);
            log.info("스틸컷 저장 완료: {} -> {}개", movieList.getMovieNm(), stillcuts.size());
        }
        
        return stillcuts;
    }

    /**
     * TMDB에서 영화 스틸컷을 가져오기 (MovieList 기반)
     */
    private List<Stillcut> fetchStillcutsFromTmdb(MovieList movieList, MovieDetail movieDetail) {
        List<Stillcut> stillcuts = new ArrayList<>();
        
        try {
            // 1단계: 영화 검색으로 TMDB movie_id 찾기
            Integer tmdbMovieId = findTmdbMovieId(movieList);
            if (tmdbMovieId == null) {
                return stillcuts;
            }

            // 2단계: 영화 이미지 API 호출
            String url = TMDB_IMAGES_URL.replace("{movie_id}", tmdbMovieId.toString()) +
                    "?api_key=" + tmdbApiKey;

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            
            // backdrop 이미지들 (스틸컷으로 사용)
            JsonNode backdrops = root.get("backdrops");
            if (backdrops != null && backdrops.isArray()) {
                int order = 1;
                for (JsonNode backdrop : backdrops) {
                    String filePath = backdrop.get("file_path").asText();
                    if (filePath != null && !filePath.isEmpty()) {
                        String imageUrl = "https://image.tmdb.org/t/p/original" + filePath;
                        
                        Stillcut stillcut = Stillcut.builder()
                                .imageUrl(imageUrl)
                                .orderInMovie(order++)
                                .movieDetail(movieDetail) // MovieDetail 객체 설정
                                .build();
                        
                        stillcuts.add(stillcut);
                        
                        // 최대 10개까지만 가져오기
                        if (stillcuts.size() >= 10) break;
                    }
                }
            }

            // backdrop이 없으면 poster를 스틸컷으로 사용
            if (stillcuts.isEmpty()) {
                JsonNode posters = root.get("posters");
                if (posters != null && posters.isArray() && posters.size() > 0) {
                    JsonNode poster = posters.get(0);
                    String filePath = poster.get("file_path").asText();
                    if (filePath != null && !filePath.isEmpty()) {
                        String imageUrl = "https://image.tmdb.org/t/p/w500" + filePath;
                        
                        Stillcut stillcut = Stillcut.builder()
                                .imageUrl(imageUrl)
                                .orderInMovie(1)
                                .movieDetail(movieDetail) // MovieDetail 객체 설정
                                .build();
                        
                        stillcuts.add(stillcut);
                    }
                }
            }

        } catch (Exception e) {
            log.warn("TMDB 스틸컷 검색 오류: {} ({}) - {}", movieList.getMovieNm(), movieList.getOpenDt(), e.getMessage());
        }
        
        return stillcuts;
    }

    /**
     * 영화 제목으로 TMDB movie_id 찾기 (MovieList 기반)
     */
    private Integer findTmdbMovieId(MovieList movieList) {
        try {
            String query = URLEncoder.encode(movieList.getMovieNm(), StandardCharsets.UTF_8);
            String year = (movieList.getOpenDt() != null) ? String.valueOf(movieList.getOpenDt().getYear()) : null;
            
            String url = TMDB_SEARCH_URL +
                    "?api_key=" + tmdbApiKey +
                    "&query=" + query +
                    (year != null ? "&year=" + year : "") +
                    "&language=ko-KR";

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");
            
            if (results != null && results.size() > 0) {
                return results.get(0).get("id").asInt();
            }

            // 2차 시도: 영문 제목으로 검색
            if (movieList.getMovieNmEn() != null && !movieList.getMovieNmEn().isEmpty()) {
                query = URLEncoder.encode(movieList.getMovieNmEn(), StandardCharsets.UTF_8);
                url = TMDB_SEARCH_URL +
                        "?api_key=" + tmdbApiKey +
                        "&query=" + query +
                        (year != null ? "&year=" + year : "") +
                        "&language=en-US";
                
                response = restTemplate.getForObject(url, String.class);
                root = objectMapper.readTree(response);
                results = root.get("results");
                
                if (results != null && results.size() > 0) {
                    return results.get(0).get("id").asInt();
                }
            }

        } catch (Exception e) {
            log.warn("TMDB movie_id 검색 오류: {} ({}) - {}", movieList.getMovieNm(), movieList.getOpenDt(), e.getMessage());
        }
        
        return null;
    }

    public List<MovieList> getAllMovieListsPaged(int chunkSize) {
        List<MovieList> result = new ArrayList<>();
        int page = 0;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page, chunkSize));
            result.addAll(moviePage.getContent());
            page++;
        } while (!moviePage.isLast());
        return result;
    }
} 
