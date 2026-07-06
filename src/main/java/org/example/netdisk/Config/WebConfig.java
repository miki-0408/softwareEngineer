package org.example.netdisk.Config;

import org.example.netdisk.Utils.JwtInterceptor;
import org.example.netdisk.Utils.JwtSysAdminInterceptor;
import org.example.netdisk.Utils.JwtUserInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${netdisk.upload.base-dir}")
    private String uploadBaseDir;
    @Value("${netdisk.upload.url-prefix:/files/}")
    private String uploadUrlPrefix;

    private final JwtInterceptor jwtInterceptor;
    private final JwtUserInterceptor jwtUserInterceptor = new JwtUserInterceptor();
    private final JwtSysAdminInterceptor jwtSysAdminInterceptor = new JwtSysAdminInterceptor();

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/files/**"
                );

        registry.addInterceptor(jwtUserInterceptor)
                .addPathPatterns("/user/**");

        registry.addInterceptor(jwtSysAdminInterceptor)
                .addPathPatterns("/systemAdmin/**");
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String prefix = uploadUrlPrefix;
        if (prefix == null || prefix.isBlank()) {
            prefix = "/files/";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String base = uploadBaseDir == null ? "uploads/" : uploadBaseDir;
        base = base.replace('\\', '/');
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        registry.addResourceHandler(prefix + "**")
                .addResourceLocations("file:" + base);
    }
}
