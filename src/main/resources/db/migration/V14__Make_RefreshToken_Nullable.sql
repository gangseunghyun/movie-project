-- OAuth2 토큰 테이블의 refresh_token 컬럼을 nullable로 변경
ALTER TABLE oauth2_tokens MODIFY COLUMN refresh_token VARCHAR(255) NULL; 