-- 기존 stillcut 테이블의 null인 movie_detail_id 수정
-- MovieDetail과 연결된 스틸컷들의 movie_detail_id를 올바르게 설정

-- 먼저 기존 null인 movie_detail_id를 가진 스틸컷들을 삭제
DELETE FROM stillcut WHERE movie_detail_id IS NULL;

-- 또는 MovieDetail과 연결된 스틸컷만 남기고 나머지 삭제
-- DELETE s FROM stillcut s 
-- LEFT JOIN movie_detail md ON s.movie_detail_id = md.movie_cd 
-- WHERE md.movie_cd IS NULL; 