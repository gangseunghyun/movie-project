# 관리자 API 가이드 (왓챠피디아 스타일)

## 개요
관리자가 영화를 등록, 수정, 비활성화하고 태그를 설정할 수 있는 REST API입니다.
왓챠피디아와 유사한 박스오피스 및 공개 예정작 기능을 제공합니다.

## 기본 URL
```
http://localhost:8080/api/admin
```

## 1. 영화 관리

### 1.1 영화 등록
**POST** `/api/admin/movies`

```json
{
  "movieCd": "20240001",
  "movieNm": "새로운 영화",
  "movieNmEn": "New Movie",
  "prdtYear": "2024",
  "showTm": 120,
  "openDt": "2024-12-25",
  "prdtStatNm": "개봉",
  "typeNm": "장편",
  "genreNm": "액션,드라마",
  "nationNm": "대한민국",
  "watchGradeNm": "15세이상관람가",
  "companyNm": "영화사",
  "description": "영화 설명입니다.",
  "status": "COMING_SOON",
  "tagNames": ["액션", "드라마", "2024년작"]
}
```

### 1.2 영화 정보 수정
**PUT** `/api/admin/movies/{movieCd}`

```json
{
  "movieNm": "수정된 영화 제목",
  "description": "수정된 영화 설명",
  "status": "NOW_PLAYING",
  "tagNames": ["액션", "스릴러", "인기작"]
}
```

### 1.3 영화 비활성화
**PUT** `/api/admin/movies/{movieCd}/deactivate`

### 1.4 영화 활성화
**PUT** `/api/admin/movies/{movieCd}/activate`

### 1.5 모든 영화 조회
**GET** `/api/admin/movies`

### 1.6 상태별 영화 조회
**GET** `/api/admin/movies/status/{status}`

- `COMING_SOON`: 개봉예정
- `NOW_PLAYING`: 개봉중
- `ENDED`: 상영종료

### 1.7 영화명으로 검색
**GET** `/api/admin/movies/search?movieNm=영화명`

## 2. 박스오피스 관리 (왓챠피디아 스타일)

### 2.1 일일 박스오피스 TOP-10 조회
**GET** `/api/admin/boxoffice/daily`

**응답 예시:**
```json
[
  {
    "id": 1,
    "movieCd": "20240001",
    "movieNm": "F1 더 무비",
    "rank": 1,
    "salesAmt": 1200000000,
    "audiCnt": 1828,
    "audiAcc": 1828,
    "targetDate": "2025-06-24",
    "rankType": "DAILY",
    "movieNmEn": "F1 The Movie",
    "genreNm": "액션,드라마",
    "nationNm": "미국",
    "watchGradeNm": "12세이상관람가",
    "posterUrl": "",
    "description": "F1 레이싱을 배경으로 한 액션 영화",
    "showTm": 120,
    "openDt": "2025-06-25",
    "reservationRate": "18.3%",
    "audienceCount": "1,828명",
    "salesAmount": "12억원",
    "rankChange": "-",
    "rankTypeDisplay": "일일",
    "movieStatus": "상영예정",
    "daysSinceRelease": -1,
    "directorName": "존 스미스",
    "directorPhotoUrl": "https://image.tmdb.org/t/p/w500/...",
    "tags": ["액션", "드라마", "F1", "2025년작"]
  }
]
```

### 2.2 주간 박스오피스 TOP-10 조회
**GET** `/api/admin/boxoffice/weekly`

### 2.3 특정 날짜의 박스오피스 조회
**GET** `/api/admin/boxoffice/date/{date}?rankType=DAILY`

- `date`: YYYY-MM-DD 형식 (예: 2025-06-24)
- `rankType`: DAILY, WEEKLY, WEEKEND (기본값: DAILY)

### 2.4 박스오피스 데이터 수동 업데이트 (일일)
**POST** `/api/admin/boxoffice/daily/update`

### 2.5 박스오피스 데이터 수동 업데이트 (주간)
**POST** `/api/admin/boxoffice/weekly/update`

## 3. 공개 예정작 관리 (왓챠피디아 스타일)

### 3.1 공개 예정작 조회 (D-1, D-2, D-3 등)
**GET** `/api/admin/movies/coming-soon`

**응답 예시:**
```json
[
  {
    "movieCd": "20240001",
    "movieNm": "그을린 사랑",
    "movieNmEn": "Burned Love",
    "prdtYear": "2025",
    "showTm": 110,
    "openDt": "2025-06-25",
    "prdtStatNm": "개봉",
    "typeNm": "장편",
    "genreNm": "로맨스,드라마",
    "nationNm": "대한민국",
    "watchGradeNm": "15세이상관람가",
    "companyNm": "영화사",
    "description": "그을린 사랑에 대한 이야기",
    "status": "COMING_SOON",
    "tagNames": ["로맨스", "드라마", "2025년작"],
    "createdAt": null,
    "updatedAt": null
  }
]
```

### 3.2 상영중인 영화 조회
**GET** `/api/admin/movies/now-playing`

### 3.3 상영 종료된 영화 조회
**GET** `/api/admin/movies/ended`

## 4. 태그 관리

### 4.1 영화 태그 설정 (전체 교체)
**PUT** `/api/admin/movies/{movieCd}/tags`

```json
["액션", "드라마", "2024년작", "인기작"]
```

### 4.2 영화 태그 추가
**POST** `/api/admin/movies/{movieCd}/tags`

```json
{
  "tagName": "새로운태그"
}
```

### 4.3 영화 태그 제거
**DELETE** `/api/admin/movies/{movieCd}/tags/{tagName}`

### 4.4 모든 태그 조회
**GET** `/api/admin/tags`

### 4.5 태그명으로 검색
**GET** `/api/admin/tags/search?name=태그명`

## 5. 응답 예시

### 성공 응답
```json
{
  "movieCd": "20240001",
  "movieNm": "새로운 영화",
  "movieNmEn": "New Movie",
  "prdtYear": "2024",
  "showTm": 120,
  "openDt": "2024-12-25",
  "prdtStatNm": "개봉",
  "typeNm": "장편",
  "genreNm": "액션,드라마",
  "nationNm": "대한민국",
  "watchGradeNm": "15세이상관람가",
  "companyNm": "영화사",
  "description": "영화 설명입니다.",
  "status": "COMING_SOON",
  "tagNames": ["액션", "드라마", "2024년작"],
  "createdAt": null,
  "updatedAt": null
}
```

### 에러 응답
```json
{
  "timestamp": "2024-01-01T12:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "이미 존재하는 영화 코드입니다: 20240001"
}
```

## 6. 사용 예시

### cURL 예시

#### 박스오피스 조회
```bash
# 일일 박스오피스 조회
curl -X GET http://localhost:8080/api/admin/boxoffice/daily

# 주간 박스오피스 조회
curl -X GET http://localhost:8080/api/admin/boxoffice/weekly

# 특정 날짜 박스오피스 조회
curl -X GET "http://localhost:8080/api/admin/boxoffice/date/2025-06-24?rankType=DAILY"
```

#### 공개 예정작 조회
```bash
# 공개 예정작 조회
curl -X GET http://localhost:8080/api/admin/movies/coming-soon

# 상영중인 영화 조회
curl -X GET http://localhost:8080/api/admin/movies/now-playing
```

#### 박스오피스 업데이트
```bash
# 일일 박스오피스 업데이트
curl -X POST http://localhost:8080/api/admin/boxoffice/daily/update

# 주간 박스오피스 업데이트
curl -X POST http://localhost:8080/api/admin/boxoffice/weekly/update
```

#### 영화 등록
```bash
curl -X POST http://localhost:8080/api/admin/movies \
  -H "Content-Type: application/json" \
  -d '{
    "movieCd": "20240001",
    "movieNm": "새로운 영화",
    "movieNmEn": "New Movie",
    "prdtYear": "2024",
    "showTm": 120,
    "openDt": "2024-12-25",
    "genreNm": "액션,드라마",
    "description": "영화 설명입니다.",
    "tagNames": ["액션", "드라마", "2024년작"]
  }'
```

#### 영화 태그 설정
```bash
curl -X PUT http://localhost:8080/api/admin/movies/20240001/tags \
  -H "Content-Type: application/json" \
  -d '["액션", "스릴러", "인기작"]'
```

#### 영화 비활성화
```bash
curl -X PUT http://localhost:8080/api/admin/movies/20240001/deactivate
```

## 7. 주의사항

1. **영화 코드 중복**: 영화 등록 시 `movieCd`는 고유해야 합니다.
2. **태그 자동 생성**: 존재하지 않는 태그명을 입력하면 자동으로 새 태그가 생성됩니다.
3. **상태 관리**: 영화 상태는 `COMING_SOON`, `NOW_PLAYING`, `ENDED` 중 하나여야 합니다.
4. **날짜 형식**: `openDt`는 `YYYY-MM-DD` 형식으로 입력해야 합니다.
5. **박스오피스 데이터**: 박스오피스 데이터는 KOBIS API에서 자동으로 가져오며, 수동 업데이트도 가능합니다.
6. **포스터 URL**: 현재 MovieDetail 엔티티에는 posterUrl 필드가 없으므로 빈 문자열로 반환됩니다.

## 8. 에러 코드

- `400 Bad Request`: 잘못된 요청 데이터
- `404 Not Found`: 존재하지 않는 영화
- `500 Internal Server Error`: 서버 내부 오류

## 9. 왓챠피디아 스타일 특징

1. **박스오피스 포맷팅**: 관객수, 매출액, 예매율 등을 왓챠피디아와 유사한 형태로 포맷팅
2. **영화 상태 표시**: 상영예정, 상영중, 상영종료 상태를 명확히 구분
3. **감독 정보**: 감독 이름과 사진 URL 포함
4. **태그 시스템**: 영화별 태그 정보 제공
5. **공개 예정작**: D-1, D-2, D-3 등 개봉일 기준으로 정렬된 공개 예정작 목록 