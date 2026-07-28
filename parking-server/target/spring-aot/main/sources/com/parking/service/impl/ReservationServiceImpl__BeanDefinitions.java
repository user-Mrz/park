package com.parking.service.impl;

import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.SystemConfigMapper;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link ReservationServiceImpl}.
 */
@Generated
public class ReservationServiceImpl__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'reservationServiceImpl'.
   */
  private static BeanInstanceSupplier<ReservationServiceImpl> getReservationServiceImplInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<ReservationServiceImpl>forConstructor(ReservationMapper.class, ParkingSpotMapper.class, SystemConfigMapper.class)
            .withGenerator((registeredBean, args) -> new ReservationServiceImpl(args.get(0), args.get(1), args.get(2)));
  }

  /**
   * Get the bean definition for 'reservationServiceImpl'.
   */
  public static BeanDefinition getReservationServiceImplBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(ReservationServiceImpl.class);
    beanDefinition.setInstanceSupplier(getReservationServiceImplInstanceSupplier());
    return beanDefinition;
  }
}
