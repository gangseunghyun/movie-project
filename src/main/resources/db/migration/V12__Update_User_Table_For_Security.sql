-- User 테이블을 Spring Security와 소셜 로그인을 지원하도록 업데이트

-- 1. 기존 username 컬럼을 loginId로 변경
ALTER TABLE users CHANGE COLUMN username loginId VARCHAR(255) NOT NULL;
ALTER TABLE users ADD UNIQUE KEY uk_users_login_id (loginId);

-- 2. email 컬럼에 unique 제약조건 추가
ALTER TABLE users ADD UNIQUE KEY uk_users_email (email);

-- 3. 새로운 컬럼들 추가
ALTER TABLE users ADD COLUMN nickname VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN social_join_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 4. provider 컬럼이 없다면 추가
ALTER TABLE users ADD COLUMN provider VARCHAR(20) DEFAULT 'LOCAL';

-- 5. provider_id 컬럼이 없다면 추가
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- 6. role 컬럼이 없다면 추가
ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER';

-- 7. 기존 데이터에 대한 기본값 설정
UPDATE users SET 
    email_verified = TRUE,
    social_join_completed = TRUE,
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE email_verified IS NULL;

-- 8. 인덱스 추가 (성능 최적화)
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at); 