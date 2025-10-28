DROP TABLE IF EXISTS `match_history`;
DROP TABLE IF EXISTS `friends`;
DROP TABLE IF EXISTS `users`;

-- Cài đặt bộ ký tự và collation để hỗ trợ tiếng Việt
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET GLOBAL time_zone = '+07:00';
SET time_zone = '+07:00';

-- 1. TẠO DATABASE
CREATE DATABASE IF NOT EXISTS pick_up_trash
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE pick_up_trash;

-- 2. TẠO BẢNG 'users'
CREATE TABLE users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    high_score INT DEFAULT 0,
    is_online TINYINT(1) NOT NULL DEFAULT 0, -- 0 = offline, 1 = online
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. TẠO BẢNG 'match_history'
CREATE TABLE match_history (
   id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
   user_id INT UNSIGNED NOT NULL,
   opponent_id INT UNSIGNED NOT NULL,
   score INT NOT NULL DEFAULT 0,
   result ENUM('win', 'lose', 'draw') DEFAULT 'draw',
   start_date DATETIME DEFAULT CURRENT_TIMESTAMP,
   end_date DATETIME DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
   FOREIGN KEY (opponent_id) REFERENCES users(id) ON DELETE CASCADE,
   INDEX idx_user_date (user_id, start_date)
) ENGINE=InnoDB;


-- 4. TẠO BẢNG 'friends'
CREATE TABLE `friends` (
    user_id INT UNSIGNED NOT NULL,
    friend_id INT UNSIGNED NOT NULL,
    status ENUM('pending','accepted','blocked') NOT NULL DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES `users`(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES `users`(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. THÊM DỮ LIỆU MẪU CHO users
INSERT INTO `users` (`username`, `password`, `email`, `high_score`) VALUES
('player1', '123', 'player1@example.com', 150),
('player2', '123', 'pro@example.com', 250),
('noobMaster', '123', 'noob@example.com', 75);

-- 7. THÊM DỮ LIỆU MẪU CHO friends
INSERT INTO `friends` (`user_id`, `friend_id`, `status`) VALUES
(1, 2, 'accepted'),
(1, 3, 'pending'),
(2, 3, 'accepted');

-- Kết thúc script
COMMIT;

SELECT 'Cơ sở dữ liệu và các bảng đã được tạo thành công với dữ liệu mẫu, bao gồm match_history và friends, kiểu dữ liệu chuẩn.' AS 'Status';
