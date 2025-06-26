-- 기존 casts 테이블의 잘못된 컬럼명 데이터를 수정
-- movie_id 컬럼을 movie_detail_id로 변경

-- 먼저 기존 잘못된 데이터 삭제
DELETE FROM casts WHERE movie_detail_id IS NULL;

-- 기존 movie_actor 테이블의 데이터를 올바른 컬럼명으로 다시 마이그레이션
INSERT INTO casts (movie_detail_id, actor_id, role_type, character_name, order_in_credits)
SELECT 
    movie_id,
    actor_id,
    'SUPPORTING' as role_type,  -- 기본값을 조연으로 설정
    NULL as character_name,     -- 캐릭터 이름은 나중에 수동 입력
    ROW_NUMBER() OVER (PARTITION BY movie_id ORDER BY actor_id) as order_in_credits
FROM movie_actor
WHERE NOT EXISTS (
    SELECT 1 FROM casts c 
    WHERE c.movie_detail_id = movie_actor.movie_id 
    AND c.actor_id = movie_actor.actor_id
); 