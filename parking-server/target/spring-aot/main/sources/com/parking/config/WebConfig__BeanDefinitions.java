package com.parking.config;

import com.parking.common.interceptor.JwtInterceptor;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassUtils;

/**
 * Bean definitions for {@link WebConfig}.
 */
@Generated
public class WebConfig__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'webConfig'.
   */
  private static BeanInstanceSupplier<WebConfig> getWebConfigInstanceSupplier() {
    return BeanInstanceSupplier.<WebConfig>forConstructor(JwtInterceptor.class)
            .withGenerator((registeredBean, args) -> new WebConfig$$SpringCGLIB$$0(args.get(0)));
  }

  /**
   * Get the bean definition for 'webConfig'.
   */
  public static BeanDefinition getWebConfigBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(WebConfig.class);
    beanDefinition.setTargetType(WebConfig.class);
    ConfigurationClassUtils.initializeConfigurationClass(WebConfig.class);
    beanDefinition.setInstanceSupplier(getWebConfigInstanceSupplier());
    return beanDefinition;
  }
}
