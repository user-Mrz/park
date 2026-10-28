package com.parking.framework.config;

import com.parking.framework.interceptor.JwtInterceptor;
import com.parking.framework.interceptor.MenuAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final MenuAuthInterceptor menuAuthInterceptor;

    @Value("${parking.upload-dir:./uploads}")
    private String uploadDir;

    /** 管理端跨域来源白名单（逗号分隔）：部署到正式域名时通过 CORS_ALLOWED_ORIGINS 覆盖 */
    @Value("${parking.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    public WebConfig(JwtInterceptor jwtInterceptor, MenuAuthInterceptor menuAuthInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.menuAuthInterceptor = menuAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 认证走 Authorization 头，不依赖 Cookie，因此关闭凭证携带以缩小跨域攻击面
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * 暴露上传目录（头像等静态文件），小程序通过 <image src="{{fileBase}}/uploads/..."> 直接访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String abs = Paths.get(uploadDir).toAbsolutePath().normalize().toString().replace("\\", "/");
        if (!abs.endsWith("/")) abs = abs + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + abs);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1) JWT 校验拦截器：所有 /api/** 都生效，排除登录/公告/学院列表等公开接口
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/**",
                    // 第三方校园管理系统对接接口：不使用 JWT，鉴权由请求签名保证
                    "/api/open/**",
                    // 注意：/api/spot/overview 不在此白名单内。
                    // 该接口会返回当前用户的在场车辆与预约信息，必须登录后才能访问（未登录返回 401）。
                    // 公告属于面向全校的公开信息，列表/详情/附件下载均无需登录。
                    "/api/announcement/**",
                    "/api/departments",
                    "/api/departments/**",
                    "/uploads/**",
                    "/doc.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                )
                .order(1);

        // 2) 后台菜单权限拦截器：仅对 /api/admin/** 生效
        //    依赖 JwtInterceptor 已设置 UserContext.role / userId，需在其之后执行
        registry.addInterceptor(menuAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);
    }
}
