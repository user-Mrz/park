package com.github.xiaoymin.knife4j.spring.configuration;

import com.github.xiaoymin.knife4j.spring.extension.Knife4jJakartaOperationCustomizer;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassUtils;
import org.springframework.core.env.Environment;

/**
 * Bean definitions for {@link Knife4jAutoConfiguration}.
 */
@Generated
public class Knife4jAutoConfiguration__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration'.
   */
  private static BeanInstanceSupplier<Knife4jAutoConfiguration> getKnifejAutoConfigurationInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<Knife4jAutoConfiguration>forConstructor(Knife4jProperties.class, Environment.class)
            .withGenerator((registeredBean, args) -> new Knife4jAutoConfiguration$$SpringCGLIB$$0(args.get(0), args.get(1)));
  }

  /**
   * Get the bean definition for 'knife4jAutoConfiguration'.
   */
  public static BeanDefinition getKnifejAutoConfigurationBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Knife4jAutoConfiguration.class);
    beanDefinition.setTargetType(Knife4jAutoConfiguration.class);
    ConfigurationClassUtils.initializeConfigurationClass(Knife4jAutoConfiguration.class);
    beanDefinition.setInstanceSupplier(getKnifejAutoConfigurationInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'knife4jOpenApiCustomizer'.
   */
  private static BeanInstanceSupplier<Knife4jOpenApiCustomizer> getKnifejOpenApiCustomizerInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<Knife4jOpenApiCustomizer>forFactoryMethod(Knife4jAutoConfiguration.class, "knife4jOpenApiCustomizer", SpringDocConfigProperties.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(Knife4jAutoConfiguration.class).knife4jOpenApiCustomizer(args.get(0)));
  }

  /**
   * Get the bean definition for 'knife4jOpenApiCustomizer'.
   */
  public static BeanDefinition getKnifejOpenApiCustomizerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Knife4jOpenApiCustomizer.class);
    beanDefinition.setInstanceSupplier(getKnifejOpenApiCustomizerInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'knife4jJakartaOperationCustomizer'.
   */
  private static BeanInstanceSupplier<Knife4jJakartaOperationCustomizer> getKnifejJakartaOperationCustomizerInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<Knife4jJakartaOperationCustomizer>forFactoryMethod(Knife4jAutoConfiguration.class, "knife4jJakartaOperationCustomizer")
            .withGenerator((registeredBean) -> registeredBean.getBeanFactory().getBean(Knife4jAutoConfiguration.class).knife4jJakartaOperationCustomizer());
  }

  /**
   * Get the bean definition for 'knife4jJakartaOperationCustomizer'.
   */
  public static BeanDefinition getKnifejJakartaOperationCustomizerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Knife4jJakartaOperationCustomizer.class);
    beanDefinition.setInstanceSupplier(getKnifejJakartaOperationCustomizerInstanceSupplier());
    return beanDefinition;
  }
}
