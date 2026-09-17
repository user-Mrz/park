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
    phone VARCHAR(20) DEFAULT NULL COMMENT 'Phone number (NULL for WeChat users until bound)',
    password VARCHAR(128) DEFAULT '' COMMENT 'BCrypt hashed password',
    plate_number VARCHAR(20) DEFAULT '' COMMENT 'License plate number',
    avatar VARCHAR(255) DEFAULT '' COMMENT 'Avatar url (/uploads/avatar/xxx.jpg)',
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
(1,  'A-001', '教学楼区', 'B1', 0, 5.00),
(2,  'A-002', '教学楼区', 'B1', 0, 5.00),
(3,  'A-003', '教学楼区', 'B1', 0, 5.00),
(4,  'A-004', '教学楼区', 'B1', 0, 5.00),
(5,  'A-005', '教学楼区', 'B1', 0, 5.00),
(6,  'A-006', '教学楼区', 'B1', 0, 5.00),
(7,  'A-007', '教学楼区', 'B1', 0, 5.00),
(8,  'A-008', '教学楼区', 'B1', 0, 5.00),
(9,  'A-009', '教学楼区', 'B1', 0, 5.00),
(10, 'A-010', '教学楼区', 'B1', 0, 5.00),
(11, 'A-011', '教学楼区', 'B1', 0, 5.00),
(12, 'A-012', '教学楼区', 'B1', 0, 5.00),
(13, 'A-013', '教学楼区', 'B1', 0, 5.00),
(14, 'A-014', '教学楼区', 'B1', 0, 5.00),
(15, 'A-015', '教学楼区', 'B1', 0, 5.00),
(16, 'B-001', '图书馆区', 'B2', 0, 4.00),
(17, 'B-002', '图书馆区', 'B2', 0, 4.00),
(18, 'B-003', '图书馆区', 'B2', 0, 4.00),
(19, 'B-004', '图书馆区', 'B2', 0, 4.00),
(20, 'B-005', '图书馆区', 'B2', 0, 4.00),
(21, 'B-006', '图书馆区', 'B2', 0, 4.00),
(22, 'B-007', '图书馆区', 'B2', 0, 4.00),
(23, 'B-008', '图书馆区', 'B2', 0, 4.00),
(24, 'B-009', '图书馆区', 'B2', 0, 4.00),
(25, 'B-010', '图书馆区', 'B2', 0, 4.00),
(26, 'B-011', '图书馆区', 'B2', 0, 4.00),
(27, 'B-012', '图书馆区', 'B2', 0, 4.00),
(28, 'B-013', '图书馆区', 'B2', 0, 4.00),
(29, 'B-014', '图书馆区', 'B2', 0, 4.00),
(30, 'B-015', '图书馆区', 'B2', 0, 4.00),
(31, 'C-001', '宿舍区', 'B3', 0, 3.00),
(32, 'C-002', '宿舍区', 'B3', 0, 3.00),
(33, 'C-003', '宿舍区', 'B3', 0, 3.00),
(34, 'C-004', '宿舍区', 'B3', 0, 3.00),
(35, 'C-005', '宿舍区', 'B3', 0, 3.00),
(36, 'C-006', '宿舍区', 'B3', 0, 3.00),
(37, 'C-007', '宿舍区', 'B3', 0, 3.00),
(38, 'C-008', '宿舍区', 'B3', 0, 3.00),
(39, 'C-009', '宿舍区', 'B3', 0, 3.00),
(40, 'C-010', '宿舍区', 'B3', 0, 3.00),
(41, 'C-011', '宿舍区', 'B3', 0, 3.00),
(42, 'C-012', '宿舍区', 'B3', 0, 3.00),
(43, 'C-013', '宿舍区', 'B3', 0, 3.00),
(44, 'C-014', '宿舍区', 'B3', 0, 3.00),
(45, 'C-015', '宿舍区', 'B3', 0, 3.00),
(46, 'C-016', '宿舍区', 'B3', 0, 3.00),
(47, 'C-017', '宿舍区', 'B3', 0, 3.00),
(48, 'C-018', '宿舍区', 'B3', 0, 3.00),
(49, 'C-019', '宿舍区', 'B3', 0, 3.00),
(50, 'C-020', '宿舍区', 'B3', 0, 3.00);

-- Default system configs
INSERT INTO t_system_config (config_key, config_value, description) VALUES
('default_hourly_rate', '5.00', '默认每小时费率(元)'),
('max_reservation_days', '30', '最大预约天数'),
('min_reservation_hours', '1', '最小预约小时数'),
('grace_period_minutes', '15', '预约宽限期(分钟)');

-- Demo user (password: 123456)
INSERT INTO t_user (id, phone, password, nickname, plate_number, balance, status)
VALUES (10001, '13800138000', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO', 'Demo User', '粤B12345', 500.00, 1);

-- ============================================================
-- 校园化改造（新装环境自动生效；已有数据库请执行 docs/migration-campus.sql）
-- ============================================================

-- 1. 用户：校园身份
ALTER TABLE t_user
    ADD COLUMN user_no VARCHAR(32) DEFAULT NULL COMMENT '学号/工号' AFTER nickname,
    ADD COLUMN identity_type TINYINT DEFAULT 1 COMMENT '1学生 2教师 3职工 4访客 5家长' AFTER user_no,
    ADD COLUMN department VARCHAR(64) DEFAULT '' COMMENT '院系/部门' AFTER identity_type,
    ADD COLUMN class_name VARCHAR(64) DEFAULT '' COMMENT '班级' AFTER department,
    ADD COLUMN identity_status TINYINT DEFAULT 0 COMMENT '身份认证: 0待审核 1已认证 2已驳回' AFTER class_name,
    ADD COLUMN identity_remark VARCHAR(255) DEFAULT '' COMMENT '身份审核备注' AFTER identity_status,
    ADD UNIQUE KEY uk_user_no (user_no);

-- 2. 车位：校园分区与可停身份
ALTER TABLE t_parking_spot
    ADD COLUMN zone_code VARCHAR(32) DEFAULT 'teaching'
        COMMENT 'teaching教学楼区/library图书馆区/dorm宿舍区/gym体育馆区/visitor访客与家长临时区' AFTER area,
    ADD COLUMN allow_identities VARCHAR(64) DEFAULT '1,2,3'
        COMMENT '允许停放的身份类型(逗号分隔)' AFTER zone_code,
    ADD COLUMN free_minutes INT DEFAULT 0 COMMENT '本区域免费时长(分钟)，0 表示按身份规则' AFTER allow_identities;

UPDATE t_parking_spot SET zone_code = 'teaching', allow_identities = '1,2,3' WHERE area = '教学楼区';
UPDATE t_parking_spot SET zone_code = 'library',  allow_identities = '1,2,3' WHERE area = '图书馆区';
UPDATE t_parking_spot SET zone_code = 'dorm',     allow_identities = '1,2,3' WHERE area = '宿舍区';

-- 访客与家长临时停车区（仅访客/家长车辆可停放）
INSERT INTO t_parking_spot
    (id, spot_code, area, zone_code, allow_identities, free_minutes, floor, status, price_per_hour, remark)
VALUES
SELECT
    50 + n,
    CONCAT('V-', LPAD(n, 3, '0')),
    '访客与家长临时停车区',
    'visitor',
    '4,5',
    0,
    CASE WHEN n <= 100 THEN 'B1' WHEN n <= 200 THEN 'B2' ELSE 'B3' END,
    0,
    6.00,
    '访客与家长临时停车区'
FROM (
    SELECT a.n + b.n * 10 + c.n * 100 AS n
    FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
         (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
         (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
) nums
WHERE n BETWEEN 1 AND 300;


-- 3. 访客与家长车辆报备
DROP TABLE IF EXISTS t_visit_report;
CREATE TABLE t_visit_report (
    id BIGINT PRIMARY KEY COMMENT '报备ID(雪花)',
    owner_user_id BIGINT NOT NULL COMMENT '报备人(校内师生)',
    visit_type TINYINT NOT NULL DEFAULT 1 COMMENT '1访客 2家长',
    visitor_name VARCHAR(64) DEFAULT '' COMMENT '来访人姓名',
    plate_number VARCHAR(20) NOT NULL COMMENT '来访车牌',
    purpose VARCHAR(255) DEFAULT '' COMMENT '来访事由',
    expected_start DATETIME NOT NULL COMMENT '预计到校时间',
    expected_end DATETIME NOT NULL COMMENT '预计离校时间',
    status TINYINT DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回 3已入场 4已完成 5已过期 6已撤销',
    visitor_user_id BIGINT DEFAULT NULL COMMENT '审核通过后自动创建的访客账号',
    spot_id BIGINT DEFAULT NULL COMMENT '分配车位',
    vehicle_record_id BIGINT DEFAULT NULL COMMENT '停车记录',
    reviewer_id BIGINT DEFAULT NULL COMMENT '审核人',
    review_time DATETIME DEFAULT NULL,
    review_remark VARCHAR(255) DEFAULT '' COMMENT '审核意见',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_visit_owner (owner_user_id),
    KEY idx_visit_plate (plate_number),
    KEY idx_visit_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客与家长车辆报备';

-- 4. 违规记录
DROP TABLE IF EXISTS t_violation;
CREATE TABLE t_violation (
    id BIGINT PRIMARY KEY COMMENT '违规ID(雪花)',
    user_id BIGINT DEFAULT NULL,
    plate_number VARCHAR(20) DEFAULT '',
    vehicle_record_id BIGINT DEFAULT NULL,
    spot_id BIGINT DEFAULT NULL,
    type VARCHAR(32) DEFAULT 'overtime' COMMENT 'overtime超时占用 / illegal_zone违规区域 / occupied_others占位',
    description VARCHAR(255) DEFAULT '',
    points INT DEFAULT 0 COMMENT '扣分',
    fine_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '罚款金额',
    status TINYINT DEFAULT 0 COMMENT '0未处理 1已处理',
    operator_id BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_violation_user (user_id),
    KEY idx_violation_plate (plate_number),
    KEY idx_violation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录';

-- 5. 黑名单
DROP TABLE IF EXISTS t_blacklist;
CREATE TABLE t_blacklist (
    id BIGINT PRIMARY KEY COMMENT '黑名单ID(雪花)',
    plate_number VARCHAR(20) DEFAULT NULL,
    user_id BIGINT DEFAULT NULL,
    reason VARCHAR(255) DEFAULT '',
    until_time DATETIME DEFAULT NULL COMMENT 'NULL 表示永久',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_blacklist_plate (plate_number),
    KEY idx_blacklist_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单';

-- 6. 校园规则配置（管理员可在「系统配置」中调整）
INSERT INTO t_system_config (config_key, config_value, description) VALUES
    ('rate_student_discount',      '0.50', '学生停车折扣系数(0.5 = 五折)'),
    ('rate_teacher_free_minutes',  '120',  '教师免费停车时长(分钟)'),
    ('rate_staff_free_minutes',    '60',   '职工免费停车时长(分钟)'),
    ('rate_student_free_minutes',  '15',   '学生免费停车时长(分钟)'),
    ('rate_visitor_free_minutes',  '15',   '访客免费停车时长(分钟)'),
    ('rate_parent_free_minutes',   '30',   '家长接送临时停车免费时长(分钟)'),
    ('rate_visitor_multiplier',    '1.50', '访客费率系数(1.5 = 加价 50%)'),
    ('reserve_daily_limit_student','2',    '学生每日最多预约次数'),
    ('reserve_daily_limit_teacher','5',    '教师每日最多预约次数'),
    ('reserve_daily_limit_staff',  '3',    '职工每日最多预约次数'),
    ('reserve_daily_limit_visitor','1',    '访客/家长每日最多预约次数'),
    ('rule_night_ban_start',       '23:00','夜间禁止预约开始时间'),
    ('rule_night_ban_end',         '06:00','夜间禁止预约结束时间'),
    ('semester_mode',              '0',    '0=学期模式 1=假期模式'),
    ('holiday_discount',           '0.80', '假期模式统一折扣'),
    ('violation_overtime_grace_minutes', '30', '超时占用超过该时长记为违规(分钟)');

-- 7. 演示账号身份：10001 为教师，其余存量账号为已认证学生
UPDATE t_user SET identity_type = 2, identity_status = 1,
                  department = '计算机与信息工程学院', user_no = 'T2024001'
WHERE id = 10001;
UPDATE t_user SET identity_type = 1, identity_status = 1,
                  department = '计算机与信息工程学院', class_name = '计算机2101班',
                  user_no = CONCAT('S', CAST(id AS CHAR))
WHERE id <> 10001 AND user_no IS NULL;
