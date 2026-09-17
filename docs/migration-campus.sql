-- ============================================================
-- 校园化改造迁移脚本（在已有数据库上执行一次）
-- 内容：校园身份体系、车位校园分区、访客/家长报备、违规与黑名单、校园规则配置
-- ============================================================

-- 1. 用户表：校园身份 ------------------------------
ALTER TABLE t_user
    ADD COLUMN user_no VARCHAR(32) DEFAULT NULL COMMENT '学号/工号' AFTER nickname,
    ADD COLUMN identity_type TINYINT DEFAULT 1 COMMENT '1学生 2教师 3职工 4访客 5家长' AFTER user_no,
    ADD COLUMN department VARCHAR(64) DEFAULT '' COMMENT '院系/部门' AFTER identity_type,
    ADD COLUMN class_name VARCHAR(64) DEFAULT '' COMMENT '班级' AFTER department,
    ADD COLUMN identity_status TINYINT DEFAULT 0 COMMENT '身份认证: 0待审核 1已认证 2已驳回' AFTER class_name,
    ADD COLUMN identity_remark VARCHAR(255) DEFAULT '' COMMENT '身份审核备注' AFTER identity_status,
    ADD UNIQUE KEY uk_user_no (user_no);

-- 2. 车位表：校园分区与可停身份 ------------------------------
ALTER TABLE t_parking_spot
    ADD COLUMN zone_code VARCHAR(32) DEFAULT 'teaching'
        COMMENT 'teaching教学楼区/library图书馆区/dorm宿舍区/gym体育馆区/visitor访客与家长临时区' AFTER area,
    ADD COLUMN allow_identities VARCHAR(64) DEFAULT '1,2,3'
        COMMENT '允许停放的身份类型(逗号分隔): 1学生 2教师 3职工 4访客 5家长' AFTER zone_code,
    ADD COLUMN free_minutes INT DEFAULT 0
        COMMENT '本区域免费时长(分钟)，0 表示按身份规则' AFTER allow_identities;

-- 3. 既有分区映射到校园区域 ------------------------------
UPDATE t_parking_spot SET zone_code = 'teaching', allow_identities = '1,2,3' WHERE area = 'A区';
UPDATE t_parking_spot SET zone_code = 'library',  allow_identities = '1,2,3' WHERE area = 'B区';
UPDATE t_parking_spot SET zone_code = 'dorm',     allow_identities = '1,2,3' WHERE area = 'C区';

-- 4. 新增「访客与家长临时停车区」（仅访客/家长车辆可停放） ------------------------------
INSERT INTO t_parking_spot
    (id, spot_code, area, zone_code, allow_identities, free_minutes, floor, status, price_per_hour, remark, created_at, updated_at)
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
    '访客与家长临时停车区',
    NOW(),
    NOW()
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

-- 4.1 分区名称中文化（A区/B区/C区/V区 -> 校园地区名称）
UPDATE t_parking_spot SET area = '教学楼区' WHERE area = 'A区';
UPDATE t_parking_spot SET area = '图书馆区' WHERE area = 'B区';
UPDATE t_parking_spot SET area = '宿舍区'   WHERE area = 'C区';
UPDATE t_parking_spot SET area = '访客与家长临时停车区' WHERE area = 'V区';
-- 可循环使用：家长接送高峰 30 分钟免费，超时按访客费率计费

-- 5. 访客与家长车辆报备 ------------------------------
CREATE TABLE IF NOT EXISTS t_visit_report (
    id BIGINT PRIMARY KEY COMMENT '报备ID(雪花)',
    owner_user_id BIGINT NOT NULL COMMENT '报备人(校内师生)',
    visit_type TINYINT NOT NULL DEFAULT 1 COMMENT '1访客 2家长',
    visitor_name VARCHAR(64) DEFAULT '' COMMENT '来访人姓名',
    plate_number VARCHAR(20) NOT NULL COMMENT '来访车牌',
    purpose VARCHAR(255) DEFAULT '' COMMENT '来访事由',
    expected_start DATETIME NOT NULL COMMENT '预计到校时间',
    expected_end DATETIME NOT NULL COMMENT '预计离校时间',
    status TINYINT DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回 3已入场 4已完成 5已过期',
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

-- 6. 违规记录 ------------------------------
CREATE TABLE IF NOT EXISTS t_violation (
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

-- 7. 黑名单（车牌或用户） ------------------------------
CREATE TABLE IF NOT EXISTS t_blacklist (
    id BIGINT PRIMARY KEY COMMENT '黑名单ID(雪花)',
    plate_number VARCHAR(20) DEFAULT NULL,
    user_id BIGINT DEFAULT NULL,
    reason VARCHAR(255) DEFAULT '',
    until_time DATETIME DEFAULT NULL COMMENT 'NULL 表示永久',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_blacklist_plate (plate_number),
    KEY idx_blacklist_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单';

-- 8. 校园规则配置（管理员可在「系统配置」里调整） ------------------------------
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
    ('holiday_discount',           '0.80', '假期模式统一折扣(0.8 = 八折)'),
    ('violation_overtime_grace_minutes', '30', '超时占用超过该时长记为违规(分钟)')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 9. 存量账号身份初始化（演示用：10001 为教师，其余为已认证学生） ------------------------------
UPDATE t_user SET identity_type = 1, identity_status = 1, department = '计算机与信息工程学院',
                  class_name = '计算机2101班', user_no = CONCAT('S', CAST(id AS CHAR))
WHERE user_no IS NULL AND identity_type = 1;
UPDATE t_user SET identity_type = 2, identity_status = 1, department = '计算机与信息工程学院',
                  class_name = '', user_no = 'T2024001'
WHERE id = 10001;
