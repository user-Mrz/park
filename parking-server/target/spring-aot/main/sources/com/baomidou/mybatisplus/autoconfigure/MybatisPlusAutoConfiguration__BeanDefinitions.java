package com.baomidou.mybatisplus.autoconfigure;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;

/**
 * Bean definitions for {@link MybatisPlusAutoConfiguration}.
 */
@Generated
public class MybatisPlusAutoConfiguration__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration'.
   */
  private static BeanInstanceSupplier<MybatisPlusAutoConfiguration> getMybatisPlusAutoConfigurationInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<MybatisPlusAutoConfiguration>forConstructor(MybatisPlusProperties.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, ResourceLoader.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, ApplicationContext.class)
            .withGenerator((registeredBean, args) -> new MybatisPlusAutoConfiguration(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4), args.get(5), args.get(6), args.get(7), args.get(8), args.get(9)));
  }

  /**
   * Get the bean definition for 'mybatisPlusAutoConfiguration'.
   */
  public static BeanDefinition getMybatisPlusAutoConfigurationBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MybatisPlusAutoConfiguration.class);
    beanDefinition.setInstanceSupplier(getMybatisPlusAutoConfigurationInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'sqlSessionFactory'.
   */
  private static BeanInstanceSupplier<SqlSessionFactory> getSqlSessionFactoryInstanceSupplier() {
    return BeanInstanceSupplier.<SqlSessionFactory>forFactoryMethod(MybatisPlusAutoConfiguration.class, "sqlSessionFactory", DataSource.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(MybatisPlusAutoConfiguration.class).sqlSessionFactory(args.get(0)));
  }

  /**
   * Get the bean definition for 'sqlSessionFactory'.
   */
  public static BeanDefinition getSqlSessionFactoryBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SqlSessionFactory.class);
    beanDefinition.setInstanceSupplier(getSqlSessionFactoryInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'sqlSessionTemplate'.
   */
  private static BeanInstanceSupplier<SqlSessionTemplate> getSqlSessionTemplateInstanceSupplier() {
    return BeanInstanceSupplier.<SqlSessionTemplate>forFactoryMethod(MybatisPlusAutoConfiguration.class, "sqlSessionTemplate", SqlSessionFactory.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(MybatisPlusAutoConfiguration.class).sqlSessionTemplate(args.get(0)));
  }

  /**
   * Get the bean definition for 'sqlSessionTemplate'.
   */
  public static BeanDefinition getSqlSessionTemplateBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SqlSessionTemplate.class);
    beanDefinition.setDependsOn("dataSourceScriptDatabaseInitializer");
    beanDefinition.setInstanceSupplier(getSqlSessionTemplateInstanceSupplier());
    return beanDefinition;
  }
}
