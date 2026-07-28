package com.parking.util;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.AutowiredFieldValueResolver;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Autowiring for {@link JwtUtil}.
 */
@Generated
public class JwtUtil__Autowiring {
  /**
   * Apply the autowiring.
   */
  public static JwtUtil apply(RegisteredBean registeredBean, JwtUtil instance) {
    AutowiredFieldValueResolver.forRequiredField("secret").resolveAndSet(registeredBean, instance);
    AutowiredFieldValueResolver.forRequiredField("expiration").resolveAndSet(registeredBean, instance);
    AutowiredFieldValueResolver.forRequiredField("adminExpiration").resolveAndSet(registeredBean, instance);
    return instance;
  }
}
