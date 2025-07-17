-- MovieList 테이블에 TMDB 관련 컬럼들 추가 (통합)
ALTER TABLE movie_list ADD COLUMN tmdb_id INT NULL COMMENT 'TMDB 영화 ID' IF NOT EXISTS;
ALTER TABLE movie_list ADD COLUMN tmdb_popularity DOUBLE NULL COMMENT 'TMDB 인기도 점수' IF NOT EXISTS; 