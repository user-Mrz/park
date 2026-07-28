package com.parking.common.interceptor;

import com.parking.util.JwtUtil;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link JwtInterceptor}.
 */
@Generated
public class JwtInterceptor__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'jwtInterceptor'.
   */
  private static BeanInstanceSupplier<JwtInterceptor> getJwtInterceptorInstanceSupplier() {
    return BeanInstanceSupplier.<JwtInterceptor>forConstructor(JwtUtil.class)
            .withGenerator((registeredBean, args) -> new JwtInterceptor(args.get(0)));
  }

  /**
   * Get the bean definition for 'jwtInterceptor'.
   */
  public static BeanDefinition getJwtInterceptorBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(JwtInterceptor.class);
    beanDefinition.setInstanceSupplier(getJwtInterceptorInstanceSupplier());
    return beanDefinition;
  }
}
