-- 인기 검색어 집계 테이블 생성
CREATE TABLE IF NOT EXISTS popular_keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    search_count INT NOT NULL,
    aggregated_at DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_popularkeyword_aggregatedat ON popular_keyword (aggregated_at); 