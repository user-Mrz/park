package com.parking.service.impl;

import com.parking.mapper.AdminMapper;
import com.parking.mapper.UserMapper;
import com.parking.service.CaptchaService;
import com.parking.util.JwtUtil;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean definitions for {@link AuthServiceImpl}.
 */
@Generated
public class AuthServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'authServiceImpl'.
   */
  private static BeanInstanceSupplier<AuthServiceImpl> getAuthServiceImplInstanceSupplier() {
    return BeanInstanceSupplier.<AuthServiceImpl>forConstructor(UserMapper.class, AdminMapper.class, PasswordEncoder.class, CaptchaService.class, JwtUtil.class)
            .withGenerator((registeredBean, args) -> new AuthServiceImpl(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4)));
  }

  /**
   * Get the bean definition for 'authServiceImpl'.
   */
  public static BeanDefinition getAuthServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(AuthServiceImpl.class);
    beanDefinition.setInstanceSupplier(getAuthServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
