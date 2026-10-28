package com.parking.framework.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局单例 Snowflake ID 生成器，避免各 Service 重复创建实例浪费内存。
 */
@Configuration
public class SnowflakeConfig {

    @Bean
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(1, 1);
    }
}
