package com.parking.controller;

import com.parking.service.AdminService;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link AdminController}.
 */
@Generated
public class AdminController__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'adminController'.
   */
  private static BeanInstanceSupplier<AdminController> getAdminControllerInstanceSupplier() {
    return BeanInstanceSupplier.<AdminController>forConstructor(AdminService.class)
            .withGenerator((registeredBean, args) -> new AdminController(args.get(0)));
  }

  /**
   * Get the bean definition for 'adminController'.
   */
  public static BeanDefinition getAdminControllerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(AdminController.class);
    beanDefinition.setInstanceSupplier(getAdminControllerInstanceSupplier());
    return beanDefinition;
  }
}
