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

@Configuration // 标记为Spring配置类，应用启动时自动加载
public class WebConfig implements WebMvcConfigurer {

    @Value("${netdisk.upload.base-dir}") // 从配置文件中读取文件上传的物理存储根目录
    private String uploadBaseDir;
    @Value("${netdisk.upload.url-prefix:/files/}") // 从配置文件读取URL访问前缀，默认值为/files/
    private String uploadUrlPrefix;

    private final JwtInterceptor jwtInterceptor; // 基础JWT拦截器，验证令牌有效性
    private final JwtUserInterceptor jwtUserInterceptor = new JwtUserInterceptor(); // 普通用户权限拦截器，校验用户身份
    private final JwtSysAdminInterceptor jwtSysAdminInterceptor = new JwtSysAdminInterceptor(); // 系统管理员权限拦截器，校验管理员身份

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor; // 通过构造函数注入JWT拦截器实例
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor) // 注册全局JWT认证拦截器
                .addPathPatterns("/**") // 拦截所有请求路径
                .excludePathPatterns( // 排除不需要认证的路径
                        "/login", // 登录接口无需令牌
                        "/register", // 注册接口无需令牌
                        "/files/**" // 静态文件资源无需令牌，可直接访问
                );

        registry.addInterceptor(jwtUserInterceptor) // 注册用户角色拦截器
                .addPathPatterns("/user/**"); // 仅拦截用户相关的API路径，确保调用者是合法用户

        registry.addInterceptor(jwtSysAdminInterceptor) // 注册管理员角色拦截器
                .addPathPatterns("/systemAdmin/**"); // 仅拦截管理员API路径，确保调用者具有管理员权限
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String prefix = uploadUrlPrefix; // 获取配置的URL访问前缀
        if (prefix == null || prefix.isBlank()) {
            prefix = "/files/"; // 前缀为空时使用默认值，保证资源始终可访问
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix; // 确保前缀以斜杠开头，符合Spring URL映射规范
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/"; // 确保前缀以斜杠结尾，构成有效的路径模式
        }
        String base = uploadBaseDir == null ? "uploads/" : uploadBaseDir; // 物理存储目录为空时使用默认uploads/目录
        base = base.replace('\\', '/'); // 将Windows反斜杠转换为正斜杠，保证跨平台兼容
        if (!base.endsWith("/")) {
            base = base + "/"; // 确保物理路径以斜杠结尾，正确拼接文件路径
        }
        registry.addResourceHandler(prefix + "**") // 将URL路径模式映射到物理文件系统
                .addResourceLocations("file:" + base); // 使用file:协议指向本地磁盘目录，使上传的文件可通过HTTP直接访问
    }
}
