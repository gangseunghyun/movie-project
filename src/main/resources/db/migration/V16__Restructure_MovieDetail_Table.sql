-- MovieDetail 테이블 구조 변경
-- 1. 기존 movie_cd를 외래키로 변경하기 위한 임시 컬럼 추가
ALTER TABLE movie_detail 
ADD COLUMN temp_movie_cd VARCHAR(20);

-- 2. 기존 movie_cd 데이터를 임시 컬럼으로 복사
UPDATE movie_detail 
SET temp_movie_cd = movie_cd;

-- 3. 기존 movie_cd 컬럼 삭제 (기본키이므로 먼저 제약조건 삭제 필요)
ALTER TABLE movie_detail DROP PRIMARY KEY;

-- 4. 새로운 기본키 컬럼 추가
ALTER TABLE movie_detail 
ADD COLUMN movie_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- 5. movie_cd 컬럼을 외래키로 다시 추가
ALTER TABLE movie_detail 
ADD COLUMN movie_cd VARCHAR(20),
ADD CONSTRAINT fk_movie_detail_movie_list 
FOREIGN KEY (movie_cd) REFERENCES movie_list(movie_cd) ON DELETE CASCADE;

-- 6. 임시 컬럼의 데이터를 새로운 movie_cd 컬럼으로 복사
UPDATE movie_detail 
SET movie_cd = temp_movie_cd 
WHERE temp_movie_cd IS NOT NULL;

-- 7. 임시 컬럼 삭제
ALTER TABLE movie_detail DROP COLUMN temp_movie_cd;

-- 8. 인덱스 생성
CREATE INDEX idx_movie_detail_movie_cd ON movie_detail(movie_cd); 