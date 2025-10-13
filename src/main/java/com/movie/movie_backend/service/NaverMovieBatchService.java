package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.Director;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.repository.PRDMovieRepository;
import com.movie.movie_backend.repository.PRDDirectorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverMovieBatchService {

    private final PRDMovieListRepository movieListRepository;
    private final PRDMovieRepository movieDetailRepository;
    private final PRDDirectorRepository directorRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.api.client-id}")
    private String naverClientId;

    @Value("${naver.api.client-secret}")
    private String naverClientSecret;

    private static final String NAVER_SEARCH_URL = "https://openapi.naver.com/v1/search/movie.json";

    @Transactional
    public void updatePosterAndDirectorFromNaver() {
        List<MovieList> movieLists = new ArrayList<>();
        int page = 0, size = 1000;
        Page<MovieList> moviePage;
        do {
            moviePage = movieListRepository.findAll(PageRequest.of(page++, size));
            movieLists.addAll(moviePage.getContent());
        } while (moviePage.hasNext());
        int posterUpdated = 0, directorUpdated = 0;

        for (MovieList movie : movieLists) {
            boolean updated = false;

            // 1. 포스터가 없으면 네이버에서 시도
            if (movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()) {
                String posterUrl = fetchPosterUrlFromNaver(movie);
                if (posterUrl != null && !posterUrl.isEmpty()) {
                    movie.setPosterUrl(posterUrl);
                    posterUpdated++;
                    updated = true;
                    log.info("네이버 포스터 매칭 성공: {} ({}) -> {}", movie.getMovieNm(), movie.getOpenDt(), posterUrl);
                }
            }

            if (updated) {
                movieListRepository.save(movie);
            }

            // 2. 감독이 없으면 네이버에서 시도 (MovieDetail 기준)
            Optional<MovieDetail> detailOpt = movieDetailRepository.findByMovieCd(movie.getMovieCd());
            if (detailOpt.isPresent()) {
                MovieDetail detail = detailOpt.get();
                if (detail.getDirector() == null || detail.getDirector().getName() == null || detail.getDirector().getName().isEmpty()) {
                    String directorName = fetchDirectorFromNaver(movie);
                    if (directorName != null && !directorName.isEmpty()) {
                        // 기존에 동일한 이름의 Director가 있으면 재사용, 없으면 새로 생성
                        Director director = directorRepository.findByName(directorName).orElseGet(() -> {
                            Director newDirector = Director.builder().name(directorName).build();
                            return directorRepository.save(newDirector);
                        });
                        detail.setDirector(director);
                        movieDetailRepository.save(detail);
                        directorUpdated++;
                        log.info("네이버 감독 매칭 성공: {} ({}) -> {}", movie.getMovieNm(), movie.getOpenDt(), directorName);
                    }
                }
            }
        }
        log.info("네이버 포스터 보완 완료: {}건, 감독 보완: {}건", posterUpdated, directorUpdated);
    }

    private String fetchPosterUrlFromNaver(MovieList movie) {
        try {
            String query = URLEncoder.encode(movie.getMovieNm(), StandardCharsets.UTF_8);
            String year = (movie.getOpenDt() != null) ? String.valueOf(movie.getOpenDt().getYear()) : "";
            String url = NAVER_SEARCH_URL + "?query=" + query + "&yearfrom=" + year + "&yearto=" + year;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.get("items");
            if (items != null && items.size() > 0) {
                String imageUrl = items.get(0).get("image").asText();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    return imageUrl;
                }
            }
        } catch (Exception e) {
            log.warn("네이버 포스터 검색 오류: {} ({}) - {}", movie.getMovieNm(), movie.getOpenDt(), e.getMessage());
        }
        return null;
    }

    private String fetchDirectorFromNaver(MovieList movie) {
        try {
            String query = URLEncoder.encode(movie.getMovieNm(), StandardCharsets.UTF_8);
            String year = (movie.getOpenDt() != null) ? String.valueOf(movie.getOpenDt().getYear()) : "";
            String url = NAVER_SEARCH_URL + "?query=" + query + "&yearfrom=" + year + "&yearto=" + year;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.get("items");
            if (items != null && items.size() > 0) {
                String director = items.get(0).get("director").asText();
                if (director != null && !director.isEmpty()) {
                    // 네이버는 여러 감독명을 |로 구분해서 내려줌
                    return director.replaceAll("\\|", ", ").replaceAll(", $", "");
                }
            }
        } catch (Exception e) {
            log.warn("네이버 감독 검색 오류: {} ({}) - {}", movie.getMovieNm(), movie.getOpenDt(), e.getMessage());
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
