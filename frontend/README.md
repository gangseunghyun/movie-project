# Movie DB 데이터 조회 React 앱

Spring Boot 백엔드 API와 연동하여 영화 데이터를 조회하는 React 애플리케이션입니다.

## 기능

- 📊 **통계**: MovieList, MovieDetail, BoxOffice 개수 조회
- 📋 **MovieList**: 영화 목록 데이터 조회 (페이지네이션)
- 🎭 **MovieDetail**: 영화 상세 정보 조회 (페이지네이션)
- 🏆 **BoxOffice**: 박스오피스 데이터 조회 (페이지네이션)
- 🎯 **BoxOffice DTO**: 왓챠피디아 스타일 박스오피스 조회

## 설치 및 실행

### 1. 의존성 설치
```bash
npm install
```

### 2. 개발 서버 실행
```bash
npm start
```

React 앱이 `http://localhost:3000`에서 실행됩니다.

### 3. 빌드 (배포용)
```bash
npm run build
```

## 백엔드 API

이 React 앱은 Spring Boot 백엔드 API와 연동됩니다:

- **API 서버**: `http://localhost:80`
- **프록시 설정**: `package.json`에서 `"proxy": "http://localhost:80"`로 설정

### API 엔드포인트

- `GET /data/api/stats` - 데이터 통계
- `GET /data/api/movie-list` - MovieList 데이터
- `GET /data/api/movie-detail` - MovieDetail 데이터
- `GET /data/api/box-office` - BoxOffice 데이터
- `GET /data/api/box-office-dto` - BoxOffice DTO 데이터

## 사용법

1. Spring Boot 백엔드 서버를 먼저 실행합니다
2. React 앱을 실행합니다: `npm start`
3. 브라우저에서 `http://localhost:3000` 접속
4. 각 탭을 클릭하여 다른 데이터 확인
5. BoxOffice 탭에서 `movie_detail_id` 연결 상태 확인 가능

## 기술 스택

- **Frontend**: React 18, Axios
- **Backend**: Spring Boot (포트 80)
- **Styling**: CSS3 (Grid, Flexbox)
- **Build Tool**: Create React App 