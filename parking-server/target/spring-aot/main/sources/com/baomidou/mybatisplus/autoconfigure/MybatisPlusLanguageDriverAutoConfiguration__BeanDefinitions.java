package com.baomidou.mybatisplus.autoconfigure;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link MybatisPlusLanguageDriverAutoConfiguration}.
 */
@Generated
public class MybatisPlusLanguageDriverAutoConfiguration__BeanDefinitions {
  /**
   * Get the bean definition for 'mybatisPlusLanguageDriverAutoConfiguration'.
   */
  public static BeanDefinition getMybatisPlusLanguageDriverAutoConfigurationBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MybatisPlusLanguageDriverAutoConfiguration.class);
    beanDefinition.setLazyInit(true);
    beanDefinition.setInstanceSupplier(MybatisPlusLanguageDriverAutoConfiguration::new);
    return beanDefinition;
  }
}
