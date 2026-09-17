-- ============================================================
-- 演示数据生成脚本（黑名单 / 访客与家长报备 / 违规记录 / 预约 / 停车与扣费 / 充值）
-- 特点：使用会话变量从现有数据中取用户与车位，可在任意环境重复执行（执行前会清理旧演示数据）
-- 说明：所有演示数据 ID 以 2101 开头，便于识别与清理
-- ============================================================
USE parking_system;

-- 0. 清理旧的演示数据（可重复执行） ------------------------------
-- 0.1 先释放上一次演示数据占用的车位（避免重复执行后车位状态残留）
UPDATE t_parking_spot SET status = 0 WHERE id IN (
    SELECT spot_id FROM t_vehicle_record WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999 AND spot_id IS NOT NULL);
UPDATE t_parking_spot SET status = 0 WHERE id IN (
    SELECT spot_id FROM t_reservation WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999 AND spot_id IS NOT NULL);
UPDATE t_parking_spot SET status = 0 WHERE id IN (
    SELECT spot_id FROM t_visit_report WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999 AND spot_id IS NOT NULL);

-- 0.2 再删除演示数据
DELETE FROM t_payment_record WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_recharge_record WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_violation WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_blacklist WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_visit_report WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_vehicle_record WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_reservation WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;
DELETE FROM t_user WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999;

-- 1. 取样例用户 / 管理员 / 车位 ----------------------------------
--    优先使用「已知手机号」的演示学生，便于用小程序登录查看"我的违规/我的预约/停车记录"
SET @stu1    = 2101000000000000054;   -- 学生·刘同学（13900000006）
SET @stu2    = 2101000000000000055;   -- 学生·陈同学（13900000007）
SET @stu3    = (SELECT id FROM t_user WHERE identity_type = 1 AND identity_status = 1 ORDER BY id LIMIT 1);
SET @stu4    = (SELECT id FROM t_user WHERE identity_type = 1 AND identity_status = 1 ORDER BY id LIMIT 1 OFFSET 1);
SET @teacher = (SELECT id FROM t_user WHERE identity_type = 2 AND identity_status = 1 ORDER BY id LIMIT 1);
SET @parent  = (SELECT id FROM t_user WHERE identity_type = 5 ORDER BY id LIMIT 1);
SET @admin   = (SELECT id FROM t_admin ORDER BY id LIMIT 1);

SET @spotTeach  = (SELECT id FROM t_parking_spot WHERE zone_code = 'teaching' AND status = 0 ORDER BY spot_code LIMIT 1);
SET @spotDorm1  = (SELECT id FROM t_parking_spot WHERE zone_code = 'dorm'     AND status = 0 ORDER BY spot_code LIMIT 1);
SET @spotDorm2  = (SELECT id FROM t_parking_spot WHERE zone_code = 'dorm'     AND status = 0 ORDER BY spot_code LIMIT 1 OFFSET 1);
SET @spotLib    = (SELECT id FROM t_parking_spot WHERE zone_code = 'library'  AND status = 0 ORDER BY spot_code LIMIT 1);
SET @v1 = (SELECT id FROM t_parking_spot WHERE zone_code = 'visitor' AND status = 0 ORDER BY spot_code LIMIT 1);
SET @v2 = (SELECT id FROM t_parking_spot WHERE zone_code = 'visitor' AND status = 0 ORDER BY spot_code LIMIT 1 OFFSET 1);
SET @v3 = (SELECT id FROM t_parking_spot WHERE zone_code = 'visitor' AND status = 0 ORDER BY spot_code LIMIT 1 OFFSET 2);
SET @v4 = (SELECT id FROM t_parking_spot WHERE zone_code = 'visitor' AND status = 0 ORDER BY spot_code LIMIT 1 OFFSET 3);

-- 2. 演示账号：2 名学生 + 3 个访客/家长（密码均为 123456；访客账号无手机号仅用于停车记录） --
INSERT INTO t_user (id, phone, password, nickname, user_no, identity_type, department, class_name,
                    identity_status, identity_remark, plate_number, balance, status)
VALUES
    (2101000000000000054, '13900000006', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '学生·刘同学', 'S2024001', 1, '计算机与信息工程学院', '计算机2101班', 1, '学号已核验', '粤B20001', 320.00, 1),
    (2101000000000000055, '13900000007', '$2a$10$DcxUnmo6fjDWtlIyuwx/K.Ma1zjO6l/fZDMP9yFCPxvOLVMGHBnpO',
     '学生·陈同学', 'S2024002', 1, '经济管理学院', '会计2201班', 1, '学号已核验', '粤B20002', 210.00, 1),
    (2101000000000000051, NULL, NULL, '访客·张先生', NULL, 4, '图书馆', '', 1, '来访访客车辆',   '粤B30001', 0.00, 1),
    (2101000000000000052, NULL, NULL, '家长·李女士', NULL, 5, '',       '', 1, '家长接送临时车辆','粤B30002', 0.00, 1),
    (2101000000000000053, NULL, NULL, '访客·周先生', NULL, 4, '图书馆', '', 1, '来访访客车辆',   '粤B30003', 0.00, 1);

-- 3. 预约记录（进行中 / 已完成 / 已过期） --------------------------
--    注意：进行中的预约独占车位（status=2），与其上停放车辆的车位互不重叠
INSERT INTO t_reservation (id, user_id, spot_id, reservation_no, start_time, end_time,
                           duration_hours, total_amount, status, created_at, updated_at)
VALUES
    (2101000000000000061, @stu4,    @spotDorm2, 'RES-DEMO-0001', NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 5 HOUR, 3.0, 4.50,  1, NOW(), NOW()),
    (2101000000000000062, @teacher, @spotLib,   'RES-DEMO-0002', NOW() + INTERVAL 1 DAY,  NOW() + INTERVAL 27 HOUR, 2.0, 8.00,  1, NOW(), NOW()),
    (2101000000000000063, @stu2,    @spotTeach, 'RES-DEMO-0003', NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 46 HOUR, 2.0, 5.00,  2, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (2101000000000000064, @stu1,    @spotDorm1, 'RES-DEMO-0004', NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 71 HOUR, 1.0, 1.50,  4, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY);
UPDATE t_parking_spot SET status = 2 WHERE id IN (@spotDorm2, @spotLib);

-- 4. 停车记录：2 辆在校内停放（占用车位） --------------------------
INSERT INTO t_vehicle_record (id, user_id, spot_id, plate_number, entry_time, payment_status, status, created_at, updated_at)
VALUES
    (2101000000000000001, @stu1, @spotTeach, '粤B20001', NOW() - INTERVAL 2 HOUR,  0, 0, NOW() - INTERVAL 2 HOUR,  NOW()),
    (2101000000000000002, @stu2, @spotDorm1, '粤B20002', NOW() - INTERVAL 45 MINUTE, 0, 0, NOW() - INTERVAL 45 MINUTE, NOW());
UPDATE t_parking_spot SET status = 1 WHERE id IN (@spotTeach, @spotDorm1);

-- 5. 历史停车记录（已出场，含费用）+ 对应扣费记录 --------------------
INSERT INTO t_vehicle_record (id, user_id, spot_id, plate_number, entry_time, exit_time, duration_minutes,
                              total_fee, payment_status, status, created_at, updated_at)
VALUES
    (2101000000000000011, @stu1,    @spotTeach, '粤B20001', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 45 MINUTE,  45,  2.50, 1, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY),
    (2101000000000000012, @stu1,    @spotLib,   '粤B20001', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY + INTERVAL 90 MINUTE,  90,  4.00, 1, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),
    (2101000000000000013, @stu2,    @spotDorm2, '粤B20002', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 180 MINUTE, 180, 4.50, 1, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
    (2101000000000000014, @teacher, @spotTeach, '粤B10001', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 240 MINUTE, 240, 10.00, 1, 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY),
    (2101000000000000015, @teacher, @spotDorm1, '粤B10001', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 300 MINUTE, 300, 9.00, 1, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (2101000000000000016, @stu3,    @spotLib,   '粤B20003', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 30 MINUTE,  30,  2.00, 1, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY);

INSERT INTO t_payment_record (id, user_id, vehicle_record_id, amount, balance_before, balance_after, type, created_at)
VALUES
    (2101000000000000111, @stu1,    2101000000000000011, 2.50,  320.00, 317.50, 'auto_deduct',       NOW() - INTERVAL 5 DAY),
    (2101000000000000112, @stu1,    2101000000000000012, 4.00,  317.50, 313.50, 'auto_deduct',       NOW() - INTERVAL 4 DAY),
    (2101000000000000113, @stu2,    2101000000000000013, 4.50,  210.00, 205.50, 'reservation_settle',NOW() - INTERVAL 3 DAY),
    (2101000000000000114, @teacher, 2101000000000000014, 10.00, 330.00, 320.00, 'auto_deduct',       NOW() - INTERVAL 6 DAY),
    (2101000000000000115, @teacher, 2101000000000000015, 9.00,  320.00, 311.00, 'auto_deduct',       NOW() - INTERVAL 2 DAY),
    (2101000000000000116, @stu3,    2101000000000000016, 2.00,  150.00, 148.00, 'admin_force',       NOW() - INTERVAL 1 DAY);

-- 6. 充值记录 ----------------------------------------------------
INSERT INTO t_recharge_record (id, user_id, order_no, amount, pay_method, status, created_at)
VALUES
    (2101000000000000121, @stu1,    'RECH-DEMO-0001', 100.00, 'wechat', 1, NOW() - INTERVAL 6 DAY),
    (2101000000000000122, @stu2,    'RECH-DEMO-0002', 50.00,  'alipay', 1, NOW() - INTERVAL 4 DAY),
    (2101000000000000123, @stu3,    'RECH-DEMO-0003', 200.00, 'wechat', 1, NOW() - INTERVAL 3 DAY),
    (2101000000000000124, @teacher, 'RECH-DEMO-0004', 100.00, 'wechat', 1, NOW() - INTERVAL 2 DAY),
    (2101000000000000125, @parent,  'RECH-DEMO-0005', 100.00, 'wechat', 1, NOW() - INTERVAL 1 DAY);

-- 7. 违规记录（超时占用 / 违规区域 / 占位；含已处理与未处理） --------
INSERT INTO t_violation (id, user_id, plate_number, vehicle_record_id, spot_id, type, description,
                         points, fine_amount, status, operator_id, handled_at, created_at)
VALUES
    (2101000000000000131, @stu1, '粤B20001', 2101000000000000011, @spotTeach, 'overtime',
     '超时占用 15 分钟（预约停车）', 1, 0.00, 0, NULL, NULL, NOW() - INTERVAL 5 DAY),
    (2101000000000000132, @teacher, '粤B10001', 2101000000000000015, @spotDorm1, 'overtime',
     '超时占用 150 分钟（预约停车）', 2, 0.00, 0, NULL, NULL, NOW() - INTERVAL 2 DAY),
    (2101000000000000133, @stu2, '粤B20002', 2101000000000000013, @spotDorm2, 'overtime',
     '超时占用 75 分钟（预约停车） | 处理意见：已电话通知车主', 1, 0.00, 1, @admin, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 DAY),
    (2101000000000000134, @stu3, '粤B20003', NULL, @v1, 'illegal_zone',
     '违规区域入场：学生（已认证）驶入「访客与家长临时停车区」', 2, 0.00, 0, NULL, NULL, NOW() - INTERVAL 1 DAY),
    (2101000000000000135, @stu2, '粤B20002', NULL, @spotLib, 'occupied_others',
     '占用他人已预约车位 | 处理意见：已现场劝导驶离', 2, 0.00, 1, @admin, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 5 DAY);

-- 8. 车辆黑名单（永久 / 临时 / 按用户） ----------------------------
INSERT INTO t_blacklist (id, plate_number, user_id, reason, until_time, created_at)
VALUES
    (2101000000000000141, '粤B99999', NULL,   '多次超时占用车位（累计 3 次）',            NULL,                       NOW() - INTERVAL 12 DAY),
    (2101000000000000142, '粤A12345', NULL,   '占用他人预约车位且拒不驶离',              NULL,                       NOW() - INTERVAL 6 DAY),
    (2101000000000000143, '粤C55555', NULL,   '违规停放消防通道（临时限制 30 天）',      NOW() + INTERVAL 30 DAY,    NOW() - INTERVAL 3 DAY),
    (2101000000000000144, NULL,       @stu3,  '账号违规：损坏车位地锁（限制 90 天）',    NOW() + INTERVAL 90 DAY,    NOW() - INTERVAL 1 DAY);

-- 9. 访客与家长车辆报备（覆盖全部状态） ----------------------------
-- 9.1 已入场（家长已在访客区停放 → 占用 V-001）
INSERT INTO t_visit_report (id, owner_user_id, visit_type, visitor_name, plate_number, purpose,
                            expected_start, expected_end, status, visitor_user_id, spot_id, vehicle_record_id,
                            reviewer_id, review_time, review_remark, created_at, updated_at)
VALUES
    (2101000000000000151, @stu1, 2, '李女士（母亲）', '粤B30002', '接送学生',
     NOW() - INTERVAL 2 HOUR, NOW() + INTERVAL 2 HOUR, 3, 2101000000000000052, @v1, 2101000000000000171,
     @admin, NOW() - INTERVAL 3 HOUR, '同意入校', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 2 HOUR);

INSERT INTO t_vehicle_record (id, user_id, spot_id, plate_number, entry_time, payment_status, status, created_at, updated_at)
VALUES (2101000000000000171, 2101000000000000052, @v1, '粤B30002', NOW() - INTERVAL 2 HOUR, 0, 0, NOW() - INTERVAL 2 HOUR, NOW());

-- 9.2 已完成（访客已离场结算 → V-004）
INSERT INTO t_visit_report (id, owner_user_id, visit_type, visitor_name, plate_number, purpose,
                            expected_start, expected_end, status, visitor_user_id, spot_id, vehicle_record_id,
                            reviewer_id, review_time, review_remark, created_at, updated_at)
VALUES
    (2101000000000000152, @teacher, 1, '周先生', '粤B30003', '学术交流',
     NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 20 HOUR, 4, 2101000000000000053, @v4, 2101000000000000172,
     @admin, NOW() - INTERVAL 26 HOUR, '同意入校', NOW() - INTERVAL 28 HOUR, NOW() - INTERVAL 19 HOUR);

INSERT INTO t_vehicle_record (id, user_id, spot_id, plate_number, entry_time, exit_time, duration_minutes,
                              total_fee, payment_status, status, created_at, updated_at)
VALUES (2101000000000000172, 2101000000000000053, @v4, '粤B30003',
        NOW() - INTERVAL 24 HOUR, NOW() - INTERVAL 22 HOUR, 120, 18.00, 1, 1, NOW() - INTERVAL 24 HOUR, NOW() - INTERVAL 22 HOUR);

INSERT INTO t_payment_record (id, user_id, vehicle_record_id, amount, balance_before, balance_after, type, created_at)
VALUES (2101000000000000117, 2101000000000000053, 2101000000000000172, 18.00, 18.00, 0.00, 'visitor_settle', NOW() - INTERVAL 22 HOUR);

-- 9.3 已通过（待入场，已分配访客区车位 → V-002 / V-003）
INSERT INTO t_visit_report (id, owner_user_id, visit_type, visitor_name, plate_number, purpose,
                            expected_start, expected_end, status, visitor_user_id, spot_id,
                            reviewer_id, review_time, review_remark, created_at, updated_at)
VALUES
    (2101000000000000153, @stu2, 2, '王先生（父亲）', '粤B30011', '家长会',
     NOW() + INTERVAL 3 HOUR, NOW() + INTERVAL 5 HOUR, 1, NULL, @v2,
     @admin, NOW() - INTERVAL 1 HOUR, '同意入校，请按规定停放', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR),
    (2101000000000000154, @teacher, 1, '陈工程师', '粤B30012', '设备维护',
     NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 26 HOUR, 1, NULL, @v3,
     @admin, NOW() - INTERVAL 30 MINUTE, '同意入校', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 30 MINUTE);
UPDATE t_parking_spot SET status = 2 WHERE id IN (@v2, @v3);
UPDATE t_parking_spot SET status = 1 WHERE id = @v1;
-- 9.4 待审核（管理员待处理）
INSERT INTO t_visit_report (id, owner_user_id, visit_type, visitor_name, plate_number, purpose,
                            expected_start, expected_end, status, created_at, updated_at)
VALUES
    (2101000000000000155, @stu1, 2, '赵女士（母亲）', '粤B30021', '接送学生放学',
     NOW() + INTERVAL 6 HOUR, NOW() + INTERVAL 8 HOUR, 0, NOW() - INTERVAL 20 MINUTE, NOW() - INTERVAL 20 MINUTE),
    (2101000000000000156, @stu3, 1, '孙同学（外校）', '粤B30022', '参加社团活动',
     NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 29 HOUR, 0, NOW() - INTERVAL 10 MINUTE, NOW() - INTERVAL 10 MINUTE);

-- 9.5 已驳回 / 已撤销
INSERT INTO t_visit_report (id, owner_user_id, visit_type, visitor_name, plate_number, purpose,
                            expected_start, expected_end, status, reviewer_id, review_time, review_remark,
                            created_at, updated_at)
VALUES
    (2101000000000000157, @stu2, 1, '未知来访人', '粤B30031', '送货',
     NOW() + INTERVAL 2 HOUR, NOW() + INTERVAL 3 HOUR, 2, @admin, NOW() - INTERVAL 5 HOUR,
     '驳回：来访事由不明确，且未提供来访单位信息', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 5 HOUR),
    (2101000000000000158, @stu1, 2, '钱女士（母亲）', '粤B30032', '接送学生',
     NOW() + INTERVAL 4 HOUR, NOW() + INTERVAL 6 HOUR, 6, NULL, NULL,
     '', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 2 HOUR);

-- 10. 结果确认 ---------------------------------------------------
SELECT '演示数据生成完成' AS 结果,
       (SELECT COUNT(*) FROM t_user            WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 访客账号,
       (SELECT COUNT(*) FROM t_reservation     WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 预约,
       (SELECT COUNT(*) FROM t_vehicle_record  WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 停车记录,
       (SELECT COUNT(*) FROM t_payment_record  WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 扣费记录,
       (SELECT COUNT(*) FROM t_recharge_record WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 充值记录,
       (SELECT COUNT(*) FROM t_violation       WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 违规记录,
       (SELECT COUNT(*) FROM t_blacklist       WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 黑名单,
       (SELECT COUNT(*) FROM t_visit_report    WHERE id BETWEEN 2101000000000000000 AND 2101000000000000999) AS 访客报备;
