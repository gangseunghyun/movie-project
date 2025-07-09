package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.MovieListDto;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.mapper.MovieListMapper;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.movie.movie_backend.constant.MovieStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PRDMovieListService {

    private final PRDMovieListRepository movieListRepository;
    private final MovieListMapper movieListMapper;

    /**
     * 영화 목록 저장 (중복 체크)
     */
    @Transactional
    public void saveMovieList(List<MovieListDto> movieListDtos) {
        log.info("영화 목록 저장 시작: {}개", movieListDtos.size());
        
        int savedCount = 0;
        int skippedCount = 0;
        
        for (MovieListDto dto : movieListDtos) {
            try {
                // 이미 존재하는지 확인
                if (!movieListRepository.existsByMovieCd(dto.getMovieCd())) {
                    MovieList movieList = movieListMapper.toEntity(dto);
                    movieListRepository.save(movieList);
                    savedCount++;
                    log.debug("영화 저장 완료: {} - {}", dto.getMovieCd(), dto.getMovieNm());
                } else {
                    skippedCount++;
                    log.debug("영화 이미 존재: {} - {}", dto.getMovieCd(), dto.getMovieNm());
                }
            } catch (Exception e) {
                log.error("영화 저장 실패: {} - {}", dto.getMovieCd(), dto.getMovieNm(), e);
            }
        }
        
        log.info("영화 목록 저장 완료: 저장={}, 건너뜀={}", savedCount, skippedCount);
    }

    /**
     * 영화 코드로 영화 조회
     */
    public Optional<MovieListDto> getMovieByCode(String movieCd) {
        return movieListRepository.findByMovieCd(movieCd)
                .map(movieListMapper::toDto);
    }

    /**
     * 모든 영화 목록 조회
     */
    public List<MovieListDto> getAllMovies() {
        return movieListRepository.findAll().stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 영화명으로 검색
     */
    public List<MovieListDto> searchMoviesByName(String movieNm) {
        return movieListRepository.findByMovieNmContainingIgnoreCase(movieNm).stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 띄어쓰기 무시 통합 검색 (제목, 장르, 국가)
     */
    public List<MovieListDto> searchMoviesIgnoreSpace(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        String noSpace = keyword.replaceAll("\\s+", "");
        List<MovieList> results = new ArrayList<>();
        
        // 1. 제목으로 검색
        List<MovieList> titleResults = movieListRepository.findByMovieNmContainingIgnoreCase(keyword);
        results.addAll(titleResults);
        
        // 2. 장르로 검색
        List<MovieList> genreResults = movieListRepository.findByGenreNmContaining(keyword);
        for (MovieList movie : genreResults) {
            if (!results.contains(movie)) {
                results.add(movie);
            }
        }
        
        // 3. 국가로 검색
        List<MovieList> nationResults = movieListRepository.findByNationNmContaining(keyword);
        for (MovieList movie : nationResults) {
            if (!results.contains(movie)) {
                results.add(movie);
            }
        }
        
        // 4. 띄어쓰기 제거 후 검색
        List<MovieList> noSpaceResults = movieListRepository.findAll().stream()
                .filter(movie -> {
                    String movieNmNoSpace = movie.getMovieNm() != null ? movie.getMovieNm().replaceAll("\\s+", "") : "";
                    String genreNmNoSpace = movie.getGenreNm() != null ? movie.getGenreNm().replaceAll("\\s+", "") : "";
                    String nationNmNoSpace = movie.getNationNm() != null ? movie.getNationNm().replaceAll("\\s+", "") : "";
                    
                    return movieNmNoSpace.contains(noSpace) || 
                           genreNmNoSpace.contains(noSpace) || 
                           nationNmNoSpace.contains(noSpace);
                })
                .collect(Collectors.toList());
        
        for (MovieList movie : noSpaceResults) {
            if (!results.contains(movie)) {
                results.add(movie);
            }
        }
        
        return results.stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 영화 상태별 조회
     */
    public List<MovieListDto> getMoviesByStatus(MovieStatus status) {
        return movieListRepository.findByStatus(status).stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 장르별 영화 조회
     */
    public List<MovieListDto> getMoviesByGenre(String genreNm) {
        return movieListRepository.findByGenreNmContaining(genreNm).stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 국가별 영화 조회
     */
    public List<MovieListDto> getMoviesByNation(String nationNm) {
        return movieListRepository.findByNationNmContaining(nationNm).stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 최신 영화 조회
     */
    public List<MovieListDto> getLatestMovies() {
        return movieListRepository.findLatestMovies().stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 영화 코드 목록으로 조회
     */
    public List<MovieListDto> getMoviesByCodes(List<String> movieCds) {
        return movieListRepository.findByMovieCdIn(movieCds).stream()
                .map(movieListMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 전체 영화 수 조회
     */
    public long getTotalMovieCount() {
        return movieListRepository.count();
    }

    /**
     * 영화 상태별 개수 조회
     */
    public long getMovieCountByStatus(MovieStatus status) {
        return movieListRepository.findByStatus(status).size();
    }
} 
