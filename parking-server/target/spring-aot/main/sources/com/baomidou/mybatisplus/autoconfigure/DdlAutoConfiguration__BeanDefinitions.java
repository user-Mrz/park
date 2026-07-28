package com.baomidou.mybatisplus.autoconfigure;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link DdlAutoConfiguration}.
 */
@Generated
public class DdlAutoConfiguration__BeanDefinitions {
  /**
   * Get the bean definition for 'ddlAutoConfiguration'.
   */
  public static BeanDefinition getDdlAutoConfigurationBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(DdlAutoConfiguration.class);
    beanDefinition.setInstanceSupplier(DdlAutoConfiguration::new);
    return beanDefinition;
  }
}
