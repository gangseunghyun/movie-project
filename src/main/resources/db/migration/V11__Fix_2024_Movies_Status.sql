-- 2024년 영화들의 상태를 개봉예정작에서 상영중으로 변경
-- 현재 날짜가 2024년이므로 2024년에 개봉한 영화들은 이미 개봉한 상태

UPDATE movie_list 
SET status = 'NOW_PLAYING' 
WHERE status = 'COMING_SOON' 
  AND open_dt IS NOT NULL 
  AND YEAR(open_dt) = 2024 
  AND open_dt <= CURDATE();

-- 로그 출력을 위한 임시 테이블 생성 (선택사항)
CREATE TEMPORARY TABLE IF NOT EXISTS temp_updated_movies AS
SELECT movie_cd, movie_nm, open_dt, status
FROM movie_list 
WHERE status = 'NOW_PLAYING' 
  AND open_dt IS NOT NULL 
  AND YEAR(open_dt) = 2024 
  AND open_dt <= CURDATE(); 