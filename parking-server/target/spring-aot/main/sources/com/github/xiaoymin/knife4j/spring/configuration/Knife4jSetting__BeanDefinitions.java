package com.github.xiaoymin.knife4j.spring.configuration;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link Knife4jSetting}.
 */
@Generated
public class Knife4jSetting__BeanDefinitions {
  /**
   * Get the bean definition for 'knife4jSetting'.
   */
  public static BeanDefinition getKnifejSettingBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Knife4jSetting.class);
    beanDefinition.setInstanceSupplier(Knife4jSetting::new);
    return beanDefinition;
  }
}
