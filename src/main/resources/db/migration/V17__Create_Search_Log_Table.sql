-- 검색 로그 테이블 생성
CREATE TABLE IF NOT EXISTS search_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    keyword VARCHAR(100) NOT NULL,
    searched_at DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_searchlog_searchedat ON search_log (searched_at);
CREATE INDEX IF NOT EXISTS idx_searchlog_keyword ON search_log (keyword); 