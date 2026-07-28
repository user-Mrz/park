package com.parking.service.impl;

import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.PaymentRecordMapper;
import com.parking.mapper.RechargeRecordMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.SystemConfigMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link AdminServiceImpl}.
 */
@Generated
public class AdminServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'adminServiceImpl'.
   */
  private static BeanInstanceSupplier<AdminServiceImpl> getAdminServiceImplInstanceSupplier() {
    return BeanInstanceSupplier.<AdminServiceImpl>forConstructor(UserMapper.class, ReservationMapper.class, VehicleRecordMapper.class, RechargeRecordMapper.class, PaymentRecordMapper.class, ParkingSpotMapper.class, SystemConfigMapper.class)
            .withGenerator((registeredBean, args) -> new AdminServiceImpl(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4), args.get(5), args.get(6)));
  }

  /**
   * Get the bean definition for 'adminServiceImpl'.
   */
  public static BeanDefinition getAdminServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(AdminServiceImpl.class);
    beanDefinition.setInstanceSupplier(getAdminServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
