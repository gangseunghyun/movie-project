-- MovieList 테이블에 TMDB 인기도 필드 추가
ALTER TABLE movie_list ADD COLUMN tmdb_popularity DOUBLE NULL COMMENT 'TMDB 인기도 점수'; 