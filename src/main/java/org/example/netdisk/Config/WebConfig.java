package org.example.netdisk.Config;

import org.example.netdisk.Utils.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${pcoi.upload.base-dir}")
    private String uploadBaseDir;
    @Value("${pcoi.upload.url-prefix:/files/}")
    private String uploadUrlPrefix;

    private final JwtInterceptor jwtInterceptor ;
    private final JwtUserInterceptor jwtUserInterceptor = new JwtUserInterceptor();
    private final JwtSysAdminInterceptor jwtSysAdminInterceptor = new JwtSysAdminInterceptor();
    private final JwtCommunityAdminInterceptor jwtCommunityAdminInterceptor = new JwtCommunityAdminInterceptor();
    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 所有接口通用拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/login",
                    "/register",
                    "/mySecurityIssues",
                    "/verifySecurityIssues",
                    "/updatePassword",
                    "/illustrations",
                    "/mangas",
                    "/searchById",
                    "/searchByName",
                    "/searchByTag",
                    "/files/**",
                    "/allContributions"
                );

        // 普通用户接口
        registry.addInterceptor(jwtUserInterceptor)
                .addPathPatterns("/user/**");

        // 系统管理员接口
        registry.addInterceptor(jwtSysAdminInterceptor)
                .addPathPatterns("/systemAdmin/**");
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 统一将 /files/** 映射到本地上传目录，供前端直接访问
        String prefix = uploadUrlPrefix;
        if (prefix == null || prefix.isBlank()) prefix = "/files/";
        if (!prefix.startsWith("/")) prefix = "/" + prefix;
        if (!prefix.endsWith("/")) prefix = prefix + "/";
        String base = uploadBaseDir == null ? "uploads/" : uploadBaseDir;
        base = base.replace('\\', '/');
        if (!base.endsWith("/")) base = base + "/";
        registry.addResourceHandler(prefix + "**")
                .addResourceLocations("file:" + base);
    }
}