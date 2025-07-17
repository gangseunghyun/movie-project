-- MovieList 테이블에 TMDB ID 컬럼 추가
ALTER TABLE movie_list ADD COLUMN tmdb_id INT NULL COMMENT 'TMDB 영화 ID'; 