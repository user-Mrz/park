package com.parking.service.impl;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Bean definitions for {@link CaptchaServiceImpl}.
 */
@Generated
public class CaptchaServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'captchaServiceImpl'.
   */
  private static BeanInstanceSupplier<CaptchaServiceImpl> getCaptchaServiceImplInstanceSupplier() {
    return BeanInstanceSupplier.<CaptchaServiceImpl>forConstructor(StringRedisTemplate.class)
            .withGenerator((registeredBean, args) -> new CaptchaServiceImpl(args.get(0)));
  }

  /**
   * Get the bean definition for 'captchaServiceImpl'.
   */
  public static BeanDefinition getCaptchaServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(CaptchaServiceImpl.class);
    beanDefinition.setInstanceSupplier(getCaptchaServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
