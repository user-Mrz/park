package com.parking.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassUtils;

/**
 * Bean definitions for {@link MyBatisPlusConfig}.
 */
@Generated
public class MyBatisPlusConfig__BeanDefinitions {
  /**
   * Get the bean definition for 'myBatisPlusConfig'.
   */
  public static BeanDefinition getMyBatisPlusConfigBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MyBatisPlusConfig.class);
    beanDefinition.setTargetType(MyBatisPlusConfig.class);
    ConfigurationClassUtils.initializeConfigurationClass(MyBatisPlusConfig.class);
    beanDefinition.setInstanceSupplier(MyBatisPlusConfig$$SpringCGLIB$$0::new);
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'mybatisPlusInterceptor'.
   */
  private static BeanInstanceSupplier<MybatisPlusInterceptor> getMybatisPlusInterceptorInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<MybatisPlusInterceptor>forFactoryMethod(MyBatisPlusConfig.class, "mybatisPlusInterceptor")
            .withGenerator((registeredBean) -> registeredBean.getBeanFactory().getBean(MyBatisPlusConfig.class).mybatisPlusInterceptor());
  }

  /**
   * Get the bean definition for 'mybatisPlusInterceptor'.
   */
  public static BeanDefinition getMybatisPlusInterceptorBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MybatisPlusInterceptor.class);
    beanDefinition.setInstanceSupplier(getMybatisPlusInterceptorInstanceSupplier());
    return beanDefinition;
  }
}
