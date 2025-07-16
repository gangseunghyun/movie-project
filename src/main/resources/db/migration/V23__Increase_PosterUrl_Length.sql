-- movie_list 테이블의 poster_url 컬럼 길이를 늘림
ALTER TABLE movie_list MODIFY COLUMN poster_url VARCHAR(1000); 