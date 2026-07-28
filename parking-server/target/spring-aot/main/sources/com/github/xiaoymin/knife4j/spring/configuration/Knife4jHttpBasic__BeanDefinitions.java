package com.github.xiaoymin.knife4j.spring.configuration;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link Knife4jHttpBasic}.
 */
@Generated
public class Knife4jHttpBasic__BeanDefinitions {
  /**
   * Get the bean definition for 'knife4jHttpBasic'.
   */
  public static BeanDefinition getKnifejHttpBasicBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Knife4jHttpBasic.class);
    beanDefinition.setInstanceSupplier(Knife4jHttpBasic::new);
    return beanDefinition;
  }
}
