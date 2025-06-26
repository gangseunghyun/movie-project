-- Add created_at column to users table (NULL 허용으로 먼저 추가)
ALTER TABLE users 
ADD COLUMN created_at DATETIME NULL;

-- Update existing records to have proper created_at timestamp
UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

-- Now make it NOT NULL
ALTER TABLE users 
MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP; 