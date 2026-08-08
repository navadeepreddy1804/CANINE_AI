-- Flyway Migration V3: Add username and make role_title, hospital, department nullable safely

DROP PROCEDURE IF EXISTS add_username_column;

DELIMITER //

CREATE PROCEDURE add_username_column()
BEGIN
    DECLARE col_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO col_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'username';

    IF col_exists = 0 THEN
        ALTER TABLE users ADD COLUMN username VARCHAR(255) NULL UNIQUE;
    END IF;

    -- Make columns nullable
    ALTER TABLE users MODIFY COLUMN role_title VARCHAR(255) NULL;
    ALTER TABLE users MODIFY COLUMN hospital VARCHAR(255) NULL;
    ALTER TABLE users MODIFY COLUMN department VARCHAR(255) NULL;
END //

DELIMITER ;

CALL add_username_column();

DROP PROCEDURE add_username_column;
