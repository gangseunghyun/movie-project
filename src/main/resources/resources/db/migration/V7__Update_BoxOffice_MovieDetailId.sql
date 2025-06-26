-- 기존 BoxOffice 데이터의 movie_detail_id 업데이트
UPDATE box_office bo 
SET movie_detail_id = bo.movie_cd 
WHERE bo.movie_detail_id IS NULL 
AND EXISTS (SELECT 1 FROM movie_detail md WHERE md.movie_cd = bo.movie_cd); 