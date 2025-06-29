-- Stillcut 테이블 생성
CREATE TABLE IF NOT EXISTS stillcut (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_detail_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    order_in_movie INT DEFAULT 0,
    FOREIGN KEY (movie_detail_id) REFERENCES movie_detail(movie_detail_id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_stillcut_movie_detail_id ON stillcut(movie_detail_id);
CREATE INDEX idx_stillcut_order ON stillcut(order_in_movie); 