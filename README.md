# 🎬 movie-project

사용자 취향을 분석해 **개인 맞춤형 영화**를 추천하고,  
리뷰·댓글·별점 등 소셜 기능을 제공하는 풀스택 영화 서비스입니다.  
백엔드는 **Spring Boot**, 프론트엔드는 **React**로 구현합니다.

---

## 📚 프로젝트 개요

- **프로젝트 기간:** 2025년 6월 ~ 7월 (3주간)
- **기획 목표:** 도메인 기반 CRUD 설계 + 추천 알고리즘 학습 및 협업 경험
- **주요 기능:** 
  - 사용자 맞춤 영화 추천
  - 소셜 기능(댓글, 별점, 좋아요)
  - OAuth 로그인, 배치, 클린봇 등 실무 기능

---

## ⚙️ 기술 스택

| 분야       | 기술                                                         |
|------------|--------------------------------------------------------------|
| Backend    | Java 21, Spring Boot, Spring Data JPA, Spring Security       |
| DB         | MySQL                                                        |
| Frontend   | React 18,                        |
| Auth       | Spring Security + OAuth2 (카카오/네이버)                     |
| DevOps     | Maven, GitHub, GitHub Actions(예정)                          |
| 기타       | Swagger/OpenAPI, Lombok, Batch, Scheduler                    |

---

## 👥 팀원 역할 분담

### 🟦 규현: CRUD 중심 / 데이터 생성자

**목표: 도메인 설계 & 데이터를 만드는 모든 기능**

- 도메인 모델링 & 엔티티 설계  
  (Movie, Actor, Director, User, Comment, Rating, Like, Tag 등)
- 영화 등록/수정/삭제, 상세 조회
- 영화 좋아요 / 태그·배우·감독 연관 설정
- 코멘트/댓글/대댓글 CRUD + 좋아요
- 마이페이지 정보 제공 (내가 단 댓글, 별점, 좋아요)
- 다크모드 설정 저장 (선택)
- 더미 데이터 생성(data.sql 또는 API)
- 결제 올인원 서비스 (Stub)

---

### 🟨 승현: 검색/추천/인증 중심 / 데이터 소비자

**목표: 데이터를 가공하고 검색, 추천, 통계를 담당**

- 로그인/회원가입 (Spring Security + OAuth2)
- 통합 검색 (영화 / 배우 / 감독 / 유저)
- 추천 검색어 / 최근 검색어 저장
- 영화 정렬 (별점순, 개봉일순 등)
- 별점 기능: 기록 저장, 변경, 평균·분포 시각화
- 추천 시스템
  - 장르 기반 / 태그 기반 / 배우·감독 기반
  - 평균 별점 기반 / 사용자 기반 Top10 추천
- 클린봇(댓글 욕설 필터링)
- 외부 API 연동 (박스오피스 배치 - KOBIS)


---

## 🚀 실행 방법

### ✅ MySQL DB 설정

```sql
CREATE DATABASE movie_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;



위 명령어로 MySQL에 movie_db 데이터베이스를 생성합니다.

로컬에서 application.properties 파일을 생성하고 다음과 같은 형식으로 작성하세요


spring.datasource.url=jdbc:mysql://localhost:3306/movie
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

