package com.parking.controller;

import com.parking.service.SpotService;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link SpotController}.
 */
@Generated
public class SpotController__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'spotController'.
   */
  private static BeanInstanceSupplier<SpotController> getSpotControllerInstanceSupplier() {
    return BeanInstanceSupplier.<SpotController>forConstructor(SpotService.class)
            .withGenerator((registeredBean, args) -> new SpotController(args.get(0)));
  }

  /**
   * Get the bean definition for 'spotController'.
   */
  public static BeanDefinition getSpotControllerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SpotController.class);
    beanDefinition.setInstanceSupplier(getSpotControllerInstanceSupplier());
    return beanDefinition;
  }
}
