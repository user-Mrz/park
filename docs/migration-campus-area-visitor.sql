-- ============================================================
-- 校园分区命名中文化 + 访客与家长临时停车区扩容至 300 个车位
-- 适用：已执行 docs/migration-campus.sql 的数据库（增量升级）
-- ============================================================

-- 1. 分区名称改为中文地区名称（现场/管理端展示）
UPDATE t_parking_spot SET area = '教学楼区' WHERE area = 'A区';
UPDATE t_parking_spot SET area = '图书馆区' WHERE area = 'B区';
UPDATE t_parking_spot SET area = '宿舍区'   WHERE area = 'C区';
UPDATE t_parking_spot SET area = '访客与家长临时停车区' WHERE area = 'V区';

-- 2. 访客与家长临时停车区扩容至 300 个车位
--    保留既有 V-001 ~ V-006，新增 V-007 ~ V-300（车位 ID = 50 + 序号）
INSERT INTO t_parking_spot
    (id, spot_code, area, zone_code, allow_identities, free_minutes, floor, status, price_per_hour, remark)
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
WHERE n BETWEEN 7 AND 300;
