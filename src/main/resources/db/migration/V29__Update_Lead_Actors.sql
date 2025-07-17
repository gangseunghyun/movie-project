-- 기존 영화들의 상위 3명 배우를 주연(LEAD)으로 업데이트
-- order_in_credits가 1, 2, 3인 배우들을 주연으로 변경

UPDATE casts 
SET role_type = 'LEAD' 
WHERE order_in_credits IN (1, 2, 3) 
AND role_type = 'SUPPORTING'; 