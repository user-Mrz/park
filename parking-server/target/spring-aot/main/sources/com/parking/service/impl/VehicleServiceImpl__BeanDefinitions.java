package com.parking.service.impl;

import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.PaymentRecordMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.SystemConfigMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link VehicleServiceImpl}.
 */
@Generated
public class VehicleServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'vehicleServiceImpl'.
   */
  private static BeanInstanceSupplier<VehicleServiceImpl> getVehicleServiceImplInstanceSupplier() {
    return BeanInstanceSupplier.<VehicleServiceImpl>forConstructor(VehicleRecordMapper.class, ParkingSpotMapper.class, UserMapper.class, PaymentRecordMapper.class, SystemConfigMapper.class, ReservationMapper.class)
            .withGenerator((registeredBean, args) -> new VehicleServiceImpl(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4), args.get(5)));
  }

  /**
   * Get the bean definition for 'vehicleServiceImpl'.
   */
  public static BeanDefinition getVehicleServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(VehicleServiceImpl.class);
    beanDefinition.setInstanceSupplier(getVehicleServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
