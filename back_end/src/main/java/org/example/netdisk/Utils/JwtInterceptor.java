package org.example.netdisk.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.netdisk.Service.Inter.LogService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final LogService logService; // 日志服务，用于记录用户操作

    public JwtInterceptor(LogService logService) { // 构造器注入日志服务
        this.logService = logService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception { // 请求前置拦截：验证JWT并记录日志
        String token = request.getHeader("Authorization"); // 从请求头获取Authorization
        if (token != null && token.startsWith("Bearer ")) { // 检查是否携带Bearer令牌
            String userId = (String) TokenProcess.getAttributeFromToken(token, "userId"); // 从令牌中提取用户ID
            String operation = request.getRequestURI(); // 获取请求的URI作为操作描述
            String original = (String) request.getAttribute("jakarta.servlet.error.request_uri"); // 处理错误转发场景：获取原始请求URI
            if (original != null) { // 若存在原始URI则使用原始路径记录日志
                operation = original;
            }
            logService.logMethodExecution(userId, operation); // 记录用户操作日志
            token = token.substring(7); // 去掉"Bearer "前缀（7个字符）
            Map<String, Object> claims = JwtUtil.parseToken(token); // 解析JWT令牌获取载荷数据
            request.setAttribute("claims", claims); // 将载荷存入request，供后续Controller使用
            return true; // 放行请求
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 无有效令牌返回401
        return false; // 拦截请求
    }
}
