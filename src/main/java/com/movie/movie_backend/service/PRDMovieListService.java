package com.movie.service;

import com.movie.dto.MovieListDto;
import com.movie.entity.MovieList;
import com.movie.mapper.MovieListMapper;
import com.movie.repository.PRDMovieListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * 영화 상태별 조회
     */
    public List<MovieListDto> getMoviesByStatus(String status) {
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
    public long getMovieCountByStatus(String status) {
        return movieListRepository.findByStatus(status).size();
    }
} 