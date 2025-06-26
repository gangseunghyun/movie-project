# 🎬 Movie API Documentation

## 📋 개요
영화 정보 및 박스오피스 API (왓챠피디아 스타일)

## 🔗 기본 정보
- **Base URL**: `http://localhost`
- **Swagger UI**: `http://localhost/swagger-ui.html`

## 🚀 주요 API 엔드포인트

### 📺 영화 목록 관련

#### 1. MovieList 데이터 조회
```javascript
// 기본 영화 목록 (페이지네이션)
GET /data/api/movie-list?page=0&size=20

// 응답 예시
{
  "data": [...],           // 영화 목록
  "total": 100,           // 전체 개수
  "page": 0,              // 현재 페이지
  "size": 20,             // 페이지 크기
  "totalPages": 5         // 전체 페이지 수
}
```

#### 2. MovieList DTO 조회 (왓챠피디아 스타일)
```javascript
// 포스터 URL이 포함된 영화 목록
GET /data/api/movie-list-dto?page=0&size=20

// 응답 예시
{
  "data": [
    {
      "movieCd": "20201234",
      "movieNm": "영화제목",
      "movieNmEn": "Movie Title",
      "openDt": "2024-01-01",
      "genreNm": "액션",
      "nationNm": "한국",
      "watchGradeNm": "12세이상관람가",
      "posterUrl": "https://...",
      "status": "개봉"
    }
  ],
  "total": 100,
  "page": 0,
  "size": 20,
  "totalPages": 5
}
```

### 🎭 영화 상세정보 관련

#### 3. MovieDetail 데이터 조회
```javascript
// 영화 상세정보 (감독, 배우, 줄거리 포함)
GET /data/api/movie-detail?page=0&size=20

// 응답 예시
{
  "data": [...],           // 영화 상세정보 목록
  "total": 100,
  "page": 0,
  "size": 20,
  "totalPages": 5
}
```

#### 4. MovieDetail DTO 조회 (왓챠피디아 스타일)
```javascript
// 완전한 영화 상세정보 (포스터, 감독, 배우, 줄거리 등)
GET /data/api/movie-detail-dto?page=0&size=20

// 응답 예시
{
  "data": [
    {
      "movieCd": "20201234",
      "movieNm": "영화제목",
      "movieNmEn": "Movie Title",
      "description": "영화 줄거리...",
      "directorName": "감독명",
      "actors": [
        {
          "peopleNm": "배우명",
          "cast": "배역명"
        }
      ],
      "posterUrl": "https://...",
      "averageRating": 4.5
    }
  ],
  "total": 100,
  "page": 0,
  "size": 20,
  "totalPages": 5
}
```

### 📊 박스오피스 관련

#### 5. BoxOffice 데이터 조회
```javascript
// 박스오피스 데이터 (순위, 매출액, 관객수)
GET /data/api/box-office?page=0&size=20

// 응답 예시
{
  "data": [...],           // 박스오피스 목록
  "total": 20,
  "page": 0,
  "size": 20,
  "totalPages": 1
}
```

#### 6. BoxOffice DTO 조회 (왓챠피디아 스타일)
```javascript
// 완전한 박스오피스 정보 (포스터, 감독, 배우 등)
GET /data/api/box-office-dto?page=0&size=20

// 응답 예시
{
  "data": [
    {
      "movieCd": "20201234",
      "movieNm": "영화제목",
      "rank": 1,
      "salesAmt": 1000000000,
      "audiCnt": 100000,
      "audiAcc": 1000000,
      "posterUrl": "https://...",
      "directorName": "감독명",
      "actors": [...]
    }
  ],
  "total": 10,
  "page": 0,
  "size": 20,
  "totalPages": 1
}
```

### 📈 통계 및 특별 API

#### 7. 데이터 통계
```javascript
// 전체 데이터 개수 조회
GET /data/api/stats

// 응답 예시
{
  "movieListCount": 100,
  "movieDetailCount": 96,
  "boxOfficeCount": 20
}
```

#### 8. 평균 별점 높은 영화
```javascript
// 평균 별점이 높은 영화 TOP-N
GET /data/api/ratings/top-rated?limit=10

// 응답 예시
[
  {
    "movieCd": "20201234",
    "movieNm": "영화제목",
    // ... 기타 영화 정보
  }
]
```

#### 9. TMDB 인기 영화
```javascript
// TMDB 인기 영화 (KOBIS 정보와 결합)
GET /data/api/popular-movies?limit=100

// 응답 예시
{
  "success": true,
  "data": [...],          // 영화 목록
  "count": 100,           // 개수
  "message": "인기 영화 100개를 성공적으로 가져왔습니다."
}
```

## 🔧 React에서 사용 예시

### 기본 영화 목록 가져오기
```javascript
import React, { useState, useEffect } from 'react';

function MovieList() {
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/data/api/movie-list-dto?page=0&size=20')
      .then(res => res.json())
      .then(data => {
        setMovies(data.data);  // 실제 데이터는 data.data에 있음
        setLoading(false);
      })
      .catch(error => {
        console.error('영화 목록 가져오기 실패:', error);
        setLoading(false);
      });
  }, []);

  if (loading) return <div>로딩 중...</div>;

  return (
    <div className="movie-grid">
      {movies.map(movie => (
        <div key={movie.movieCd} className="movie-card">
          <img src={movie.posterUrl} alt={movie.movieNm} />
          <h3>{movie.movieNm}</h3>
          <p>{movie.genreNm}</p>
        </div>
      ))}
    </div>
  );
}
```

### 영화 상세정보 가져오기
```javascript
function MovieDetail({ movieCd }) {
  const [movie, setMovie] = useState(null);

  useEffect(() => {
    fetch(`/data/api/movie-detail-dto?page=0&size=100`)  // 충분히 큰 size로 모든 데이터 가져오기
      .then(res => res.json())
      .then(data => {
        const targetMovie = data.data.find(m => m.movieCd === movieCd);
        setMovie(targetMovie);
      });
  }, [movieCd]);

  if (!movie) return <div>로딩 중...</div>;

  return (
    <div className="movie-detail">
      <img src={movie.posterUrl} alt={movie.movieNm} />
      <h1>{movie.movieNm}</h1>
      <p><strong>감독:</strong> {movie.directorName}</p>
      <p><strong>줄거리:</strong> {movie.description}</p>
      <div className="actors">
        <h3>배우</h3>
        {movie.actors.map((actor, index) => (
          <div key={index}>
            {actor.peopleNm} - {actor.cast}
          </div>
        ))}
      </div>
    </div>
  );
}
```

### 페이지네이션 구현
```javascript
function MovieListWithPagination() {
  const [movies, setMovies] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  const fetchMovies = (page) => {
    setLoading(true);
    fetch(`/data/api/movie-list-dto?page=${page}&size=20`)
      .then(res => res.json())
      .then(data => {
        setMovies(data.data);
        setTotalPages(data.totalPages);
        setLoading(false);
      })
      .catch(error => {
        console.error('영화 목록 가져오기 실패:', error);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchMovies(currentPage);
  }, [currentPage]);

  return (
    <div>
      <div className="movie-grid">
        {movies.map(movie => (
          <div key={movie.movieCd} className="movie-card">
            <img src={movie.posterUrl} alt={movie.movieNm} />
            <h3>{movie.movieNm}</h3>
          </div>
        ))}
      </div>
      
      <div className="pagination">
        <button 
          onClick={() => setCurrentPage(currentPage - 1)}
          disabled={currentPage === 0}
        >
          이전
        </button>
        <span>페이지 {currentPage + 1} / {totalPages}</span>
        <button 
          onClick={() => setCurrentPage(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
        >
          다음
        </button>
      </div>
    </div>
  );
}
```

## 📝 참고사항

1. **페이지네이션**: 모든 목록 API는 `page`와 `size` 파라미터 지원
2. **응답 구조**: 모든 API는 `{ data: [...], total: 100, page: 0, size: 20, totalPages: 5 }` 형태로 응답
3. **DTO vs 기본**: DTO 버전이 더 완전한 정보 제공 (포스터, 감독, 배우 등)
4. **에러 처리**: 모든 API는 에러 시 `{ "error": "에러메시지" }` 형태로 응답
5. **CORS**: 개발 환경에서는 CORS 설정 필요할 수 있음

## 🔗 추가 리소스
- **Swagger UI**: `http://localhost/swagger-ui.html`
- **API 상태 확인**: `http://localhost/data/api/stats` 