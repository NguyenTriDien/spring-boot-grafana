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

-- Bảng merchants
CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Bảng products (được cập nhật)
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    merchant_id BIGINT NOT NULL,
    CONSTRAINT fk_products_merchants FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- Dữ liệu mẫu merchants
INSERT INTO merchants (name) VALUES
('Apple Inc.'),
('Samsung Electronics'),
('Dell Technologies');

-- Dữ liệu mẫu products (tham chiếu merchants)
INSERT INTO products (name, description, price, merchant_id) VALUES
('Laptop Dell XPS 13', 'Laptop cao cấp với màn hình 13 inch, Intel i7', 15000000, 3),
('iPhone 15 Pro', 'Smartphone Apple với camera 48MP, chip A17 Pro', 25000000, 1),
('Samsung Galaxy S24', 'Android flagship với AI features', 20000000, 2),
('MacBook Air M2', 'Laptop Apple với chip M2, pin trâu', 30000000, 1),
('iPad Pro 12.9', 'Tablet cao cấp với màn hình 12.9 inch', 25000000, 1),
('AirPods Pro', 'Tai nghe không dây với noise cancellation', 5000000, 1),
('Apple Watch Series 9', 'Smartwatch với health monitoring', 8000000, 1),
('Sony WH-1000XM5', 'Headphone noise cancelling cao cấp', 6000000, 1),
('Nintendo Switch OLED', 'Console gaming portable', 7000000, 1),
('PlayStation 5', 'Console gaming thế hệ mới', 12000000, 1),
('Xbox Series X', 'Console gaming Microsoft', 11000000, 1),
('GoPro Hero 11', 'Camera action với 5.3K video', 9000000, 1),
('DJI Mini 3 Pro', 'Drone camera nhỏ gọn', 15000000, 1),
('Canon EOS R6', 'Camera mirrorless full-frame', 40000000, 1),
('Nikon Z6 II', 'Camera mirrorless với 2 card slots', 35000000, 1);
