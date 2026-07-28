package com.parking.service;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.AutowiredFieldValueResolver;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Autowiring for {@link ScheduledTaskService}.
 */
@Generated
public class ScheduledTaskService__Autowiring {
  /**
   * Apply the autowiring.
   */
  public static ScheduledTaskService apply(RegisteredBean registeredBean,
      ScheduledTaskService instance) {
    AutowiredFieldValueResolver.forRequiredField("gracePeriodMinutes").resolveAndSet(registeredBean, instance);
    return instance;
  }
}
