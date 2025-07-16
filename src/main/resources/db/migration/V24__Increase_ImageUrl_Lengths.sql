-- stillcut 테이블의 image_url 컬럼 길이를 늘림
ALTER TABLE stillcut MODIFY COLUMN image_url VARCHAR(1000);

-- director 테이블의 photo_url 컬럼 길이를 늘림
ALTER TABLE director MODIFY COLUMN photo_url VARCHAR(1000);

-- actor 테이블의 photo_url 컬럼 길이를 늘림
ALTER TABLE actor MODIFY COLUMN photo_url VARCHAR(1000); 