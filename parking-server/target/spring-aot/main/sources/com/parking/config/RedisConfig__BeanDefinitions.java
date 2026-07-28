package com.parking.config;

import java.lang.Object;
import java.lang.String;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassUtils;
import org.springframework.core.ResolvableType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Bean definitions for {@link RedisConfig}.
 */
@Generated
public class RedisConfig__BeanDefinitions {
  /**
   * Get the bean definition for 'redisConfig'.
   */
  public static BeanDefinition getRedisConfigBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(RedisConfig.class);
    beanDefinition.setTargetType(RedisConfig.class);
    ConfigurationClassUtils.initializeConfigurationClass(RedisConfig.class);
    beanDefinition.setInstanceSupplier(RedisConfig$$SpringCGLIB$$0::new);
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'redisTemplate'.
   */
  private static BeanInstanceSupplier<RedisTemplate> getRedisTemplateInstanceSupplier() {
    return BeanInstanceSupplier.<RedisTemplate>forFactoryMethod(RedisConfig.class, "redisTemplate", RedisConnectionFactory.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(RedisConfig.class).redisTemplate(args.get(0)));
  }

  /**
   * Get the bean definition for 'redisTemplate'.
   */
  public static BeanDefinition getRedisTemplateBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(RedisTemplate.class);
    beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(RedisTemplate.class, String.class, Object.class));
    beanDefinition.setInstanceSupplier(getRedisTemplateInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'stringRedisTemplate'.
   */
  private static BeanInstanceSupplier<StringRedisTemplate> getStringRedisTemplateInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<StringRedisTemplate>forFactoryMethod(RedisConfig.class, "stringRedisTemplate", RedisConnectionFactory.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(RedisConfig.class).stringRedisTemplate(args.get(0)));
  }

  /**
   * Get the bean definition for 'stringRedisTemplate'.
   */
  public static BeanDefinition getStringRedisTemplateBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(StringRedisTemplate.class);
    beanDefinition.setInstanceSupplier(getStringRedisTemplateInstanceSupplier());
    return beanDefinition;
  }
}
