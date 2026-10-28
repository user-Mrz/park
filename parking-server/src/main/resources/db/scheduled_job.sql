-- 定时任务管理：表结构与内置任务种子
-- 由 spring.sql.init 在应用启动时执行；CREATE IF NOT EXISTS / 空表种子保证幂等。

CREATE TABLE IF NOT EXISTS `t_scheduled_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(100) NOT NULL COMMENT '任务名称（唯一）',
  `job_group` varchar(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '任务分组',
  `handler_key` varchar(100) NOT NULL COMMENT '执行器标识（后端白名单 key）',
  `cron_expression` varchar(100) NOT NULL COMMENT 'Cron 表达式（秒 分 时 日 月 周）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0暂停 1运行',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  `last_run_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `last_run_result` varchar(500) DEFAULT '' COMMENT '上次执行结果',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_name` (`job_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态定时任务表';

-- 内置任务：仅在任务表为空（首次初始化，或管理员删除了全部任务）时写入，
-- 避免管理员删除单个内置任务后重启又被自动恢复。
INSERT INTO `t_scheduled_job`
(`id`, `job_name`, `job_group`, `handler_key`, `cron_expression`, `status`, `remark`)
SELECT * FROM (
  SELECT 1 AS id, '预约超时自动释放' AS job_name, 'SYSTEM' AS job_group, 'expireReservations' AS handler_key, '0 * * * * ?' AS cron_expression, 1 AS status, '每分钟扫描已过期预约并释放车位' AS remark
  UNION ALL SELECT 2, '潮汐批次状态同步', 'SYSTEM', 'syncTidalBatches', '0 * * * * ?', 1, '每分钟同步潮汐批次状态并清理已结束批次'
  UNION ALL SELECT 3, '车位缓存对账', 'SYSTEM', 'reconcileSpotCache', '*/30 * * * * ?', 1, '每 30 秒比对 Redis 与 MySQL 车位状态'
  UNION ALL SELECT 4, '月卡到期流转', 'SYSTEM', 'expireMonthlyPasses', '0 5 0 * * ?', 1, '每天 00:05 将到期月卡置为已过期'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM `t_scheduled_job`);

-- 普通管理员默认拥有「定时任务」菜单；超级管理员代码内直接放行
INSERT IGNORE INTO `t_role_menu` (`role_id`, `menu_code`, `created_at`)
SELECT `id`, 'jobs', NOW() FROM `t_role` WHERE `role_code` = 'admin';
