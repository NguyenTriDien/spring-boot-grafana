-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS demo_db;
USE demo_db;

-- Tạo user ứng dụng Spring Boot (nếu chưa tồn tại) và cấp quyền
CREATE USER IF NOT EXISTS 'demo_user'@'%' IDENTIFIED BY 'demo_password';
GRANT ALL PRIVILEGES ON demo_db.* TO 'demo_user'@'%';

-- Tạo user monitoring cho mysqld_exporter
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter_password' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
-- Áp dụng thay đổi
FLUSH PRIVILEGES;

-- (Tùy chọn) Insert dữ liệu mẫu vào bảng products nếu bạn muốn
-- Lưu ý: Bảng `products` sẽ được JPA tạo sau khi ứng dụng Spring Boot khởi động
-- INSERT INTO products (...) VALUES (...);
