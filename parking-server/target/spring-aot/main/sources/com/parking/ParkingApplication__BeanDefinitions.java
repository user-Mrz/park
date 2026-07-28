package com.parking;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassUtils;

/**
 * Bean definitions for {@link ParkingApplication}.
 */
@Generated
public class ParkingApplication__BeanDefinitions {
  /**
   * Get the bean definition for 'parkingApplication'.
   */
  public static BeanDefinition getParkingApplicationBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(ParkingApplication.class);
    beanDefinition.setTargetType(ParkingApplication.class);
    ConfigurationClassUtils.initializeConfigurationClass(ParkingApplication.class);
    beanDefinition.setInstanceSupplier(ParkingApplication$$SpringCGLIB$$0::new);
    return beanDefinition;
  }
}
