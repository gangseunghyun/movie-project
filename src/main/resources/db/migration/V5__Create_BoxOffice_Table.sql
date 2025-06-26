-- BoxOffice 테이블 생성
CREATE TABLE IF NOT EXISTS box_office (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_cd VARCHAR(20) NOT NULL,
    movie_nm VARCHAR(200) NOT NULL,
    `rank` INT NOT NULL,
    sales_amt BIGINT NOT NULL,
    audi_cnt BIGINT NOT NULL,
    audi_acc BIGINT NOT NULL,
    target_date DATE NOT NULL,
    rank_type VARCHAR(20) NOT NULL,
    movie_detail_id VARCHAR(20),
    FOREIGN KEY (movie_detail_id) REFERENCES movie_detail(movie_cd)
);

-- 인덱스 생성
CREATE INDEX idx_box_office_target_date ON box_office(target_date);
CREATE INDEX idx_box_office_rank_type ON box_office(rank_type);
CREATE INDEX idx_box_office_movie_cd ON box_office(movie_cd); 