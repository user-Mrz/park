package com.parking.service.impl;

import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link SpotServiceImpl}.
 */
@Generated
public class SpotServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'spotServiceImpl'.
   */
  private static BeanInstanceSupplier<SpotServiceImpl> getSpotServiceImplInstanceSupplier() {
    return BeanInstanceSupplier.<SpotServiceImpl>forConstructor(ParkingSpotMapper.class, VehicleRecordMapper.class, ReservationMapper.class, UserMapper.class)
            .withGenerator((registeredBean, args) -> new SpotServiceImpl(args.get(0), args.get(1), args.get(2), args.get(3)));
  }

  /**
   * Get the bean definition for 'spotServiceImpl'.
   */
  public static BeanDefinition getSpotServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SpotServiceImpl.class);
    beanDefinition.setInstanceSupplier(getSpotServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
