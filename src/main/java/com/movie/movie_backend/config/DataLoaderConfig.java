//package com.movie.movie_backend.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//
//@Slf4j
//@Configuration
//public class DataLoaderConfig {
//    // 모든 로직이 별도 클래스로 분리되었습니다.
//    // - BoxOfficeLoader: 박스오피스 데이터 로드 (Order 1)
//    // - MovieListLoader: 영화 목록 로딩 (Order 2)
//    // - MovieDetailLoader: 영화 상세 정보 로딩 (Order 3)
//    // - KmdbMapper: KMDb ID 매핑 (Order 3)
//    // - DescriptionLoader: 줄거리 채우기 (Order 4)
//    // - StatusUpdater: 영화 상태 업데이트 (Order 5)
//    // - StillcutLoader: 스틸컷 업데이트 (Order 6)
//    // - TagMapper: 태그 매핑 (Order 7)
//    // - CleanupLoader: Orphan MovieList 정리 (Order 없음, 최우선 실행)
//}