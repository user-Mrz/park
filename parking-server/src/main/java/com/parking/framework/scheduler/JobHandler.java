package com.parking.framework.scheduler;

/**
 * 定时任务执行器定义。
 *
 * @param key         执行器标识（保存到数据库 handler_key）
 * @param name        展示名称
 * @param description 功能说明
 * @param defaultCron 默认 Cron 表达式，供前端新增任务时预填
 * @param task        实际执行逻辑
 */
public record JobHandler(String key, String name, String description, String defaultCron, Runnable task) {
}
