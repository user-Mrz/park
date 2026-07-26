-- =============================================
-- Parking Management System - Database Init
-- =============================================

CREATE DATABASE IF NOT EXISTS parking_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE parking_system;

-- 1. User Table
DROP TABLE IF EXISTS t_recharge_record;
DROP TABLE IF EXISTS t_payment_record;
DROP TABLE IF EXISTS t_vehicle_record;
DROP TABLE IF EXISTS t_reservation;
DROP TABLE IF EXISTS t_parking_spot;
DROP TABLE IF EXISTS t_user;
DROP TABLE IF EXISTS t_admin;
DROP TABLE IF EXISTS t_system_config;

CREATE TABLE t_user (
    id BIGINT PRIMARY KEY COMMENT 'User ID (Snowflake)',
    openid VARCHAR(64) DEFAULT NULL COMMENT 'WeChat OpenID',
    nickname VARCHAR(64) DEFAULT '' COMMENT 'Nickname',
    phone VARCHAR(20) DEFAULT '' COMMENT 'Phone number',
    password VARCHAR(128) DEFAULT '' COMMENT 'BCrypt hashed password',
    plate_number VARCHAR(20) DEFAULT '' COMMENT 'License plate number',
    balance DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Account balance',
    status TINYINT DEFAULT 1 COMMENT '0-disabled 1-normal',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    UNIQUE KEY uk_phone (phone),
    KEY idx_openid (openid),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

-- 2. Admin Table
CREATE TABLE t_admin (
    id BIGINT PRIMARY KEY COMMENT 'Admin ID',
    username VARCHAR(64) NOT NULL COMMENT 'Username',
    password VARCHAR(128) NOT NULL COMMENT 'BCrypt hashed password',
    nickname VARCHAR(64) DEFAULT '' COMMENT 'Display name',
    role VARCHAR(32) DEFAULT 'normal' COMMENT 'super / normal',
    status TINYINT DEFAULT 1 COMMENT '0-disabled 1-normal',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Admin table';

-- 3. Parking Spot Table
CREATE TABLE t_parking_spot (
    id BIGINT PRIMARY KEY COMMENT 'Spot ID',
    spot_code VARCHAR(32) NOT NULL COMMENT 'Spot code e.g. A-001',
    area VARCHAR(64) DEFAULT '' COMMENT 'Area / Zone',
    floor VARCHAR(32) DEFAULT '' COMMENT 'Floor',
    status TINYINT DEFAULT 0 COMMENT '0-vacant 1-occupied 2-reserved 3-maintenance',
    price_per_hour DECIMAL(10,2) DEFAULT 5.00 COMMENT 'Hourly rate (yuan)',
    remark VARCHAR(256) DEFAULT '' COMMENT 'Remark',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_spot_code (spot_code),
    KEY idx_area (area),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Parking spot table';

-- 4. Reservation Table
CREATE TABLE t_reservation (
    id BIGINT PRIMARY KEY COMMENT 'Reservation ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    spot_id BIGINT NOT NULL COMMENT 'Spot ID',
    reservation_no VARCHAR(64) NOT NULL COMMENT 'Reservation number',
    start_time DATETIME NOT NULL COMMENT 'Reservation start time',
    end_time DATETIME NOT NULL COMMENT 'Reservation end time',
    duration_hours DECIMAL(5,1) NOT NULL COMMENT 'Reservation duration in hours',
    total_amount DECIMAL(10,2) NOT NULL COMMENT 'Pre-calculated fee',
    status TINYINT DEFAULT 0 COMMENT '0-pending 1-active 2-completed 3-cancelled 4-expired',
    cancel_reason VARCHAR(256) DEFAULT '' COMMENT 'Cancel reason',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_no (reservation_no),
    KEY idx_user_id (user_id),
    KEY idx_spot_id (spot_id),
    KEY idx_status (status),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reservation table';

-- 5. Vehicle Record Table (Entry/Exit)
CREATE TABLE t_vehicle_record (
    id BIGINT PRIMARY KEY COMMENT 'Record ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    spot_id BIGINT DEFAULT NULL COMMENT 'Spot ID',
    reservation_id BIGINT DEFAULT NULL COMMENT 'Related reservation ID',
    plate_number VARCHAR(20) NOT NULL COMMENT 'License plate',
    entry_time DATETIME NOT NULL COMMENT 'Entry time',
    exit_time DATETIME DEFAULT NULL COMMENT 'Exit time',
    duration_minutes INT DEFAULT NULL COMMENT 'Stay duration in minutes',
    total_fee DECIMAL(10,2) DEFAULT NULL COMMENT 'Total parking fee',
    payment_status TINYINT DEFAULT 0 COMMENT '0-unpaid 1-paid',
    status TINYINT DEFAULT 0 COMMENT '0-parked 1-exited 2-admin-override',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_spot_id (spot_id),
    KEY idx_plate_number (plate_number),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vehicle entry/exit record';

-- 6. Recharge Record Table
CREATE TABLE t_recharge_record (
    id BIGINT PRIMARY KEY COMMENT 'Recharge ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    order_no VARCHAR(64) NOT NULL COMMENT 'Order number',
    amount DECIMAL(10,2) NOT NULL COMMENT 'Recharge amount',
    pay_method VARCHAR(32) DEFAULT 'wechat' COMMENT 'Payment method: wechat / admin',
    status TINYINT DEFAULT 0 COMMENT '0-pending 1-success 2-failed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recharge record';

-- 7. Payment Record Table (Deduction)
CREATE TABLE t_payment_record (
    id BIGINT PRIMARY KEY COMMENT 'Payment ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    vehicle_record_id BIGINT NOT NULL COMMENT 'Vehicle record ID',
    amount DECIMAL(10,2) NOT NULL COMMENT 'Deducted amount',
    balance_before DECIMAL(10,2) NOT NULL COMMENT 'Balance before deduction',
    balance_after DECIMAL(10,2) NOT NULL COMMENT 'Balance after deduction',
    type VARCHAR(32) DEFAULT 'auto_deduct' COMMENT 'auto_deduct / manual',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_vehicle_record_id (vehicle_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment/deduction record';

-- 8. System Config Table
CREATE TABLE t_system_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL COMMENT 'Config key',
    config_value VARCHAR(256) NOT NULL COMMENT 'Config value',
    description VARCHAR(256) DEFAULT '' COMMENT 'Description',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System configuration';

-- =============================================
-- Default Data
-- =============================================

-- Default admin (password: admin123, BCrypt)
INSERT INTO t_admin (id, username, password, nickname, role, status)
VALUES (1, 'admin', '$2a$10$qQ66MbeZATLJ81NLxubRIOpc9ka3iG40tysLK0f2Tb30QQ7W9ac8O', 'Super Admin', 'super', 1);

-- Default parking spots (50 spots across 3 areas)
INSERT INTO t_parking_spot (id, spot_code, area, floor, status, price_per_hour) VALUES
(1,  'A-001', 'A区', 'B1', 0, 5.00),
(2,  'A-002', 'A区', 'B1', 0, 5.00),
(3,  'A-003', 'A区', 'B1', 0, 5.00),
(4,  'A-004', 'A区', 'B1', 0, 5.00),
(5,  'A-005', 'A区', 'B1', 0, 5.00),
(6,  'A-006', 'A区', 'B1', 0, 5.00),
(7,  'A-007', 'A区', 'B1', 0, 5.00),
(8,  'A-008', 'A区', 'B1', 0, 5.00),
(9,  'A-009', 'A区', 'B1', 0, 5.00),
(10, 'A-010', 'A区', 'B1', 0, 5.00),
(11, 'A-011', 'A区', 'B1', 0, 5.00),
(12, 'A-012', 'A区', 'B1', 0, 5.00),
(13, 'A-013', 'A区', 'B1', 0, 5.00),
(14, 'A-014', 'A区', 'B1', 0, 5.00),
(15, 'A-015', 'A区', 'B1', 0, 5.00),
(16, 'B-001', 'B区', 'B2', 0, 4.00),
(17, 'B-002', 'B区', 'B2', 0, 4.00),
(18, 'B-003', 'B区', 'B2', 0, 4.00),
(19, 'B-004', 'B区', 'B2', 0, 4.00),
(20, 'B-005', 'B区', 'B2', 0, 4.00),
(21, 'B-006', 'B区', 'B2', 0, 4.00),
(22, 'B-007', 'B区', 'B2', 0, 4.00),
(23, 'B-008', 'B区', 'B2', 0, 4.00),
(24, 'B-009', 'B区', 'B2', 0, 4.00),
(25, 'B-010', 'B区', 'B2', 0, 4.00),
(26, 'B-011', 'B区', 'B2', 0, 4.00),
(27, 'B-012', 'B区', 'B2', 0, 4.00),
(28, 'B-013', 'B区', 'B2', 0, 4.00),
(29, 'B-014', 'B区', 'B2', 0, 4.00),
(30, 'B-015', 'B区', 'B2', 0, 4.00),
(31, 'C-001', 'C区', 'B3', 0, 3.00),
(32, 'C-002', 'C区', 'B3', 0, 3.00),
(33, 'C-003', 'C区', 'B3', 0, 3.00),
(34, 'C-004', 'C区', 'B3', 0, 3.00),
(35, 'C-005', 'C区', 'B3', 0, 3.00),
(36, 'C-006', 'C区', 'B3', 0, 3.00),
(37, 'C-007', 'C区', 'B3', 0, 3.00),
(38, 'C-008', 'C区', 'B3', 0, 3.00),
(39, 'C-009', 'C区', 'B3', 0, 3.00),
(40, 'C-010', 'C区', 'B3', 0, 3.00),
(41, 'C-011', 'C区', 'B3', 0, 3.00),
(42, 'C-012', 'C区', 'B3', 0, 3.00),
(43, 'C-013', 'C区', 'B3', 0, 3.00),
(44, 'C-014', 'C区', 'B3', 0, 3.00),
(45, 'C-015', 'C区', 'B3', 0, 3.00),
(46, 'C-016', 'C区', 'B3', 0, 3.00),
(47, 'C-017', 'C区', 'B3', 0, 3.00),
(48, 'C-018', 'C区', 'B3', 0, 3.00),
(49, 'C-019', 'C区', 'B3', 0, 3.00),
(50, 'C-020', 'C区', 'B3', 0, 3.00);

-- Default system configs
INSERT INTO t_system_config (config_key, config_value, description) VALUES
('default_hourly_rate', '5.00', 'Default hourly parking rate (yuan)'),
('max_reservation_days', '30', 'Max reservation duration in days'),
('min_reservation_hours', '1', 'Min reservation duration in hours'),
('grace_period_minutes', '15', 'Grace period after reservation start (minutes)');

-- Demo user (password: 123456)
INSERT INTO t_user (id, phone, password, nickname, plate_number, balance, status)
VALUES (10001, '13800138000', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO', 'Demo User', '粤B12345', 500.00, 1);
