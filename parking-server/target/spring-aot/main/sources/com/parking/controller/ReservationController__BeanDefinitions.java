package com.parking.controller;

import com.parking.service.ReservationService;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link ReservationController}.
 */
@Generated
public class ReservationController__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'reservationController'.
   */
  private static BeanInstanceSupplier<ReservationController> getReservationControllerInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<ReservationController>forConstructor(ReservationService.class)
            .withGenerator((registeredBean, args) -> new ReservationController(args.get(0)));
  }

  /**
   * Get the bean definition for 'reservationController'.
   */
  public static BeanDefinition getReservationControllerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(ReservationController.class);
    beanDefinition.setInstanceSupplier(getReservationControllerInstanceSupplier());
    return beanDefinition;
  }
}
