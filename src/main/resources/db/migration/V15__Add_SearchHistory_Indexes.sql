-- 최근 검색어/인기 검색어 집계 성능 향상 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_searchhistory_searchedat ON search_history (searched_at);
CREATE INDEX IF NOT EXISTS idx_searchhistory_keyword ON search_history (keyword);
CREATE INDEX IF NOT EXISTS idx_searchhistory_searchedat_keyword ON search_history (searched_at, keyword); 