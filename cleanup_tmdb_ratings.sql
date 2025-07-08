-- TMDB 사용자로 저장된 평점들 삭제
-- 사용자들이 직접 주는 평점만 남기고 TMDB 평점은 제거

-- ===== 1단계: 현재 상태 확인 =====

-- 1-1. TMDB 사용자 찾기
SELECT id, login_id, email, created_at FROM users WHERE login_id = 'tmdb_rating_user';

-- 1-2. 전체 평점 개수 확인
SELECT COUNT(*) as total_rating_count FROM rating;

-- 1-3. TMDB 사용자가 남긴 평점 개수 확인
SELECT COUNT(*) as tmdb_rating_count 
FROM rating r 
JOIN users u ON r.user_id = u.id 
WHERE u.login_id = 'tmdb_rating_user';

-- 1-4. 실제 사용자가 남긴 평점 개수 확인
SELECT COUNT(*) as user_rating_count 
FROM rating r 
JOIN users u ON r.user_id = u.id 
WHERE u.login_id != 'tmdb_rating_user';

-- 1-5. TMDB 사용자가 남긴 평점들 상세 확인 (최대 10개만)
SELECT r.id, r.score, r.created_at, m.movie_nm, u.login_id 
FROM rating r 
JOIN movie_detail m ON r.movie_detail_id = m.movie_detail_id 
JOIN users u ON r.user_id = u.id 
WHERE u.login_id = 'tmdb_rating_user'
LIMIT 10;

-- ===== 2단계: TMDB 평점 삭제 =====

-- 2-1. TMDB 사용자가 남긴 평점들 삭제
DELETE r FROM rating r 
JOIN users u ON r.user_id = u.id 
WHERE u.login_id = 'tmdb_rating_user';

-- 2-2. 삭제 후 평점 개수 확인
SELECT COUNT(*) as remaining_rating_count FROM rating;

-- ===== 3단계: TMDB 사용자 계정 정리 =====

-- 3-1. TMDB 사용자 계정 삭제 (선택사항 - 주석 해제하여 실행)
-- DELETE FROM users WHERE login_id = 'tmdb_rating_user';

-- ===== 4단계: 정리 후 상태 확인 =====

-- 4-1. 영화별 평점 개수 확인 (사용자 평점만)
SELECT m.movie_nm, COUNT(r.id) as user_rating_count 
FROM movie_detail m 
LEFT JOIN rating r ON m.movie_detail_id = r.movie_detail_id 
GROUP BY m.movie_detail_id, m.movie_nm 
HAVING user_rating_count > 0
ORDER BY user_rating_count DESC
LIMIT 20;

-- 4-2. 평점이 있는 영화 개수 확인
SELECT COUNT(DISTINCT m.movie_detail_id) as movies_with_ratings
FROM movie_detail m 
JOIN rating r ON m.movie_detail_id = r.movie_detail_id;

-- 4-3. 평점이 없는 영화 개수 확인
SELECT COUNT(DISTINCT m.movie_detail_id) as movies_without_ratings
FROM movie_detail m 
LEFT JOIN rating r ON m.movie_detail_id = r.movie_detail_id 
WHERE r.id IS NULL; 