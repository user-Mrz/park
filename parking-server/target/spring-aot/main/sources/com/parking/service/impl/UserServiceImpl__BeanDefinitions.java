package com.parking.service.impl;

import com.parking.mapper.PaymentRecordMapper;
import com.parking.mapper.RechargeRecordMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link UserServiceImpl}.
 */
@Generated
public class UserServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'userServiceImpl'.
   */
  private static BeanInstanceSupplier<UserServiceImpl> getUserServiceImplInstanceSupplier() {
    return BeanInstanceSupplier.<UserServiceImpl>forConstructor(UserMapper.class, RechargeRecordMapper.class, PaymentRecordMapper.class, ReservationMapper.class, VehicleRecordMapper.class)
            .withGenerator((registeredBean, args) -> new UserServiceImpl(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4)));
  }

  /**
   * Get the bean definition for 'userServiceImpl'.
   */
  public static BeanDefinition getUserServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(UserServiceImpl.class);
    beanDefinition.setInstanceSupplier(getUserServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
