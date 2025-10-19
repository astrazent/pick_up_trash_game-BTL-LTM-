-- Cài đặt bộ ký tự và collation để hỗ trợ tiếng Việt
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. TẠO DATABASE
-- Tạo database 'pick_up_trash' nếu nó chưa tồn tại.
CREATE DATABASE IF NOT EXISTS pick_up_trash
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Sử dụng database vừa tạo
USE pick_up_trash;


-- 2. TẠO BẢNG 'users'
-- Bảng này lưu trữ thông tin tài khoản của người chơi.
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL, -- QUAN TRỌNG: Trong thực tế, cột này nên lưu mật khẩu đã được băm (hashed).
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `high_score` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Giải thích các cột trong bảng `users`:
-- `id`:         Mã định danh duy nhất cho mỗi người dùng (Khóa chính).
-- `username`:   Tên đăng nhập, không được trùng.
-- `password`:   Mật khẩu. Độ dài 255 để tương thích với các thuật toán băm mật khẩu hiện đại.
-- `email`:      Email người dùng, không được trùng.
-- `high_score`: Lưu điểm số cao nhất của người dùng, mặc định là 0.
-- `created_at`: Thời gian tài khoản được tạo.


-- 3. TẠO BẢNG 'leaderboard'
-- Bảng này lưu trữ kết quả của các trận đấu để làm bảng xếp hạng.
DROP TABLE IF EXISTS `leaderboard`;
CREATE TABLE `leaderboard` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT UNSIGNED NOT NULL,
    `score` INT NOT NULL,
    `game_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Tạo khóa ngoại để liên kết với bảng 'users'
    -- ON DELETE CASCADE: Nếu một user bị xóa, tất cả các điểm số của họ trong leaderboard cũng sẽ bị xóa.
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Giải thích các cột trong bảng `leaderboard`:
-- `id`:        Mã định danh duy nhất cho mỗi lần ghi điểm.
-- `user_id`:   Liên kết tới người chơi trong bảng `users`.
-- `score`:     Điểm số đạt được trong trận đấu đó.
-- `game_date`: Thời gian trận đấu kết thúc.


-- 4. TẠO INDEXES (CHỈ MỤC)
-- Index giúp tăng tốc độ truy vấn dữ liệu trên các cột được sử dụng thường xuyên.
CREATE INDEX `idx_leaderboard_score` ON `leaderboard`(`score` DESC);
CREATE INDEX `idx_leaderboard_user_id` ON `leaderboard`(`user_id`);


-- 5. THÊM DỮ LIỆU MẪU (để kiểm tra)
-- Thêm một vài người dùng mẫu. Mật khẩu ở đây là '123' (chỉ dùng để test!).
INSERT INTO `users` (`username`, `password`, `email`, `high_score`) VALUES
('player1', '123', 'player1@example.com', 150),
('player2', '123', 'pro@example.com', 250),
('noobMaster', '123', 'noob@example.com', 75);

-- Thêm một vài điểm số mẫu vào bảng xếp hạng.
-- Lưu ý: user_id tương ứng với id của các user vừa tạo (1, 2, 3).
INSERT INTO `leaderboard` (`user_id`, `score`) VALUES
(1, 150), -- player1
(1, 120), -- player1
(2, 250), -- gamerPro
(2, 210), -- gamerPro
(3, 75),  -- noobMaster
(1, 90);  -- player1

-- Kết thúc script
COMMIT;

SELECT 'Cơ sở dữ liệu và các bảng đã được tạo thành công với dữ liệu mẫu.' AS 'Status';