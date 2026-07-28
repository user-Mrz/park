package cn.hutool.extra.spring;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link SpringUtil}.
 */
@Generated
public class SpringUtil__BeanDefinitions {
  /**
   * Get the bean definition for 'springUtil'.
   */
  public static BeanDefinition getSpringUtilBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SpringUtil.class);
    beanDefinition.setInstanceSupplier(SpringUtil::new);
    return beanDefinition;
  }
}
