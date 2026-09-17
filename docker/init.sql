SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS c2c_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS c2c_item CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS c2c_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE c2c_user;
CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT NOT NULL AUTO_INCREMENT, username VARCHAR(50) NOT NULL, password VARCHAR(100) NOT NULL,
  nickname VARCHAR(64), avatar VARCHAR(255), phone VARCHAR(20), status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, bio VARCHAR(255) DEFAULT '',
  PRIMARY KEY (id), UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, receiver VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL, region VARCHAR(100) NOT NULL, detail VARCHAR(255) NOT NULL,
  is_default TINYINT DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_address_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, target_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id),
  UNIQUE KEY uk_user_target (user_id, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE c2c_item;
CREATE TABLE IF NOT EXISTS category (
  id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(50) NOT NULL, parent_id BIGINT DEFAULT 0,
  sort INT DEFAULT 0, PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO category (id, name, parent_id, sort) VALUES
  (1, 'Digital', 0, 0), (2, 'Appliances', 0, 0), (3, 'Fashion', 0, 0);
CREATE TABLE IF NOT EXISTS item (
  id BIGINT NOT NULL AUTO_INCREMENT, seller_id BIGINT NOT NULL, category_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL, content TEXT, images VARCHAR(1000), price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2), condition_level TINYINT NOT NULL, status TINYINT NOT NULL DEFAULT 0,
  stock INT NOT NULL DEFAULT 1, city VARCHAR(50), view_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_category_id (category_id), KEY idx_seller_id (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE c2c_order;
CREATE TABLE IF NOT EXISTS order_info (
  id BIGINT NOT NULL AUTO_INCREMENT, order_no VARCHAR(64) NOT NULL, buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL, item_id BIGINT NOT NULL, pay_amount DECIMAL(10,2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0, create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  item_title VARCHAR(200), item_image VARCHAR(500), item_price DECIMAL(10,2), address_id BIGINT,
  receiver_name VARCHAR(50), receiver_phone VARCHAR(20), receiver_addr VARCHAR(500),
  PRIMARY KEY (id), UNIQUE KEY uk_order_no (order_no), KEY idx_buyer (buyer_id), KEY idx_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS order_review (
  id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, reviewer_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL, item_id BIGINT NOT NULL, rating INT NOT NULL, content VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id),
  UNIQUE KEY uk_order_reviewer (order_id, reviewer_id), KEY idx_review_target (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
