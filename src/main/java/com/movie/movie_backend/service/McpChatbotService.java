package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.McpToolResponseDto;
import com.movie.movie_backend.dto.MovieDetailDto;
import com.movie.movie_backend.dto.MovieListDto;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import com.movie.movie_backend.entity.MovieDetail;
import com.movie.movie_backend.entity.MovieList;
import com.movie.movie_backend.repository.MovieDetailRepository;
import com.movie.movie_backend.repository.PRDMovieListRepository;
import com.movie.movie_backend.mapper.MovieDetailMapper;
import com.movie.movie_backend.mapper.MovieListMapper;
import com.movie.movie_backend.constant.MovieStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
public class McpChatbotService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private MovieDetailRepository movieDetailRepository;
    @Autowired
    private PRDMovieListRepository movieListRepository;
    @Autowired
    private MovieDetailMapper movieDetailMapper;
    @Autowired
    private MovieListMapper movieListMapper;
    @Autowired
    private TmdbPopularMovieService tmdbPopularMovieService;
    @Autowired
    private KobisPopularMovieService kobisPopularMovieService;
    @Autowired
    private BoxOfficeService boxOfficeService;
    
    /**
     * MCP 도구 요청을 처리하는 메서드
     * AI가 MCP 도구를 사용할 때 호출됨
     */
    public McpToolResponseDto handleMcpToolRequest(String tool, Map<String, Object> parameters) {
        try {
            if ("searchMovies".equals(tool)) {
                return handleSearchMoviesRequest(parameters);
            } else if ("getMovieInfo".equals(tool)) {
                return handleGetMovieInfoRequest(parameters);
            } else {
                return McpToolResponseDto.builder()
                        .tool(tool)
                        .success(false)
                        .error("지원하지 않는 도구입니다: " + tool)
                        .build();
            }
        } catch (Exception e) {
            return McpToolResponseDto.builder()
                    .tool(tool)
                    .success(false)
                    .error("도구 실행 중 오류 발생: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * searchMovies 도구 요청 처리
     * 실제 DB에서 영화 검색 결과를 반환
     */
    private McpToolResponseDto handleSearchMoviesRequest(Map<String, Object> parameters) {
        List<MovieDetailDto> movies = new ArrayList<>();
        String type = parameters.get("type") != null ? parameters.get("type").toString() : null;
        String genre = parameters.get("genre") != null ? parameters.get("genre").toString() : null;
        String situation = parameters.get("situation") != null ? parameters.get("situation").toString() : null;

        // 박스오피스 분기 추가
        if ("boxoffice".equals(type)) {
            System.out.println("박스오피스 TOP5 추천 분기");
            List<com.movie.movie_backend.dto.BoxOfficeDto> boxOfficeTop5 =
                boxOfficeService.getDailyBoxOfficeTop10AsDto().stream().limit(5).collect(java.util.stream.Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("movies", boxOfficeTop5);
            result.put("totalCount", boxOfficeTop5.size());
            return McpToolResponseDto.builder()
                .tool("searchMovies")
                .success(true)
                .result(result)
                .build();
        }

        // 로그 추가: 각 분기별 동작 확인
        System.out.println("=== 챗봇 추천 분기 로그 ===");
        System.out.println("type: " + type + ", genre: " + genre + ", situation: " + situation);

        try {
            if (situation != null) {
                System.out.println("상황별 추천 분기: " + situation);
                if ("우울할 때".equals(situation) || "힐링이 필요할 때".equals(situation) || "슬플 때".equals(situation)) {
                    System.out.println("→ 힐링 영화 추천 실행");
                    movies.addAll(getHealingMovies());
                } else if ("기분이 좋을 때".equals(situation)) {
                    System.out.println("→ 로맨스 영화 추천 실행");
                    movies.addAll(getRomanceMovies());
                } else if ("스트레스 받을 때".equals(situation)) {
                    System.out.println("→ 액션+코미디 영화 추천 실행");
                    movies.addAll(getActionMovies());
                    movies.addAll(getComedyMovies());
                } else {
                    System.out.println("→ 일반 인기 영화 추천 실행");
                    movies.addAll(getRealPopularMovies());
                }
            } else if (genre != null) {
                System.out.println("장르별 추천 분기: " + genre);
                // 콤마로 여러 장르가 들어오면 각각 추천
                String[] genres = genre.split(",");
                for (String g : genres) {
                    String trimmed = g.trim();
                    System.out.println("→ 장르 처리: " + trimmed);
                    if ("로맨스".equals(trimmed)) {
                        movies.addAll(getRomanceMovies());
                    } else if ("액션".equals(trimmed)) {
                        movies.addAll(getActionMovies());
                    } else if ("코미디".equals(trimmed)) {
                        movies.addAll(getComedyMovies());
                    } else if ("공포".equals(trimmed) || "호러".equals(trimmed)) {
                        movies.addAll(getHorrorMovies());
                    } else if ("스릴러".equals(trimmed) || "thriller".equalsIgnoreCase(trimmed)) {
                        movies.addAll(getThrillerMovies());
                    }
                }
            } else if (type != null) {
                System.out.println("타입별 추천 분기: " + type);
                switch (type) {
                    case "coming_soon":
                        System.out.println("→ 개봉예정 영화 추천 실행");
                        movies.addAll(getComingSoonMovies());
                        break;
                    case "latest":
                        System.out.println("→ 최신 영화 추천 실행");
                        movies.addAll(getLatestMovies());
                        break;
                    case "popular":
                        System.out.println("→ 인기 영화 추천 실행");
                        movies.addAll(getPopularMovies());
                        break;
                    default:
                        System.out.println("→ 기본 인기 영화 추천 실행");
                        movies.addAll(getRealPopularMovies());
                }
            } else {
                System.out.println("기본 추천 분기: 다양한 카테고리 혼합");
                // 기본 추천: 다양한 카테고리 혼합
                movies.addAll(getRealPopularMovies());
                movies.addAll(getLatestMovies());
                movies.addAll(getRomanceMovies());
                movies.addAll(getActionMovies());
                movies.addAll(getComedyMovies());
            }
            
            System.out.println("최종 추천 영화 수: " + movies.size());
            System.out.println("=== 분기 로그 끝 ===\n");
        } catch (Exception e) {
            System.err.println("Error in handleSearchMoviesRequest: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("movies", movies);
        result.put("totalCount", movies.size());

        return McpToolResponseDto.builder()
                .tool("searchMovies")
                .success(true)
                .result(result)
                .build();
    }
    
    /**
     * 실제 DB에서 최신 인기 영화 가져오기
     */
    private List<MovieDetailDto> getRealPopularMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> popularMovies = movieDetailRepository.findTop20ByOrderByTotalAudienceDesc();
            popularMovies = popularMovies.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : popularMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching popular movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * 실제 DB에서 로맨스 영화 가져오기
     */
    private List<MovieDetailDto> getRomanceMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> romanceMovies = movieDetailRepository.findByGenreNmContaining("로맨스");
            romanceMovies = romanceMovies.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : romanceMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching romance movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * 실제 DB에서 액션 영화 가져오기
     */
    private List<MovieDetailDto> getActionMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> actionMovies = movieDetailRepository.findByGenreNmContaining("액션");
            actionMovies = actionMovies.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : actionMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching action movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * 실제 DB에서 힐링 영화 가져오기
     */
    private List<MovieDetailDto> getHealingMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            System.out.println("힐링 영화 검색 시작...");
            List<MovieDetail> healingMovies = new ArrayList<>();
            healingMovies.addAll(movieDetailRepository.findByGenreNmContaining("로맨스"));
            healingMovies.addAll(movieDetailRepository.findByGenreNmContaining("코미디"));
            healingMovies.addAll(movieDetailRepository.findByGenreNmContaining("드라마"));
            healingMovies = healingMovies.stream().distinct().limit(5).collect(Collectors.toList());
            System.out.println("DB에서 가져온 힐링 영화 수: " + healingMovies.size());

            for (MovieDetail detail : healingMovies) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
                System.out.println("힐링 영화 추가: " + detail.getMovieNm() + " (" + detail.getMovieCd() + ")");
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            System.out.println("중복 제거 후 힐링 영화 수: " + movies.size());
        } catch (Exception e) {
            System.err.println("Error fetching healing movies: " + e.getMessage());
        }
        return movies;
    }
    
    /**
     * getMovieInfo 도구 요청 처리
     * MovieList에서 포스터 URL과 함께 영화 상세 정보를 반환
     */
    private McpToolResponseDto handleGetMovieInfoRequest(Map<String, Object> parameters) {
        String movieName = (String) parameters.get("movieCd"); // 실제로는 영화명
        String redisKey = "movie:info:" + movieName;

        // 1. 정확한 영화명으로 검색
        List<MovieList> exactMatches = movieListRepository.findByMovieNmContainingIgnoreCase(movieName);
        
        // 2. 유사도 기반 검색 (새로운 메서드 사용)
        List<MovieList> similarMatches = movieListRepository.findByMovieNmSimilar(movieName);
        
        // 3. 띄어쓰기 무시 검색
        List<MovieList> spaceIgnoreMatches = movieListRepository.findByMovieNmIgnoreSpace(movieName);
        
        // 4. 단어별 검색 (2글자 이상인 단어만)
        List<MovieList> wordMatches = new ArrayList<>();
        String[] words = movieName.split("\\s+");
        for (String word : words) {
            if (word.length() >= 2) {
                wordMatches.addAll(movieListRepository.findByMovieNmContainingWord(word));
            }
        }
        
        // 5. 모든 결과 합치기 및 중복 제거
        List<MovieList> allMatches = new ArrayList<>();
        allMatches.addAll(exactMatches);
        allMatches.addAll(similarMatches);
        allMatches.addAll(spaceIgnoreMatches);
        allMatches.addAll(wordMatches);
        
        // 중복 제거
        allMatches = allMatches.stream().distinct().collect(Collectors.toList());
        
        // 6. 유사도 점수 계산 및 정렬
        allMatches.sort((a, b) -> {
            double scoreA = calculateSimilarityScore(movieName, a.getMovieNm());
            double scoreB = calculateSimilarityScore(movieName, b.getMovieNm());
            return Double.compare(scoreB, scoreA); // 내림차순
        });
        
        // 7. 결과 처리
        if (!allMatches.isEmpty()) {
            MovieList movieList = allMatches.get(0); // 가장 유사한 영화
            String movieCd = movieList.getMovieCd();
            MovieListDto movieListDto = movieListMapper.toDto(movieList);
            MovieDetail movieDetail = movieDetailRepository.findByMovieCd(movieCd);
            MovieDetailDto movie;
            if (movieDetail != null) {
                movie = movieDetailMapper.toDto(movieDetail, 0, false);
                if (movieListDto.getPosterUrl() != null && !movieListDto.getPosterUrl().isEmpty()) {
                    movie.setPosterUrl(movieListDto.getPosterUrl());
                }
            } else {
                movie = createMovieDetailDtoFromMovieList(movieListDto);
            }
            redisTemplate.opsForValue().set(redisKey, movie, java.time.Duration.ofHours(12));
            return buildMovieInfoResponse(movie, true, null);
        }

        // 결과 없음
        return buildMovieInfoResponse(null, false, "해당 영화 정보를 찾을 수 없습니다.");
    }
    
    /**
     * 영화명 유사도 점수 계산 (개선된 버전)
     */
    private double calculateSimilarityScore(String searchTerm, String movieTitle) {
        if (searchTerm == null || movieTitle == null) return 0.0;
        
        String searchLower = searchTerm.toLowerCase().trim();
        String titleLower = movieTitle.toLowerCase().trim();
        
        // 1. 정확한 일치 (가장 높은 점수)
        if (searchLower.equals(titleLower)) {
            return 1000.0;
        }
        
        // 2. 정확한 포함 여부
        if (titleLower.contains(searchLower)) {
            return 500.0;
        }
        
        // 3. 띄어쓰기 제거 후 포함 여부
        String searchNoSpace = searchLower.replaceAll("\\s+", "");
        String titleNoSpace = titleLower.replaceAll("\\s+", "");
        if (titleNoSpace.contains(searchNoSpace)) {
            return 400.0;
        }
        
        // 4. 단어별 매칭 점수
        String[] searchWords = searchLower.split("\\s+");
        String[] titleWords = titleLower.split("\\s+");
        
        double wordMatchScore = 0.0;
        int matchedWords = 0;
        
        for (String searchWord : searchWords) {
            if (searchWord.length() < 2) continue;
            for (String titleWord : titleWords) {
                if (titleWord.contains(searchWord) || searchWord.contains(titleWord)) {
                    wordMatchScore += 50.0;
                    matchedWords++;
                }
            }
        }
        
        // 5. 매칭된 단어 비율에 따른 보너스
        double wordRatioBonus = 0.0;
        if (searchWords.length > 0) {
            double ratio = (double) matchedWords / searchWords.length;
            wordRatioBonus = ratio * 100.0;
        }
        
        // 6. 길이 유사도 보너스
        double lengthBonus = 0.0;
        int lengthDiff = Math.abs(searchTerm.length() - movieTitle.length());
        if (lengthDiff <= 5) {
            lengthBonus = 20.0;
        } else if (lengthDiff <= 10) {
            lengthBonus = 10.0;
        }
        
        // 7. 시작 부분 일치 보너스
        double startBonus = 0.0;
        if (titleLower.startsWith(searchLower) || searchLower.startsWith(titleLower)) {
            startBonus = 30.0;
        }
        
        return wordMatchScore + wordRatioBonus + lengthBonus + startBonus;
    }
    
    /**
     * MovieList 정보로 MovieDetailDto 생성
     */
    private MovieDetailDto createMovieDetailDtoFromMovieList(MovieListDto movieListDto) {
        return MovieDetailDto.builder()
                .movieCd(movieListDto.getMovieCd())
                .movieNm(movieListDto.getMovieNm())
                .movieNmEn(movieListDto.getMovieNmEn())
                .openDt(movieListDto.getOpenDt())
                .genreNm(movieListDto.getGenreNm())
                .nationNm(movieListDto.getNationNm())
                .watchGradeNm(movieListDto.getWatchGradeNm())
                .status(movieListDto.getStatus() != null ? movieListDto.getStatus().toString() : null)
                .posterUrl(movieListDto.getPosterUrl())
                .description("영화에 대한 상세한 정보를 제공합니다.")
                .directors(new ArrayList<>())
                .actors(new ArrayList<>())
                .build();
    }
    
    private McpToolResponseDto buildMovieInfoResponse(MovieDetailDto movie, boolean success, String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("movie", movie);
        return McpToolResponseDto.builder()
                .tool("getMovieInfo")
                .success(success)
                .result(result)
                .error(error)
                .build();
    }

    // 스릴러 추천
    private List<MovieDetailDto> getThrillerMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> thriller = movieDetailRepository.findByGenreNmContaining("스릴러");
            thriller = thriller.stream().distinct().limit(10).collect(Collectors.toList());
            Collections.shuffle(thriller);
            thriller = thriller.stream().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : thriller) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching thriller movies: " + e.getMessage());
        }
        return movies;
    }

    // 상영중인 영화 추천 (NOW_PLAYING 상태)
    private List<MovieDetailDto> getLatestMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            System.out.println("상영중인 영화 검색 시작...");
            
            // 전체 MovieList의 상태별 개수 확인
            List<MovieList> allMovies = movieListRepository.findAll();
            System.out.println("전체 MovieList 수: " + allMovies.size());
            
            long nowPlayingCount = allMovies.stream().filter(m -> m.getStatus() == com.movie.movie_backend.constant.MovieStatus.NOW_PLAYING).count();
            long comingSoonCount = allMovies.stream().filter(m -> m.getStatus() == com.movie.movie_backend.constant.MovieStatus.COMING_SOON).count();
            long endedCount = allMovies.stream().filter(m -> m.getStatus() == com.movie.movie_backend.constant.MovieStatus.ENDED).count();
            long nullCount = allMovies.stream().filter(m -> m.getStatus() == null).count();
            
            System.out.println("상태별 영화 수 - NOW_PLAYING: " + nowPlayingCount + ", COMING_SOON: " + comingSoonCount + ", ENDED: " + endedCount + ", NULL: " + nullCount);
            
            // NOW_PLAYING 상태 영화 찾기
            List<MovieList> nowPlaying = movieListRepository.findByStatus(com.movie.movie_backend.constant.MovieStatus.NOW_PLAYING);
            System.out.println("NOW_PLAYING 상태 영화 수: " + nowPlaying.size());
            
            nowPlaying = nowPlaying.stream().limit(20).collect(Collectors.toList());
            Collections.shuffle(nowPlaying);
            nowPlaying = nowPlaying.stream().limit(5).collect(Collectors.toList());
            
            for (MovieList movieList : nowPlaying) {
                MovieDetail detail = movieDetailRepository.findByMovieCd(movieList.getMovieCd());
                if (detail != null) {
                    MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                    movies.add(dto);
                    System.out.println("상영중인 영화 추가: " + movieList.getMovieNm() + " (상태: " + movieList.getStatus() + ")");
                }
            }
            
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            
            System.out.println("상영중인 영화 최종 수: " + movies.size());
            
            // 영화를 찾지 못한 경우 대체 로직
            if (movies.isEmpty()) {
                System.out.println("상영중인 영화를 찾지 못해 인기 영화로 대체");
                movies.addAll(getRealPopularMovies());
            }
        } catch (Exception e) {
            System.err.println("Error fetching latest movies: " + e.getMessage());
        }
        return movies;
    }

    // 개봉예정 추천 (랜덤 섞기)
    private List<MovieDetailDto> getComingSoonMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            System.out.println("개봉예정 영화 검색 시작...");
            List<MovieList> comingSoon = movieListRepository.findByStatus(com.movie.movie_backend.constant.MovieStatus.COMING_SOON);
            System.out.println("COMING_SOON 상태 영화 수: " + comingSoon.size());
            
            comingSoon = comingSoon.stream().limit(20).collect(Collectors.toList());
            Collections.shuffle(comingSoon);
            comingSoon = comingSoon.stream().limit(5).collect(Collectors.toList());
            
            for (MovieList movieList : comingSoon) {
                MovieDetail detail = movieDetailRepository.findByMovieCd(movieList.getMovieCd());
                if (detail != null) {
                    MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                    movies.add(dto);
                    System.out.println("개봉예정 영화 추가: " + movieList.getMovieNm() + " (상태: " + movieList.getStatus() + ")");
                }
            }
            
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            
            System.out.println("개봉예정 영화 최종 수: " + movies.size());
            
            // 영화를 찾지 못한 경우 대체 로직
            if (movies.isEmpty()) {
                System.out.println("개봉예정 영화를 찾지 못해 최신 영화로 대체");
                // 개봉일이 미래인 영화들을 찾아보기
                List<MovieDetail> futureMovies = movieDetailRepository.findAll().stream()
                    .filter(m -> m.getOpenDt() != null && m.getOpenDt().isAfter(java.time.LocalDate.now()))
                    .limit(5)
                    .collect(Collectors.toList());
                
                for (MovieDetail detail : futureMovies) {
                    MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                    MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                    if (movieList != null) {
                        dto.setPosterUrl(movieList.getPosterUrl());
                        dto.setStatus("COMING_SOON"); // 강제로 COMING_SOON 상태 설정
                    }
                    movies.add(dto);
                    System.out.println("미래 개봉 영화 추가: " + detail.getMovieNm() + " (개봉일: " + detail.getOpenDt() + ")");
                }
                
                // 그래도 없으면 인기 영화로 대체
                if (movies.isEmpty()) {
                    System.out.println("미래 개봉 영화도 없어 인기 영화로 대체");
                    movies.addAll(getRealPopularMovies());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching coming soon movies: " + e.getMessage());
        }
        return movies;
    }

    // 인기순 추천 (관객수 내림차순, 랜덤 섞기)
    private List<MovieDetailDto> getPopularMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> popular = movieDetailRepository.findTop20ByOrderByTotalAudienceDesc();
            popular = popular.stream().distinct().limit(20).collect(Collectors.toList());
            Collections.shuffle(popular);
            popular = popular.stream().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : popular) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching popular movies: " + e.getMessage());
        }
        return movies;
    }

    // 코미디 추천
    private List<MovieDetailDto> getComedyMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> comedy = movieDetailRepository.findByGenreNmContaining("코미디");
            comedy = comedy.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : comedy) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching comedy movies: " + e.getMessage());
        }
        return movies;
    }

    // 공포/호러 추천
    private List<MovieDetailDto> getHorrorMovies() {
        List<MovieDetailDto> movies = new ArrayList<>();
        try {
            List<MovieDetail> horror = movieDetailRepository.findByGenreNmContaining("공포");
            horror.addAll(movieDetailRepository.findByGenreNmContaining("호러"));
            horror = horror.stream().distinct().limit(5).collect(Collectors.toList());
            for (MovieDetail detail : horror) {
                MovieList movieList = movieListRepository.findById(detail.getMovieCd()).orElse(null);
                MovieDetailDto dto = movieDetailMapper.toDto(detail, 0, false);
                if (movieList != null) {
                    dto.setPosterUrl(movieList.getPosterUrl());
                    dto.setStatus(movieList.getStatus() != null ? movieList.getStatus().name() : null);
                }
                movies.add(dto);
            }
            // 최종 중복 제거 (movieCd 기준)
            movies = movies.stream()
                .collect(Collectors.toMap(
                    MovieDetailDto::getMovieCd,
                    movie -> movie,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching horror movies: " + e.getMessage());
        }
        return movies;
    }
}