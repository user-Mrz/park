package com.parking.util;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link JwtUtil}.
 */
@Generated
public class JwtUtil__BeanDefinitions {
  /**
   * Get the bean definition for 'jwtUtil'.
   */
  public static BeanDefinition getJwtUtilBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(JwtUtil.class);
    InstanceSupplier<JwtUtil> instanceSupplier = InstanceSupplier.using(JwtUtil::new);
    instanceSupplier = instanceSupplier.andThen(JwtUtil__Autowiring::apply);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    return beanDefinition;
  }
}
