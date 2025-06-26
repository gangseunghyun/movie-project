-- 기존 movie_actor 테이블의 데이터를 새로운 casts 테이블로 마이그레이션
-- 기본적으로 모든 배우를 'SUPPORTING' (조연)으로 설정하고, 나중에 수동으로 주연 배우를 업데이트

INSERT INTO casts (movie_detail_id, actor_id, role_type, character_name, order_in_credits)
SELECT 
    movie_id,
    actor_id,
    'SUPPORTING' as role_type,  -- 기본값을 조연으로 설정
    NULL as character_name,     -- 캐릭터 이름은 나중에 수동 입력
    ROW_NUMBER() OVER (PARTITION BY movie_id ORDER BY actor_id) as order_in_credits
FROM movie_actor;

-- 마이그레이션 완료 후 기존 테이블 삭제 (선택사항)
-- DROP TABLE movie_actor; 