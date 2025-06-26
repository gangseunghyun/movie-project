-- BoxOffice 테이블에 movie_detail_id 컬럼 추가
ALTER TABLE box_office 
ADD COLUMN movie_detail_id VARCHAR(20);

-- 외래키 제약조건 추가
ALTER TABLE box_office 
ADD CONSTRAINT fk_box_office_movie_detail 
FOREIGN KEY (movie_detail_id) REFERENCES movie_detail(movie_cd);

-- 인덱스 추가
CREATE INDEX idx_box_office_movie_detail_id ON box_office(movie_detail_id); 