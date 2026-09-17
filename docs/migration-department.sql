-- ============================================================
-- 学院/部门管理 + 多身份演示用户
-- 适用：已执行 docs/migration-campus.sql 的数据库
-- ============================================================

-- 1. 学院/部门表 ------------------------------------------------
CREATE TABLE IF NOT EXISTS t_department (
    id BIGINT PRIMARY KEY COMMENT '学院ID(雪花)',
    name VARCHAR(64) NOT NULL COMMENT '学院/部门名称',
    code VARCHAR(32) DEFAULT '' COMMENT '学院代码',
    contact VARCHAR(32) DEFAULT '' COMMENT '联系人/辅导员',
    phone VARCHAR(20) DEFAULT '' COMMENT '联系电话',
    remark VARCHAR(255) DEFAULT '' COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    sort_order INT DEFAULT 0 COMMENT '排序号（越小越靠前）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院/部门';

-- 2. 种子学院（含既有院系，保证存量用户可继续使用） ------------------
INSERT INTO t_department (id, name, code, contact, phone, remark, status, sort_order) VALUES
    (900001, '计算机与信息工程学院', 'CS',  '王辅导员', '0731-88880001', '含计算机、软件工程、网络工程专业', 1, 1),
    (900002, '机械与电气工程学院',   'ME',  '李辅导员', '0731-88880002', '含机械设计、自动化、电气工程专业', 1, 2),
    (900003, '经济管理学院',         'EM',  '刘辅导员', '0731-88880003', '含工商管理、会计学、市场营销专业', 1, 3),
    (900004, '外国语学院',           'FL',  '陈辅导员', '0731-88880004', '含英语、日语专业', 1, 4),
    (900005, '体育学院',             'PE',  '赵辅导员', '0731-88880005', '体育教育与运动训练', 1, 5),
    (900006, '图书馆',               'LIB', '周老师',   '0731-88880006', '直属部门（职工）', 1, 6),
    (900007, '后勤保障处',           'LOG', '孙老师',   '0731-88880007', '直属部门（职工）', 1, 7)
ON DUPLICATE KEY UPDATE code = VALUES(code), remark = VALUES(remark);

-- 3. 多身份演示用户（密码均为 123456） ------------------------------
--    教师 / 职工 / 访客 / 家长，另含 1 名待审核教师用于演示认证审核
INSERT INTO t_user
    (id, phone, password, nickname, user_no, identity_type, department, class_name,
     identity_status, identity_remark, plate_number, balance, status)
VALUES
    (2100900000000000001, '13900000001', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '王老师', 'T2024002', 2, '计算机与信息工程学院', '', 1, '教师资格已核验', '粤B10001', 300.00, 1),
    (2100900000000000002, '13900000002', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '李老师（待审核）', 'T2024003', 2, '机械与电气工程学院', '', 0, '等待管理员审核工号', '粤B10002', 200.00, 1),
    (2100900000000000003, '13900000003', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '张职工', 'Z2024001', 3, '后勤保障处', '', 1, '职工工号已核验', '粤B10003', 200.00, 1),
    (2100900000000000004, '13900000004', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '访客刘先生', NULL, 4, '图书馆', '', 1, '', '粤B10004', 100.00, 1),
    (2100900000000000005, '13900000005', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '家长陈女士', NULL, 5, '', '', 1, '', '粤B10005', 100.00, 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), identity_type = VALUES(identity_type),
    department = VALUES(department), identity_status = VALUES(identity_status), plate_number = VALUES(plate_number);

-- 4. 既有学生按 ID 区间分配到不同学院（让院系统计有区分度） --------------
UPDATE t_user SET department = '经济管理学院', class_name = '会计2201班'
WHERE id BETWEEN 2090000000000000000 AND 2090000000000000027;
UPDATE t_user SET department = '外国语学院', class_name = '英语2201班'
WHERE id BETWEEN 2090000000000000030 AND 2090000000000000057;
UPDATE t_user SET department = '体育学院', class_name = '体育2201班'
WHERE id BETWEEN 2090000000000000060 AND 2090000000000000087;
