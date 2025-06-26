-- Comment 테이블에 평점 컬럼 추가 (왓챠피디아 스타일)
ALTER TABLE comment ADD COLUMN rating INTEGER;

-- 평점 유효성 체크 (1~5점)
ALTER TABLE comment ADD CONSTRAINT chk_rating_range CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5));

-- 기존 Rating 테이블의 데이터를 Comment 테이블로 마이그레이션 (선택사항)
-- 이 부분은 나중에 필요에 따라 실행할 수 있습니다.
-- INSERT INTO comment (content, created_at, updated_at, status, user_id, movie_detail_id, rating)
-- SELECT 
--     CONCAT('평점: ', r.score, '점') as content,
--     r.created_at,
--     r.created_at as updated_at,
--     'ACTIVE' as status,
--     r.user_id,
--     r.movie_detail_id,
--     r.score as rating
-- FROM rating r
-- WHERE r.score >= 1 AND r.score <= 5; 