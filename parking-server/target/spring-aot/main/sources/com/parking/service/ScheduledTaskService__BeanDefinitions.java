package com.parking.service;

import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.ReservationMapper;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link ScheduledTaskService}.
 */
@Generated
public class ScheduledTaskService__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'scheduledTaskService'.
   */
  private static BeanInstanceSupplier<ScheduledTaskService> getScheduledTaskServiceInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<ScheduledTaskService>forConstructor(ReservationMapper.class, ParkingSpotMapper.class)
            .withGenerator((registeredBean, args) -> new ScheduledTaskService(args.get(0), args.get(1)));
  }

  /**
   * Get the bean definition for 'scheduledTaskService'.
   */
  public static BeanDefinition getScheduledTaskServiceBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(ScheduledTaskService.class);
    InstanceSupplier<ScheduledTaskService> instanceSupplier = getScheduledTaskServiceInstanceSupplier();
    instanceSupplier = instanceSupplier.andThen(ScheduledTaskService__Autowiring::apply);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    return beanDefinition;
  }
}
