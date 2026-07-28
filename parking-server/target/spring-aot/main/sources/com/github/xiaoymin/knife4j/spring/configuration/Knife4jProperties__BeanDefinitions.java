package com.github.xiaoymin.knife4j.spring.configuration;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link Knife4jProperties}.
 */
@Generated
public class Knife4jProperties__BeanDefinitions {
  /**
   * Get the bean definition for 'knife4jProperties'.
   */
  public static BeanDefinition getKnifejPropertiesBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Knife4jProperties.class);
    beanDefinition.setInstanceSupplier(Knife4jProperties::new);
    return beanDefinition;
  }
}
