package com.baomidou.mybatisplus.autoconfigure;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link MybatisPlusProperties}.
 */
@Generated
public class MybatisPlusProperties__BeanDefinitions {
  /**
   * Get the bean definition for 'mybatisPlusProperties'.
   */
  public static BeanDefinition getMybatisPlusPropertiesBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MybatisPlusProperties.class);
    beanDefinition.setInstanceSupplier(MybatisPlusProperties::new);
    return beanDefinition;
  }
}
