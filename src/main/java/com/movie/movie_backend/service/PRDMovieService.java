package com.movie.movie_backend.service;

import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.repository.PRDMovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PRDMovieService {
    private final PRDMovieRepository movieRepository;

    public MovieDetail registerMovieDetail(MovieDetail movieDetail) {
        return movieRepository.save(movieDetail);
    }

    public MovieDetail updateMovieDetail(String movieCd, MovieDetail update) {
        Optional<MovieDetail> optionalMovieDetail = movieRepository.findByMovieCd(movieCd);
        if (optionalMovieDetail.isPresent()) {
            MovieDetail movieDetail = optionalMovieDetail.get();
            movieDetail.setMovieNm(update.getMovieNm());
            movieDetail.setMovieNmEn(update.getMovieNmEn());
            movieDetail.setPrdtYear(update.getPrdtYear());
            movieDetail.setShowTm(update.getShowTm());
            movieDetail.setOpenDt(update.getOpenDt());
            movieDetail.setPrdtStatNm(update.getPrdtStatNm());
            movieDetail.setTypeNm(update.getTypeNm());
            movieDetail.setGenreNm(update.getGenreNm());
            movieDetail.setNationNm(update.getNationNm());
            movieDetail.setWatchGradeNm(update.getWatchGradeNm());
            movieDetail.setCompanyNm(update.getCompanyNm());
            movieDetail.setDescription(update.getDescription());
            movieDetail.setReservationRank(update.getReservationRank());
            movieDetail.setReservationRate(update.getReservationRate());
            movieDetail.setDaysSinceRelease(update.getDaysSinceRelease());
            movieDetail.setTotalAudience(update.getTotalAudience());
            return movieRepository.save(movieDetail);
        }
        return null;
    }

    public void deleteMovieDetail(String movieCd) {
        movieRepository.findByMovieCd(movieCd).ifPresent(movieRepository::delete);
    }

    public List<MovieDetail> getAllMovieDetails() {
        return movieRepository.findAll();
    }

    public Optional<MovieDetail> getMovieDetailByCode(String movieCd) {
        return movieRepository.findByMovieCd(movieCd);
    }

    /**
     * 띄어쓰기 무시 통합 검색 (제목, 감독, 배우, 장르)
     */
    public List<MovieDetail> searchMoviesIgnoreSpace(String keyword) {
        if (keyword == null) return List.of();
        String noSpace = keyword.replaceAll("\\s+", "");
        return movieRepository.searchIgnoreSpace(noSpace);
    }
} 
