CREATE TABLE recommendation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    score INT NOT NULL,
    reasons VARCHAR(255),
    recommended_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
); 