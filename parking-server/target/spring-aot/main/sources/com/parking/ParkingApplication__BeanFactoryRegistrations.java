package com.parking;

import cn.hutool.extra.spring.SpringUtil__BeanDefinitions;
import com.baomidou.mybatisplus.autoconfigure.DdlAutoConfiguration__BeanDefinitions;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration__BeanDefinitions;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration__BeanDefinitions;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties__BeanDefinitions;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration__BeanDefinitions;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jHttpBasic__BeanDefinitions;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties__BeanDefinitions;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting__BeanDefinitions;
import com.parking.common.GlobalExceptionHandler__BeanDefinitions;
import com.parking.common.interceptor.JwtInterceptor__BeanDefinitions;
import com.parking.config.MyBatisPlusConfig__BeanDefinitions;
import com.parking.config.RedisConfig__BeanDefinitions;
import com.parking.config.SecurityConfig__BeanDefinitions;
import com.parking.config.WebConfig__BeanDefinitions;
import com.parking.controller.AdminController__BeanDefinitions;
import com.parking.controller.AuthController__BeanDefinitions;
import com.parking.controller.ReservationController__BeanDefinitions;
import com.parking.controller.SpotController__BeanDefinitions;
import com.parking.controller.UserController__BeanDefinitions;
import com.parking.controller.VehicleController__BeanDefinitions;
import com.parking.mapper.AdminMapper;
import com.parking.mapper.ParkingSpotMapper;
import com.parking.mapper.PaymentRecordMapper;
import com.parking.mapper.RechargeRecordMapper;
import com.parking.mapper.ReservationMapper;
import com.parking.mapper.SystemConfigMapper;
import com.parking.mapper.UserMapper;
import com.parking.mapper.VehicleRecordMapper;
import com.parking.service.ScheduledTaskService__BeanDefinitions;
import com.parking.service.impl.AdminServiceImpl__BeanDefinitions;
import com.parking.service.impl.AuthServiceImpl__BeanDefinitions;
import com.parking.service.impl.CaptchaServiceImpl__BeanDefinitions;
import com.parking.service.impl.ReservationServiceImpl__BeanDefinitions;
import com.parking.service.impl.SpotServiceImpl__BeanDefinitions;
import com.parking.service.impl.UserServiceImpl__BeanDefinitions;
import com.parking.service.impl.VehicleServiceImpl__BeanDefinitions;
import com.parking.util.JwtUtil__BeanDefinitions;
import java.lang.Class;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer__BeanDefinitions;
import org.springdoc.core.configuration.SpringDocConfiguration__BeanDefinitions;
import org.springdoc.core.configuration.SpringDocPageableConfiguration__BeanDefinitions;
import org.springdoc.core.configuration.SpringDocSecurityConfiguration__BeanDefinitions;
import org.springdoc.core.configuration.SpringDocSortConfiguration__BeanDefinitions;
import org.springdoc.core.configuration.SpringDocUIConfiguration__BeanDefinitions;
import org.springdoc.core.properties.SpringDocConfigProperties__BeanDefinitions;
import org.springdoc.core.properties.SwaggerUiConfigParameters__BeanDefinitions;
import org.springdoc.core.properties.SwaggerUiConfigProperties__BeanDefinitions;
import org.springdoc.core.properties.SwaggerUiOAuthProperties__BeanDefinitions;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration__BeanDefinitions;
import org.springdoc.webmvc.ui.SwaggerConfig__BeanDefinitions;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator__BeanDefinitions;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages__BeanDefinitions;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.context.LifecycleProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.http.JacksonHttpMessageConvertersConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.DataSourceJmxConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.JdbcProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.NamedParameterJdbcTemplateConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.netty.NettyAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.netty.NettyProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.reactor.ReactorAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.reactor.ReactorProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.security.SecurityProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.security.servlet.SpringBootWebSecurityConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.sql.init.DataSourceInitializationConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.ssl.SslProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations__BeanDefinitions;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.task.TaskSchedulingConfigurations__BeanDefinitions;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.transaction.TransactionProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.ServerProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.WebProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties__BeanDefinitions;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration__BeanDefinitions;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration__BeanDefinitions;
import org.springframework.boot.context.properties.BoundConfigurationProperties__BeanDefinitions;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinder__BeanDefinitions;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor__BeanDefinitions;
import org.springframework.boot.jackson.JsonMixinModuleEntries__BeanDefinitions;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer__BeanDefinitions;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter__BeanDefinitions;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor__BeanDefinitions;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor__BeanDefinitions;
import org.springframework.context.event.DefaultEventListenerFactory__BeanDefinitions;
import org.springframework.context.event.EventListenerMethodProcessor__BeanDefinitions;
import org.springframework.core.ResolvableType;
import org.springframework.data.redis.core.RedisKeyValueAdapter__BeanDefinitions;
import org.springframework.data.redis.core.RedisKeyValueTemplate__BeanDefinitions;
import org.springframework.data.redis.core.convert.KeyspaceConfiguration__BeanDefinitions;
import org.springframework.data.redis.core.convert.MappingConfiguration__BeanDefinitions;
import org.springframework.data.redis.core.convert.MappingRedisConverter__BeanDefinitions;
import org.springframework.data.redis.core.convert.RedisCustomConversions__BeanDefinitions;
import org.springframework.data.redis.core.convert.ReferenceResolverImpl__BeanDefinitions;
import org.springframework.data.redis.core.index.IndexConfiguration__BeanDefinitions;
import org.springframework.data.redis.core.mapping.RedisMappingContext__BeanDefinitions;
import org.springframework.data.web.config.ProjectingArgumentResolverRegistrar__BeanDefinitions;
import org.springframework.data.web.config.SpringDataJacksonConfiguration__BeanDefinitions;
import org.springframework.data.web.config.SpringDataWebConfiguration__BeanDefinitions;
import org.springframework.data.web.config.SpringDataWebSettings__BeanDefinitions;
import org.springframework.scheduling.annotation.SchedulingConfiguration__BeanDefinitions;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration__BeanDefinitions;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration__BeanDefinitions;
import org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration__BeanDefinitions;
import org.springframework.security.config.annotation.web.configuration.WebMvcSecurityConfiguration__BeanDefinitions;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration__BeanDefinitions;
import org.springframework.security.web.access.HandlerMappingIntrospectorRequestTransformer__BeanDefinitions;
import org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration__BeanDefinitions;
import org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration__BeanDefinitions;

/**
 * Register bean definitions for the bean factory.
 */
@Generated
public class ParkingApplication__BeanFactoryRegistrations {
  /**
   * Get the bean instance supplier for 'adminMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getAdminMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'adminMapper'.
   */
  private static BeanDefinition getAdminMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.AdminMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", AdminMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getAdminMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'parkingSpotMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getParkingSpotMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'parkingSpotMapper'.
   */
  private static BeanDefinition getParkingSpotMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.ParkingSpotMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", ParkingSpotMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getParkingSpotMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'paymentRecordMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getPaymentRecordMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'paymentRecordMapper'.
   */
  private static BeanDefinition getPaymentRecordMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.PaymentRecordMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", PaymentRecordMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getPaymentRecordMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'rechargeRecordMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getRechargeRecordMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'rechargeRecordMapper'.
   */
  private static BeanDefinition getRechargeRecordMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.RechargeRecordMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", RechargeRecordMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getRechargeRecordMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'reservationMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getReservationMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'reservationMapper'.
   */
  private static BeanDefinition getReservationMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.ReservationMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", ReservationMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getReservationMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'systemConfigMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getSystemConfigMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'systemConfigMapper'.
   */
  private static BeanDefinition getSystemConfigMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.SystemConfigMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", SystemConfigMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getSystemConfigMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'userMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getUserMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'userMapper'.
   */
  private static BeanDefinition getUserMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.UserMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", UserMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getUserMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'vehicleRecordMapper'.
   */
  private static BeanInstanceSupplier<MapperFactoryBean> getVehicleRecordMapperInstanceSupplier() {
    return BeanInstanceSupplier.<MapperFactoryBean>forConstructor(Class.class)
            .withGenerator((registeredBean, args) -> new MapperFactoryBean(args.get(0)));
  }

  /**
   * Get the bean definition for 'vehicleRecordMapper'.
   */
  private static BeanDefinition getVehicleRecordMapperBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MapperFactoryBean.class);
    beanDefinition.setTargetType(ResolvableType.forClass(MapperFactoryBean.class));
    beanDefinition.setLazyInit(false);
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("com.parking.mapper.VehicleRecordMapper");
    beanDefinition.getPropertyValues().addPropertyValue("mapperInterface", VehicleRecordMapper.class);
    beanDefinition.getPropertyValues().addPropertyValue("addToConfig", true);
    beanDefinition.setInstanceSupplier(getVehicleRecordMapperInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Register the bean definitions.
   */
  public void registerBeanDefinitions(DefaultListableBeanFactory beanFactory) {
    beanFactory.registerBeanDefinition("org.springframework.context.event.internalEventListenerProcessor", EventListenerMethodProcessor__BeanDefinitions.getInternalEventListenerProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.context.event.internalEventListenerFactory", DefaultEventListenerFactory__BeanDefinitions.getInternalEventListenerFactoryBeanDefinition());
    beanFactory.registerBeanDefinition("parkingApplication", ParkingApplication__BeanDefinitions.getParkingApplicationBeanDefinition());
    beanFactory.registerBeanDefinition("globalExceptionHandler", GlobalExceptionHandler__BeanDefinitions.getGlobalExceptionHandlerBeanDefinition());
    beanFactory.registerBeanDefinition("jwtInterceptor", JwtInterceptor__BeanDefinitions.getJwtInterceptorBeanDefinition());
    beanFactory.registerBeanDefinition("myBatisPlusConfig", MyBatisPlusConfig__BeanDefinitions.getMyBatisPlusConfigBeanDefinition());
    beanFactory.registerBeanDefinition("redisConfig", RedisConfig__BeanDefinitions.getRedisConfigBeanDefinition());
    beanFactory.registerBeanDefinition("securityConfig", SecurityConfig__BeanDefinitions.getSecurityConfigBeanDefinition());
    beanFactory.registerBeanDefinition("webConfig", WebConfig__BeanDefinitions.getWebConfigBeanDefinition());
    beanFactory.registerBeanDefinition("adminController", AdminController__BeanDefinitions.getAdminControllerBeanDefinition());
    beanFactory.registerBeanDefinition("authController", AuthController__BeanDefinitions.getAuthControllerBeanDefinition());
    beanFactory.registerBeanDefinition("reservationController", ReservationController__BeanDefinitions.getReservationControllerBeanDefinition());
    beanFactory.registerBeanDefinition("spotController", SpotController__BeanDefinitions.getSpotControllerBeanDefinition());
    beanFactory.registerBeanDefinition("userController", UserController__BeanDefinitions.getUserControllerBeanDefinition());
    beanFactory.registerBeanDefinition("vehicleController", VehicleController__BeanDefinitions.getVehicleControllerBeanDefinition());
    beanFactory.registerBeanDefinition("adminServiceImpl", AdminServiceImpl__BeanDefinitions.getAdminServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("authServiceImpl", AuthServiceImpl__BeanDefinitions.getAuthServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("captchaServiceImpl", CaptchaServiceImpl__BeanDefinitions.getCaptchaServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("reservationServiceImpl", ReservationServiceImpl__BeanDefinitions.getReservationServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("spotServiceImpl", SpotServiceImpl__BeanDefinitions.getSpotServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("userServiceImpl", UserServiceImpl__BeanDefinitions.getUserServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("vehicleServiceImpl", VehicleServiceImpl__BeanDefinitions.getVehicleServiceImplBeanDefinition());
    beanFactory.registerBeanDefinition("scheduledTaskService", ScheduledTaskService__BeanDefinitions.getScheduledTaskServiceBeanDefinition());
    beanFactory.registerBeanDefinition("jwtUtil", JwtUtil__BeanDefinitions.getJwtUtilBeanDefinition());
    beanFactory.registerBeanDefinition("mybatisPlusInterceptor", MyBatisPlusConfig__BeanDefinitions.getMybatisPlusInterceptorBeanDefinition());
    beanFactory.registerBeanDefinition("redisTemplate", RedisConfig__BeanDefinitions.getRedisTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("stringRedisTemplate", RedisConfig__BeanDefinitions.getStringRedisTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration", ObjectPostProcessorConfiguration__BeanDefinitions.getObjectPostProcessorConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("objectPostProcessor", ObjectPostProcessorConfiguration__BeanDefinitions.getObjectPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration", AuthenticationConfiguration__BeanDefinitions.getAuthenticationConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("authenticationManagerBuilder", AuthenticationConfiguration__BeanDefinitions.getAuthenticationManagerBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("enableGlobalAuthenticationAutowiredConfigurer", AuthenticationConfiguration__BeanDefinitions.getEnableGlobalAuthenticationAutowiredConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("initializeUserDetailsBeanManagerConfigurer", AuthenticationConfiguration__BeanDefinitions.getInitializeUserDetailsBeanManagerConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("initializeAuthenticationProviderBeanManagerConfigurer", AuthenticationConfiguration__BeanDefinitions.getInitializeAuthenticationProviderBeanManagerConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration", WebSecurityConfiguration__BeanDefinitions.getWebSecurityConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("delegatingApplicationListener", WebSecurityConfiguration__BeanDefinitions.getDelegatingApplicationListenerBeanDefinition());
    beanFactory.registerBeanDefinition("webSecurityExpressionHandler", WebSecurityConfiguration__BeanDefinitions.getWebSecurityExpressionHandlerBeanDefinition());
    beanFactory.registerBeanDefinition("privilegeEvaluator", WebSecurityConfiguration__BeanDefinitions.getPrivilegeEvaluatorBeanDefinition());
    beanFactory.registerBeanDefinition("conversionServicePostProcessor", WebSecurityConfiguration__BeanDefinitions.getConversionServicePostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.security.config.annotation.web.configuration.WebMvcSecurityConfiguration", WebMvcSecurityConfiguration__BeanDefinitions.getWebMvcSecurityConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("requestDataValueProcessor", WebMvcSecurityConfiguration__BeanDefinitions.getRequestDataValueProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("springSecurityHandlerMappingIntrospectorBeanDefinitionRegistryPostProcessor", WebMvcSecurityConfiguration__BeanDefinitions.getSpringSecurityHandlerMappingIntrospectorBeanDefinitionRegistryPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration", HttpSecurityConfiguration__BeanDefinitions.getHttpSecurityConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity", HttpSecurityConfiguration__BeanDefinitions.getHttpSecurityBeanDefinition());
    beanFactory.registerBeanDefinition("passwordEncoder", SecurityConfig__BeanDefinitions.getPasswordEncoderBeanDefinition());
    beanFactory.registerBeanDefinition("filterChain", SecurityConfig__BeanDefinitions.getFilterChainBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.scheduling.annotation.SchedulingConfiguration", SchedulingConfiguration__BeanDefinitions.getSchedulingConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.context.annotation.internalScheduledAnnotationProcessor", SchedulingConfiguration__BeanDefinitions.getInternalScheduledAnnotationProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.AutoConfigurationPackages", AutoConfigurationPackages__BeanDefinitions.BasePackages.getAutoConfigurationPackagesBeanDefinition());
    beanFactory.registerBeanDefinition("com.parking.ParkingApplication#MapperScannerRegistrar#0", MapperScannerConfigurer__BeanDefinitions.getParkingApplicationMapperScannerRegistrarBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration", PropertyPlaceholderAutoConfiguration__BeanDefinitions.getPropertyPlaceholderAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("propertySourcesPlaceholderConfigurer", PropertyPlaceholderAutoConfiguration__BeanDefinitions.getPropertySourcesPlaceholderConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration", SslAutoConfiguration__BeanDefinitions.getSslAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("fileWatcher", SslAutoConfiguration__BeanDefinitions.getFileWatcherBeanDefinition());
    beanFactory.registerBeanDefinition("sslPropertiesSslBundleRegistrar", SslAutoConfiguration__BeanDefinitions.getSslPropertiesSslBundleRegistrarBeanDefinition());
    beanFactory.registerBeanDefinition("sslBundleRegistry", SslAutoConfiguration__BeanDefinitions.getSslBundleRegistryBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor", ConfigurationPropertiesBindingPostProcessor__BeanDefinitions.getConfigurationPropertiesBindingPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.context.internalConfigurationPropertiesBinder", ConfigurationPropertiesBinder__BeanDefinitions.ConfigurationPropertiesBinderFactory.getInternalConfigurationPropertiesBinderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.context.properties.BoundConfigurationProperties", BoundConfigurationProperties__BeanDefinitions.getBoundConfigurationPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.context.properties.EnableConfigurationPropertiesRegistrar.methodValidationExcludeFilter", MethodValidationExcludeFilter__BeanDefinitions.getMethodValidationExcludeFilterBeanDefinition());
    beanFactory.registerBeanDefinition("spring.ssl-org.springframework.boot.autoconfigure.ssl.SslProperties", SslProperties__BeanDefinitions.getSslPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration$TomcatWebSocketConfiguration", WebSocketServletAutoConfiguration__BeanDefinitions.TomcatWebSocketConfiguration.getTomcatWebSocketConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("websocketServletWebServerCustomizer", WebSocketServletAutoConfiguration__BeanDefinitions.TomcatWebSocketConfiguration.getWebsocketServletWebServerCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration", WebSocketServletAutoConfiguration__BeanDefinitions.getWebSocketServletAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryConfiguration$EmbeddedTomcat", ServletWebServerFactoryConfiguration__BeanDefinitions.EmbeddedTomcat.getEmbeddedTomcatBeanDefinition());
    beanFactory.registerBeanDefinition("tomcatServletWebServerFactory", ServletWebServerFactoryConfiguration__BeanDefinitions.EmbeddedTomcat.getTomcatServletWebServerFactoryBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration", ServletWebServerFactoryAutoConfiguration__BeanDefinitions.getServletWebServerFactoryAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("servletWebServerFactoryCustomizer", ServletWebServerFactoryAutoConfiguration__BeanDefinitions.getServletWebServerFactoryCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("tomcatServletWebServerFactoryCustomizer", ServletWebServerFactoryAutoConfiguration__BeanDefinitions.getTomcatServletWebServerFactoryCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("server-org.springframework.boot.autoconfigure.web.ServerProperties", ServerProperties__BeanDefinitions.getServerPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("webServerFactoryCustomizerBeanPostProcessor", WebServerFactoryCustomizerBeanPostProcessor__BeanDefinitions.getWebServerFactoryCustomizerBeanPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("errorPageRegistrarBeanPostProcessor", ErrorPageRegistrarBeanPostProcessor__BeanDefinitions.getErrorPageRegistrarBeanPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletConfiguration", DispatcherServletAutoConfiguration__BeanDefinitions.DispatcherServletConfiguration.getDispatcherServletConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("dispatcherServlet", DispatcherServletAutoConfiguration__BeanDefinitions.DispatcherServletConfiguration.getDispatcherServletBeanDefinition());
    beanFactory.registerBeanDefinition("spring.mvc-org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties", WebMvcProperties__BeanDefinitions.getWebMvcPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletRegistrationConfiguration", DispatcherServletAutoConfiguration__BeanDefinitions.DispatcherServletRegistrationConfiguration.getDispatcherServletRegistrationConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("dispatcherServletRegistration", DispatcherServletAutoConfiguration__BeanDefinitions.DispatcherServletRegistrationConfiguration.getDispatcherServletRegistrationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration", DispatcherServletAutoConfiguration__BeanDefinitions.getDispatcherServletAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations$ThreadPoolTaskExecutorBuilderConfiguration", TaskExecutorConfigurations__BeanDefinitions.ThreadPoolTaskExecutorBuilderConfiguration.getThreadPoolTaskExecutorBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("threadPoolTaskExecutorBuilder", TaskExecutorConfigurations__BeanDefinitions.ThreadPoolTaskExecutorBuilderConfiguration.getThreadPoolTaskExecutorBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations$TaskExecutorBuilderConfiguration", TaskExecutorConfigurations__BeanDefinitions.TaskExecutorBuilderConfiguration.getTaskExecutorBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("taskExecutorBuilder", TaskExecutorConfigurations__BeanDefinitions.TaskExecutorBuilderConfiguration.getTaskExecutorBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations$SimpleAsyncTaskExecutorBuilderConfiguration", TaskExecutorConfigurations__BeanDefinitions.SimpleAsyncTaskExecutorBuilderConfiguration.getSimpleAsyncTaskExecutorBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("simpleAsyncTaskExecutorBuilder", TaskExecutorConfigurations__BeanDefinitions.SimpleAsyncTaskExecutorBuilderConfiguration.getSimpleAsyncTaskExecutorBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations$TaskExecutorConfiguration", TaskExecutorConfigurations__BeanDefinitions.TaskExecutorConfiguration.getTaskExecutorConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("applicationTaskExecutor", TaskExecutorConfigurations__BeanDefinitions.TaskExecutorConfiguration.getApplicationTaskExecutorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration", TaskExecutionAutoConfiguration__BeanDefinitions.getTaskExecutionAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.task.execution-org.springframework.boot.autoconfigure.task.TaskExecutionProperties", TaskExecutionProperties__BeanDefinitions.getTaskExecutionPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration", ValidationAutoConfiguration__BeanDefinitions.getValidationAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("defaultValidator", ValidationAutoConfiguration__BeanDefinitions.getDefaultValidatorBeanDefinition());
    beanFactory.registerBeanDefinition("methodValidationPostProcessor", ValidationAutoConfiguration__BeanDefinitions.getMethodValidationPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration$WhitelabelErrorViewConfiguration", ErrorMvcAutoConfiguration__BeanDefinitions.WhitelabelErrorViewConfiguration.getWhitelabelErrorViewConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("error", ErrorMvcAutoConfiguration__BeanDefinitions.WhitelabelErrorViewConfiguration.getErrorBeanDefinition());
    beanFactory.registerBeanDefinition("beanNameViewResolver", ErrorMvcAutoConfiguration__BeanDefinitions.WhitelabelErrorViewConfiguration.getBeanNameViewResolverBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration$DefaultErrorViewResolverConfiguration", ErrorMvcAutoConfiguration__BeanDefinitions.DefaultErrorViewResolverConfiguration.getDefaultErrorViewResolverConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("conventionErrorViewResolver", ErrorMvcAutoConfiguration__BeanDefinitions.DefaultErrorViewResolverConfiguration.getConventionErrorViewResolverBeanDefinition());
    beanFactory.registerBeanDefinition("spring.web-org.springframework.boot.autoconfigure.web.WebProperties", WebProperties__BeanDefinitions.getWebPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration", ErrorMvcAutoConfiguration__BeanDefinitions.getErrorMvcAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("errorAttributes", ErrorMvcAutoConfiguration__BeanDefinitions.getErrorAttributesBeanDefinition());
    beanFactory.registerBeanDefinition("basicErrorController", ErrorMvcAutoConfiguration__BeanDefinitions.getBasicErrorControllerBeanDefinition());
    beanFactory.registerBeanDefinition("errorPageCustomizer", ErrorMvcAutoConfiguration__BeanDefinitions.getErrorPageCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("preserveErrorControllerTargetClassPostProcessor", ErrorMvcAutoConfiguration__BeanDefinitions.getPreserveErrorControllerTargetClassPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration$EnableWebMvcConfiguration", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getEnableWebMvcConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("welcomePageHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getWelcomePageHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("welcomePageNotAcceptableHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getWelcomePageNotAcceptableHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("localeResolver", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getLocaleResolverBeanDefinition());
    beanFactory.registerBeanDefinition("themeResolver", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getThemeResolverBeanDefinition());
    beanFactory.registerBeanDefinition("flashMapManager", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getFlashMapManagerBeanDefinition());
    beanFactory.registerBeanDefinition("mvcConversionService", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcConversionServiceBeanDefinition());
    beanFactory.registerBeanDefinition("mvcValidator", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcValidatorBeanDefinition());
    beanFactory.registerBeanDefinition("mvcContentNegotiationManager", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcContentNegotiationManagerBeanDefinition());
    beanFactory.registerBeanDefinition("requestMappingHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getRequestMappingHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("mvcPatternParser", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcPatternParserBeanDefinition());
    beanFactory.registerBeanDefinition("mvcUrlPathHelper", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcUrlPathHelperBeanDefinition());
    beanFactory.registerBeanDefinition("mvcPathMatcher", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcPathMatcherBeanDefinition());
    beanFactory.registerBeanDefinition("viewControllerHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getViewControllerHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("beanNameHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getBeanNameHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("routerFunctionMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getRouterFunctionMappingBeanDefinition());
    beanFactory.registerBeanDefinition("resourceHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getResourceHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("mvcResourceUrlProvider", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcResourceUrlProviderBeanDefinition());
    beanFactory.registerBeanDefinition("defaultServletHandlerMapping", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getDefaultServletHandlerMappingBeanDefinition());
    beanFactory.registerBeanDefinition("requestMappingHandlerAdapter", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getRequestMappingHandlerAdapterBeanDefinition());
    beanFactory.registerBeanDefinition("handlerFunctionAdapter", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getHandlerFunctionAdapterBeanDefinition());
    beanFactory.registerBeanDefinition("mvcUriComponentsContributor", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcUriComponentsContributorBeanDefinition());
    beanFactory.registerBeanDefinition("httpRequestHandlerAdapter", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getHttpRequestHandlerAdapterBeanDefinition());
    beanFactory.registerBeanDefinition("simpleControllerHandlerAdapter", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getSimpleControllerHandlerAdapterBeanDefinition());
    beanFactory.registerBeanDefinition("handlerExceptionResolver", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getHandlerExceptionResolverBeanDefinition());
    beanFactory.registerBeanDefinition("mvcViewResolver", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcViewResolverBeanDefinition());
    beanFactory.registerBeanDefinition("mvcHandlerMappingIntrospector", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getMvcHandlerMappingIntrospectorBeanDefinition());
    beanFactory.registerBeanDefinition("viewNameTranslator", WebMvcAutoConfiguration__BeanDefinitions.EnableWebMvcConfiguration.getViewNameTranslatorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration$WebMvcAutoConfigurationAdapter", WebMvcAutoConfiguration__BeanDefinitions.WebMvcAutoConfigurationAdapter.getWebMvcAutoConfigurationAdapterBeanDefinition());
    beanFactory.registerBeanDefinition("defaultViewResolver", WebMvcAutoConfiguration__BeanDefinitions.WebMvcAutoConfigurationAdapter.getDefaultViewResolverBeanDefinition());
    beanFactory.registerBeanDefinition("viewResolver", WebMvcAutoConfiguration__BeanDefinitions.WebMvcAutoConfigurationAdapter.getViewResolverBeanDefinition());
    beanFactory.registerBeanDefinition("requestContextFilter", WebMvcAutoConfiguration__BeanDefinitions.WebMvcAutoConfigurationAdapter.getRequestContextFilterBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration", WebMvcAutoConfiguration__BeanDefinitions.getWebMvcAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("formContentFilter", WebMvcAutoConfiguration__BeanDefinitions.getFormContentFilterBeanDefinition());
    beanFactory.registerBeanDefinition("cn.hutool.extra.spring.SpringUtil", SpringUtil__BeanDefinitions.getSpringUtilBeanDefinition());
    beanFactory.registerBeanDefinition("com.baomidou.mybatisplus.autoconfigure.DdlAutoConfiguration", DdlAutoConfiguration__BeanDefinitions.getDdlAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration", MybatisPlusLanguageDriverAutoConfiguration__BeanDefinitions.getMybatisPlusLanguageDriverAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari", DataSourceConfiguration__BeanDefinitions.Hikari.getHikariBeanDefinition());
    beanFactory.registerBeanDefinition("jdbcConnectionDetailsHikariBeanPostProcessor", DataSourceConfiguration__BeanDefinitions.Hikari.getJdbcConnectionDetailsHikariBeanPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("dataSource", DataSourceConfiguration__BeanDefinitions.Hikari.getDataSourceBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceJmxConfiguration$Hikari", DataSourceJmxConfiguration__BeanDefinitions.Hikari.getHikariBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceJmxConfiguration", DataSourceJmxConfiguration__BeanDefinitions.getDataSourceJmxConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration", DataSourceAutoConfiguration__BeanDefinitions.PooledDataSourceConfiguration.getPooledDataSourceConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jdbcConnectionDetails", DataSourceAutoConfiguration__BeanDefinitions.PooledDataSourceConfiguration.getJdbcConnectionDetailsBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration$HikariPoolDataSourceMetadataProviderConfiguration", DataSourcePoolMetadataProvidersConfiguration__BeanDefinitions.HikariPoolDataSourceMetadataProviderConfiguration.getHikariPoolDataSourceMetadataProviderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("hikariPoolDataSourceMetadataProvider", DataSourcePoolMetadataProvidersConfiguration__BeanDefinitions.HikariPoolDataSourceMetadataProviderConfiguration.getHikariPoolDataSourceMetadataProviderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration", DataSourcePoolMetadataProvidersConfiguration__BeanDefinitions.getDataSourcePoolMetadataProvidersConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration", DataSourceAutoConfiguration__BeanDefinitions.getDataSourceAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.datasource-org.springframework.boot.autoconfigure.jdbc.DataSourceProperties", DataSourceProperties__BeanDefinitions.getDataSourcePropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration", MybatisPlusAutoConfiguration__BeanDefinitions.getMybatisPlusAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("sqlSessionFactory", MybatisPlusAutoConfiguration__BeanDefinitions.getSqlSessionFactoryBeanDefinition());
    beanFactory.registerBeanDefinition("sqlSessionTemplate", MybatisPlusAutoConfiguration__BeanDefinitions.getSqlSessionTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("mybatis-plus-com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties", MybatisPlusProperties__BeanDefinitions.getMybatisPlusPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration", Knife4jAutoConfiguration__BeanDefinitions.getKnifejAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("knife4jOpenApiCustomizer", Knife4jAutoConfiguration__BeanDefinitions.getKnifejOpenApiCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("knife4jJakartaOperationCustomizer", Knife4jAutoConfiguration__BeanDefinitions.getKnifejJakartaOperationCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("knife4j.setting-com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting", Knife4jSetting__BeanDefinitions.getKnifejSettingBeanDefinition());
    beanFactory.registerBeanDefinition("knife4j.basic-com.github.xiaoymin.knife4j.spring.configuration.Knife4jHttpBasic", Knife4jHttpBasic__BeanDefinitions.getKnifejHttpBasicBeanDefinition());
    beanFactory.registerBeanDefinition("knife4j-com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties", Knife4jProperties__BeanDefinitions.getKnifejPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocConfiguration$QuerydslProvider", SpringDocConfiguration__BeanDefinitions.QuerydslProvider.getQuerydslProviderBeanDefinition());
    beanFactory.registerBeanDefinition("queryDslQuerydslPredicateOperationCustomizer", SpringDocConfiguration__BeanDefinitions.QuerydslProvider.getQueryDslQuerydslPredicateOperationCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocConfiguration$OpenApiResourceAdvice", SpringDocConfiguration__BeanDefinitions.OpenApiResourceAdvice.getOpenApiResourceAdviceBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocConfiguration$SpringDocWebFluxSupportConfiguration", SpringDocConfiguration__BeanDefinitions.SpringDocWebFluxSupportConfiguration.getSpringDocWebFluxSupportConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("webFluxSupportConverter", SpringDocConfiguration__BeanDefinitions.SpringDocWebFluxSupportConfiguration.getWebFluxSupportConverterBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocConfiguration$SpringDocSpringDataWebPropertiesProvider", SpringDocConfiguration__BeanDefinitions.SpringDocSpringDataWebPropertiesProvider.getSpringDocSpringDataWebPropertiesProviderBeanDefinition());
    beanFactory.registerBeanDefinition("springDataWebPropertiesProvider", SpringDocConfiguration__BeanDefinitions.SpringDocSpringDataWebPropertiesProvider.getSpringDataWebPropertiesProviderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocConfiguration$WebConversionServiceConfiguration", SpringDocConfiguration__BeanDefinitions.WebConversionServiceConfiguration.getWebConversionServiceConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("webConversionServiceProvider", SpringDocConfiguration__BeanDefinitions.WebConversionServiceConfiguration.getWebConversionServiceProviderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocConfiguration", SpringDocConfiguration__BeanDefinitions.getSpringDocConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("localSpringDocParameterNameDiscoverer", SpringDocConfiguration__BeanDefinitions.getLocalSpringDocParameterNameDiscovererBeanDefinition());
    beanFactory.registerBeanDefinition("additionalModelsConverter", SpringDocConfiguration__BeanDefinitions.getAdditionalModelsConverterBeanDefinition());
    beanFactory.registerBeanDefinition("fileSupportConverter", SpringDocConfiguration__BeanDefinitions.getFileSupportConverterBeanDefinition());
    beanFactory.registerBeanDefinition("responseSupportConverter", SpringDocConfiguration__BeanDefinitions.getResponseSupportConverterBeanDefinition());
    beanFactory.registerBeanDefinition("schemaPropertyDeprecatingConverter", SpringDocConfiguration__BeanDefinitions.getSchemaPropertyDeprecatingConverterBeanDefinition());
    beanFactory.registerBeanDefinition("polymorphicModelConverter", SpringDocConfiguration__BeanDefinitions.getPolymorphicModelConverterBeanDefinition());
    beanFactory.registerBeanDefinition("openAPIBuilder", SpringDocConfiguration__BeanDefinitions.getOpenAPIBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("modelConverterRegistrar", SpringDocConfiguration__BeanDefinitions.getModelConverterRegistrarBeanDefinition());
    beanFactory.registerBeanDefinition("operationBuilder", SpringDocConfiguration__BeanDefinitions.getOperationBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("propertyResolverUtils", SpringDocConfiguration__BeanDefinitions.getPropertyResolverUtilsBeanDefinition());
    beanFactory.registerBeanDefinition("requestBodyBuilder", SpringDocConfiguration__BeanDefinitions.getRequestBodyBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("securityParser", SpringDocConfiguration__BeanDefinitions.getSecurityParserBeanDefinition());
    beanFactory.registerBeanDefinition("genericReturnTypeParser", SpringDocConfiguration__BeanDefinitions.getGenericReturnTypeParserBeanDefinition());
    beanFactory.registerBeanDefinition("parameterBuilder", SpringDocConfiguration__BeanDefinitions.getParameterBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("springDocProviders", SpringDocConfiguration__BeanDefinitions.getSpringDocProvidersBeanDefinition());
    beanFactory.registerBeanDefinition("springdocObjectMapperProvider", SpringDocConfiguration__BeanDefinitions.getSpringdocObjectMapperProviderBeanDefinition());
    beanFactory.registerBeanDefinition("springDocCustomizers", SpringDocConfiguration__BeanDefinitions.getSpringDocCustomizersBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocPageableConfiguration", SpringDocPageableConfiguration__BeanDefinitions.getSpringDocPageableConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("pageableOpenAPIConverter", SpringDocPageableConfiguration__BeanDefinitions.getPageableOpenAPIConverterBeanDefinition());
    beanFactory.registerBeanDefinition("delegatingMethodParameterCustomizer", SpringDocPageableConfiguration__BeanDefinitions.getDelegatingMethodParameterCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocSecurityConfiguration$SpringSecurityLoginEndpointConfiguration", SpringDocSecurityConfiguration__BeanDefinitions.SpringSecurityLoginEndpointConfiguration.getSpringSecurityLoginEndpointConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocSecurityConfiguration", SpringDocSecurityConfiguration__BeanDefinitions.getSpringDocSecurityConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocSortConfiguration", SpringDocSortConfiguration__BeanDefinitions.getSpringDocSortConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("sortOpenAPIConverter", SpringDocSortConfiguration__BeanDefinitions.getSortOpenAPIConverterBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.configuration.SpringDocUIConfiguration", SpringDocUIConfiguration__BeanDefinitions.getSpringDocUIConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.properties.SpringDocConfigProperties", SpringDocConfigProperties__BeanDefinitions.getSpringDocConfigPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.properties.SwaggerUiConfigParameters", SwaggerUiConfigParameters__BeanDefinitions.getSwaggerUiConfigParametersBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.properties.SwaggerUiConfigProperties", SwaggerUiConfigProperties__BeanDefinitions.getSwaggerUiConfigPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.core.properties.SwaggerUiOAuthProperties", SwaggerUiOAuthProperties__BeanDefinitions.getSwaggerUiOAuthPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration$SpringDocWebMvcRouterConfiguration", SpringDocWebMvcConfiguration__BeanDefinitions.SpringDocWebMvcRouterConfiguration.getSpringDocWebMvcRouterConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("routerFunctionProvider", SpringDocWebMvcConfiguration__BeanDefinitions.SpringDocWebMvcRouterConfiguration.getRouterFunctionProviderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration", SpringDocWebMvcConfiguration__BeanDefinitions.getSpringDocWebMvcConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("openApiResource", SpringDocWebMvcConfiguration__BeanDefinitions.getOpenApiResourceBeanDefinition());
    beanFactory.registerBeanDefinition("requestBuilder", SpringDocWebMvcConfiguration__BeanDefinitions.getRequestBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("springWebProvider", SpringDocWebMvcConfiguration__BeanDefinitions.getSpringWebProviderBeanDefinition());
    beanFactory.registerBeanDefinition("responseBuilder", SpringDocWebMvcConfiguration__BeanDefinitions.getResponseBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springdoc.webmvc.ui.SwaggerConfig", SwaggerConfig__BeanDefinitions.getSwaggerConfigBeanDefinition());
    beanFactory.registerBeanDefinition("swaggerWelcome", SwaggerConfig__BeanDefinitions.getSwaggerWelcomeBeanDefinition());
    beanFactory.registerBeanDefinition("swaggerConfigResource", SwaggerConfig__BeanDefinitions.getSwaggerConfigResourceBeanDefinition());
    beanFactory.registerBeanDefinition("indexPageTransformer", SwaggerConfig__BeanDefinitions.getIndexPageTransformerBeanDefinition());
    beanFactory.registerBeanDefinition("swaggerWebMvcConfigurer", SwaggerConfig__BeanDefinitions.getSwaggerWebMvcConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("swaggerResourceResolver", SwaggerConfig__BeanDefinitions.getSwaggerResourceResolverBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.aop.AopAutoConfiguration$ClassProxyingConfiguration", AopAutoConfiguration__BeanDefinitions.ClassProxyingConfiguration.getClassProxyingConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("forceAutoProxyCreatorToUseClassProxying", AopAutoConfiguration__BeanDefinitions.ClassProxyingConfiguration.getForceAutoProxyCreatorToUseClassProxyingBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.aop.AopAutoConfiguration", AopAutoConfiguration__BeanDefinitions.getAopAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration", ApplicationAvailabilityAutoConfiguration__BeanDefinitions.getApplicationAvailabilityAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("applicationAvailability", ApplicationAvailabilityAutoConfiguration__BeanDefinitions.getApplicationAvailabilityBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration", LettuceConnectionConfiguration__BeanDefinitions.getLettuceConnectionConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("lettuceClientResources", LettuceConnectionConfiguration__BeanDefinitions.getLettuceClientResourcesBeanDefinition());
    beanFactory.registerBeanDefinition("redisConnectionFactory", LettuceConnectionConfiguration__BeanDefinitions.getRedisConnectionFactoryBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration", RedisAutoConfiguration__BeanDefinitions.getRedisAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("redisConnectionDetails", RedisAutoConfiguration__BeanDefinitions.getRedisConnectionDetailsBeanDefinition());
    beanFactory.registerBeanDefinition("spring.data.redis-org.springframework.boot.autoconfigure.data.redis.RedisProperties", RedisProperties__BeanDefinitions.getRedisPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration$Jackson2ObjectMapperBuilderCustomizerConfiguration", JacksonAutoConfiguration__BeanDefinitions.Jackson2ObjectMapperBuilderCustomizerConfiguration.getJacksonObjectMapperBuilderCustomizerConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("standardJacksonObjectMapperBuilderCustomizer", JacksonAutoConfiguration__BeanDefinitions.Jackson2ObjectMapperBuilderCustomizerConfiguration.getStandardJacksonObjectMapperBuilderCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("spring.jackson-org.springframework.boot.autoconfigure.jackson.JacksonProperties", JacksonProperties__BeanDefinitions.getJacksonPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration$JacksonObjectMapperBuilderConfiguration", JacksonAutoConfiguration__BeanDefinitions.JacksonObjectMapperBuilderConfiguration.getJacksonObjectMapperBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jacksonObjectMapperBuilder", JacksonAutoConfiguration__BeanDefinitions.JacksonObjectMapperBuilderConfiguration.getJacksonObjectMapperBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration$ParameterNamesModuleConfiguration", JacksonAutoConfiguration__BeanDefinitions.ParameterNamesModuleConfiguration.getParameterNamesModuleConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("parameterNamesModule", JacksonAutoConfiguration__BeanDefinitions.ParameterNamesModuleConfiguration.getParameterNamesModuleBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration$JacksonObjectMapperConfiguration", JacksonAutoConfiguration__BeanDefinitions.JacksonObjectMapperConfiguration.getJacksonObjectMapperConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jacksonObjectMapper", JacksonAutoConfiguration__BeanDefinitions.JacksonObjectMapperConfiguration.getJacksonObjectMapperBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration$JacksonMixinConfiguration", JacksonAutoConfiguration__BeanDefinitions.JacksonMixinConfiguration.getJacksonMixinConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jsonMixinModuleEntries", JsonMixinModuleEntries__BeanDefinitions.getJsonMixinModuleEntriesBeanDefinition());
    beanFactory.registerBeanDefinition("jsonMixinModule", JacksonAutoConfiguration__BeanDefinitions.JacksonMixinConfiguration.getJsonMixinModuleBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration", JacksonAutoConfiguration__BeanDefinitions.getJacksonAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jsonComponentModule", JacksonAutoConfiguration__BeanDefinitions.getJsonComponentModuleBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration", TransactionManagerCustomizationAutoConfiguration__BeanDefinitions.getTransactionManagerCustomizationAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("platformTransactionManagerCustomizers", TransactionManagerCustomizationAutoConfiguration__BeanDefinitions.getPlatformTransactionManagerCustomizersBeanDefinition());
    beanFactory.registerBeanDefinition("transactionExecutionListeners", TransactionManagerCustomizationAutoConfiguration__BeanDefinitions.getTransactionExecutionListenersBeanDefinition());
    beanFactory.registerBeanDefinition("spring.transaction-org.springframework.boot.autoconfigure.transaction.TransactionProperties", TransactionProperties__BeanDefinitions.getTransactionPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration", ConfigurationPropertiesAutoConfiguration__BeanDefinitions.getConfigurationPropertiesAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration", LifecycleAutoConfiguration__BeanDefinitions.getLifecycleAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("lifecycleProcessor", LifecycleAutoConfiguration__BeanDefinitions.getLifecycleProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("spring.lifecycle-org.springframework.boot.autoconfigure.context.LifecycleProperties", LifecycleProperties__BeanDefinitions.getLifecyclePropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration", PersistenceExceptionTranslationAutoConfiguration__BeanDefinitions.getPersistenceExceptionTranslationAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("persistenceExceptionTranslationPostProcessor", PersistenceExceptionTranslationAutoConfiguration__BeanDefinitions.getPersistenceExceptionTranslationPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration", RedisReactiveAutoConfiguration__BeanDefinitions.getRedisReactiveAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("reactiveRedisTemplate", RedisReactiveAutoConfiguration__BeanDefinitions.getReactiveRedisTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("reactiveStringRedisTemplate", RedisReactiveAutoConfiguration__BeanDefinitions.getReactiveStringRedisTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration", RedisRepositoriesAutoConfiguration__BeanDefinitions.getRedisRepositoriesAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("redisIndexConfiguration#0", IndexConfiguration__BeanDefinitions.getRedisIndexConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("redisKeyspaceConfiguration#0", KeyspaceConfiguration__BeanDefinitions.getRedisKeyspaceConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("redisMappingConfiguration#0", MappingConfiguration__BeanDefinitions.getRedisMappingConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("keyValueMappingContext", RedisMappingContext__BeanDefinitions.getKeyValueMappingContextBeanDefinition());
    beanFactory.registerBeanDefinition("redisCustomConversions", RedisCustomConversions__BeanDefinitions.getRedisCustomConversionsBeanDefinition());
    beanFactory.registerBeanDefinition("redisReferenceResolver", ReferenceResolverImpl__BeanDefinitions.getRedisReferenceResolverBeanDefinition());
    beanFactory.registerBeanDefinition("redisConverter", MappingRedisConverter__BeanDefinitions.getRedisConverterBeanDefinition());
    beanFactory.registerBeanDefinition("redisKeyValueAdapter", RedisKeyValueAdapter__BeanDefinitions.getRedisKeyValueAdapterBeanDefinition());
    beanFactory.registerBeanDefinition("redisKeyValueTemplate", RedisKeyValueTemplate__BeanDefinitions.getRedisKeyValueTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration$StringHttpMessageConverterConfiguration", HttpMessageConvertersAutoConfiguration__BeanDefinitions.StringHttpMessageConverterConfiguration.getStringHttpMessageConverterConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("stringHttpMessageConverter", HttpMessageConvertersAutoConfiguration__BeanDefinitions.StringHttpMessageConverterConfiguration.getStringHttpMessageConverterBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.http.JacksonHttpMessageConvertersConfiguration$MappingJackson2HttpMessageConverterConfiguration", JacksonHttpMessageConvertersConfiguration__BeanDefinitions.MappingJackson2HttpMessageConverterConfiguration.getMappingJacksonHttpMessageConverterConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("mappingJackson2HttpMessageConverter", JacksonHttpMessageConvertersConfiguration__BeanDefinitions.MappingJackson2HttpMessageConverterConfiguration.getMappingJacksonHttpMessageConverterBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.http.JacksonHttpMessageConvertersConfiguration", JacksonHttpMessageConvertersConfiguration__BeanDefinitions.getJacksonHttpMessageConvertersConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration", HttpMessageConvertersAutoConfiguration__BeanDefinitions.getHttpMessageConvertersAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("messageConverters", HttpMessageConvertersAutoConfiguration__BeanDefinitions.getMessageConvertersBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.data.web.config.ProjectingArgumentResolverRegistrar", ProjectingArgumentResolverRegistrar__BeanDefinitions.getProjectingArgumentResolverRegistrarBeanDefinition());
    beanFactory.registerBeanDefinition("projectingArgumentResolverBeanPostProcessor", ProjectingArgumentResolverRegistrar__BeanDefinitions.getProjectingArgumentResolverBeanPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.data.web.config.SpringDataWebConfiguration", SpringDataWebConfiguration__BeanDefinitions.getSpringDataWebConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("pageableResolver", SpringDataWebConfiguration__BeanDefinitions.getPageableResolverBeanDefinition());
    beanFactory.registerBeanDefinition("sortResolver", SpringDataWebConfiguration__BeanDefinitions.getSortResolverBeanDefinition());
    beanFactory.registerBeanDefinition("offsetResolver", SpringDataWebConfiguration__BeanDefinitions.getOffsetResolverBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.data.web.config.SpringDataJacksonConfiguration", SpringDataJacksonConfiguration__BeanDefinitions.getSpringDataJacksonConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jacksonGeoModule", SpringDataJacksonConfiguration__BeanDefinitions.getJacksonGeoModuleBeanDefinition());
    beanFactory.registerBeanDefinition("pageModule", SpringDataJacksonConfiguration__BeanDefinitions.getPageModuleBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration", SpringDataWebAutoConfiguration__BeanDefinitions.getSpringDataWebAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("pageableCustomizer", SpringDataWebAutoConfiguration__BeanDefinitions.getPageableCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("sortCustomizer", SpringDataWebAutoConfiguration__BeanDefinitions.getSortCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.data.web.config.SpringDataWebSettings", SpringDataWebSettings__BeanDefinitions.getSpringDataWebSettingsBeanDefinition());
    beanFactory.registerBeanDefinition("spring.data.web-org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties", SpringDataWebProperties__BeanDefinitions.getSpringDataWebPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration", ProjectInfoAutoConfiguration__BeanDefinitions.getProjectInfoAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.info-org.springframework.boot.autoconfigure.info.ProjectInfoProperties", ProjectInfoProperties__BeanDefinitions.getProjectInfoPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.JdbcTemplateConfiguration", JdbcTemplateConfiguration__BeanDefinitions.getJdbcTemplateConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jdbcTemplate", JdbcTemplateConfiguration__BeanDefinitions.getJdbcTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.NamedParameterJdbcTemplateConfiguration", NamedParameterJdbcTemplateConfiguration__BeanDefinitions.getNamedParameterJdbcTemplateConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("namedParameterJdbcTemplate", NamedParameterJdbcTemplateConfiguration__BeanDefinitions.getNamedParameterJdbcTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration", JdbcTemplateAutoConfiguration__BeanDefinitions.getJdbcTemplateAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.jdbc-org.springframework.boot.autoconfigure.jdbc.JdbcProperties", JdbcProperties__BeanDefinitions.getJdbcPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer$DependsOnDatabaseInitializationPostProcessor", DatabaseInitializationDependencyConfigurer__BeanDefinitions.DependsOnDatabaseInitializationPostProcessor.getDependsOnDatabaseInitializationPostProcessorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration", JdbcClientAutoConfiguration__BeanDefinitions.getJdbcClientAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("jdbcClient", JdbcClientAutoConfiguration__BeanDefinitions.getJdbcClientBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.netty.NettyAutoConfiguration", NettyAutoConfiguration__BeanDefinitions.getNettyAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.netty-org.springframework.boot.autoconfigure.netty.NettyProperties", NettyProperties__BeanDefinitions.getNettyPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.reactor.ReactorAutoConfiguration", ReactorAutoConfiguration__BeanDefinitions.getReactorAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.reactor-org.springframework.boot.autoconfigure.reactor.ReactorProperties", ReactorProperties__BeanDefinitions.getReactorPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.security.servlet.SpringBootWebSecurityConfiguration", SpringBootWebSecurityConfiguration__BeanDefinitions.getSpringBootWebSecurityConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration", SecurityAutoConfiguration__BeanDefinitions.getSecurityAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("authenticationEventPublisher", SecurityAutoConfiguration__BeanDefinitions.getAuthenticationEventPublisherBeanDefinition());
    beanFactory.registerBeanDefinition("spring.security-org.springframework.boot.autoconfigure.security.SecurityProperties", SecurityProperties__BeanDefinitions.getSecurityPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration", SecurityFilterAutoConfiguration__BeanDefinitions.getSecurityFilterAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("securityFilterChainRegistration", SecurityFilterAutoConfiguration__BeanDefinitions.getSecurityFilterChainRegistrationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration", UserDetailsServiceAutoConfiguration__BeanDefinitions.getUserDetailsServiceAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("inMemoryUserDetailsManager", UserDetailsServiceAutoConfiguration__BeanDefinitions.getInMemoryUserDetailsManagerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.sql.init.DataSourceInitializationConfiguration", DataSourceInitializationConfiguration__BeanDefinitions.getDataSourceInitializationConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("dataSourceScriptDatabaseInitializer", DataSourceInitializationConfiguration__BeanDefinitions.getDataSourceScriptDatabaseInitializerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration", SqlInitializationAutoConfiguration__BeanDefinitions.getSqlInitializationAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("spring.sql.init-org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties", SqlInitializationProperties__BeanDefinitions.getSqlInitializationPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskSchedulingConfigurations$ThreadPoolTaskSchedulerBuilderConfiguration", TaskSchedulingConfigurations__BeanDefinitions.ThreadPoolTaskSchedulerBuilderConfiguration.getThreadPoolTaskSchedulerBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("threadPoolTaskSchedulerBuilder", TaskSchedulingConfigurations__BeanDefinitions.ThreadPoolTaskSchedulerBuilderConfiguration.getThreadPoolTaskSchedulerBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskSchedulingConfigurations$TaskSchedulerBuilderConfiguration", TaskSchedulingConfigurations__BeanDefinitions.TaskSchedulerBuilderConfiguration.getTaskSchedulerBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("taskSchedulerBuilder", TaskSchedulingConfigurations__BeanDefinitions.TaskSchedulerBuilderConfiguration.getTaskSchedulerBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskSchedulingConfigurations$SimpleAsyncTaskSchedulerBuilderConfiguration", TaskSchedulingConfigurations__BeanDefinitions.SimpleAsyncTaskSchedulerBuilderConfiguration.getSimpleAsyncTaskSchedulerBuilderConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("simpleAsyncTaskSchedulerBuilder", TaskSchedulingConfigurations__BeanDefinitions.SimpleAsyncTaskSchedulerBuilderConfiguration.getSimpleAsyncTaskSchedulerBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskSchedulingConfigurations$TaskSchedulerConfiguration", TaskSchedulingConfigurations__BeanDefinitions.TaskSchedulerConfiguration.getTaskSchedulerConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("taskScheduler", TaskSchedulingConfigurations__BeanDefinitions.TaskSchedulerConfiguration.getTaskSchedulerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration", TaskSchedulingAutoConfiguration__BeanDefinitions.getTaskSchedulingAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("scheduledBeanLazyInitializationExcludeFilter", TaskSchedulingAutoConfiguration__BeanDefinitions.getScheduledBeanLazyInitializationExcludeFilterBeanDefinition());
    beanFactory.registerBeanDefinition("spring.task.scheduling-org.springframework.boot.autoconfigure.task.TaskSchedulingProperties", TaskSchedulingProperties__BeanDefinitions.getTaskSchedulingPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration$JdbcTransactionManagerConfiguration", DataSourceTransactionManagerAutoConfiguration__BeanDefinitions.JdbcTransactionManagerConfiguration.getJdbcTransactionManagerConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("transactionManager", DataSourceTransactionManagerAutoConfiguration__BeanDefinitions.JdbcTransactionManagerConfiguration.getTransactionManagerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration", DataSourceTransactionManagerAutoConfiguration__BeanDefinitions.getDataSourceTransactionManagerAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration", ProxyTransactionManagementConfiguration__BeanDefinitions.getProxyTransactionManagementConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.transaction.config.internalTransactionAdvisor", ProxyTransactionManagementConfiguration__BeanDefinitions.getInternalTransactionAdvisorBeanDefinition());
    beanFactory.registerBeanDefinition("transactionAttributeSource", ProxyTransactionManagementConfiguration__BeanDefinitions.getTransactionAttributeSourceBeanDefinition());
    beanFactory.registerBeanDefinition("transactionInterceptor", ProxyTransactionManagementConfiguration__BeanDefinitions.getTransactionInterceptorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.transaction.config.internalTransactionalEventListenerFactory", AbstractTransactionManagementConfiguration__BeanDefinitions.getInternalTransactionalEventListenerFactoryBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration$EnableTransactionManagementConfiguration$CglibAutoProxyConfiguration", TransactionAutoConfiguration__BeanDefinitions.EnableTransactionManagementConfiguration.CglibAutoProxyConfiguration.getCglibAutoProxyConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator", InfrastructureAdvisorAutoProxyCreator__BeanDefinitions.getInternalAutoProxyCreatorBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration$EnableTransactionManagementConfiguration", TransactionAutoConfiguration__BeanDefinitions.EnableTransactionManagementConfiguration.getEnableTransactionManagementConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration$TransactionTemplateConfiguration", TransactionAutoConfiguration__BeanDefinitions.TransactionTemplateConfiguration.getTransactionTemplateConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("transactionTemplate", TransactionAutoConfiguration__BeanDefinitions.TransactionTemplateConfiguration.getTransactionTemplateBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration", TransactionAutoConfiguration__BeanDefinitions.getTransactionAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration", RestClientAutoConfiguration__BeanDefinitions.getRestClientAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("httpMessageConvertersRestClientCustomizer", RestClientAutoConfiguration__BeanDefinitions.getHttpMessageConvertersRestClientCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("restClientSsl", RestClientAutoConfiguration__BeanDefinitions.getRestClientSslBeanDefinition());
    beanFactory.registerBeanDefinition("restClientBuilderConfigurer", RestClientAutoConfiguration__BeanDefinitions.getRestClientBuilderConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("restClientBuilder", RestClientAutoConfiguration__BeanDefinitions.getRestClientBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration", RestTemplateAutoConfiguration__BeanDefinitions.getRestTemplateAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("restTemplateBuilderConfigurer", RestTemplateAutoConfiguration__BeanDefinitions.getRestTemplateBuilderConfigurerBeanDefinition());
    beanFactory.registerBeanDefinition("restTemplateBuilder", RestTemplateAutoConfiguration__BeanDefinitions.getRestTemplateBuilderBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration$TomcatWebServerFactoryCustomizerConfiguration", EmbeddedWebServerFactoryCustomizerAutoConfiguration__BeanDefinitions.TomcatWebServerFactoryCustomizerConfiguration.getTomcatWebServerFactoryCustomizerConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("tomcatWebServerFactoryCustomizer", EmbeddedWebServerFactoryCustomizerAutoConfiguration__BeanDefinitions.TomcatWebServerFactoryCustomizerConfiguration.getTomcatWebServerFactoryCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration", EmbeddedWebServerFactoryCustomizerAutoConfiguration__BeanDefinitions.getEmbeddedWebServerFactoryCustomizerAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration", HttpEncodingAutoConfiguration__BeanDefinitions.getHttpEncodingAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("characterEncodingFilter", HttpEncodingAutoConfiguration__BeanDefinitions.getCharacterEncodingFilterBeanDefinition());
    beanFactory.registerBeanDefinition("localeCharsetMappingsCustomizer", HttpEncodingAutoConfiguration__BeanDefinitions.getLocaleCharsetMappingsCustomizerBeanDefinition());
    beanFactory.registerBeanDefinition("org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration", MultipartAutoConfiguration__BeanDefinitions.getMultipartAutoConfigurationBeanDefinition());
    beanFactory.registerBeanDefinition("multipartConfigElement", MultipartAutoConfiguration__BeanDefinitions.getMultipartConfigElementBeanDefinition());
    beanFactory.registerBeanDefinition("multipartResolver", MultipartAutoConfiguration__BeanDefinitions.getMultipartResolverBeanDefinition());
    beanFactory.registerBeanDefinition("spring.servlet.multipart-org.springframework.boot.autoconfigure.web.servlet.MultipartProperties", MultipartProperties__BeanDefinitions.getMultipartPropertiesBeanDefinition());
    beanFactory.registerBeanDefinition("mvcHandlerMappingIntrospectorRequestTransformer", HandlerMappingIntrospectorRequestTransformer__BeanDefinitions.getMvcHandlerMappingIntrospectorRequestTransformerBeanDefinition());
    beanFactory.registerBeanDefinition("springSecurityFilterChain", WebMvcSecurityConfiguration__BeanDefinitions.CompositeFilterChainProxy.getSpringSecurityFilterChainBeanDefinition());
    beanFactory.registerBeanDefinition("adminMapper", getAdminMapperBeanDefinition());
    beanFactory.registerBeanDefinition("parkingSpotMapper", getParkingSpotMapperBeanDefinition());
    beanFactory.registerBeanDefinition("paymentRecordMapper", getPaymentRecordMapperBeanDefinition());
    beanFactory.registerBeanDefinition("rechargeRecordMapper", getRechargeRecordMapperBeanDefinition());
    beanFactory.registerBeanDefinition("reservationMapper", getReservationMapperBeanDefinition());
    beanFactory.registerBeanDefinition("systemConfigMapper", getSystemConfigMapperBeanDefinition());
    beanFactory.registerBeanDefinition("userMapper", getUserMapperBeanDefinition());
    beanFactory.registerBeanDefinition("vehicleRecordMapper", getVehicleRecordMapperBeanDefinition());
  }

  /**
   * Register the aliases.
   */
  public void registerAliases(DefaultListableBeanFactory beanFactory) {
    beanFactory.registerAlias("applicationTaskExecutor", "taskExecutor");
  }
}
