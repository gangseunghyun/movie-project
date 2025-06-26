-- 기존 comment 테이블을 comment_old로 이름 변경
RENAME TABLE comment TO comment_old;

-- 새로 생성된 comments 테이블을 comment로 이름 변경
RENAME TABLE comments TO comment; 