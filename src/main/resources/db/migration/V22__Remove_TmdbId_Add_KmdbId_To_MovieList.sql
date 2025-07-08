-- movie_list 테이블에서 tmdb_id 컬럼 삭제, kmdb_id 컬럼 추가
ALTER TABLE movie_list DROP COLUMN tmdb_id;
ALTER TABLE movie_list ADD COLUMN kmdb_id VARCHAR(32) NULL; 