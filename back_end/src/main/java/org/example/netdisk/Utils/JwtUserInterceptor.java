package org.example.netdisk.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

import static org.example.netdisk.Service.Support.Enum.normalUser;

public class JwtUserInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) { // 普通用户权限拦截：仅允许role=user的请求通过
        String token = request.getHeader("Authorization"); // 获取Authorization请求头
        if (token != null && token.startsWith("Bearer ")) { // 检查令牌格式
            token = token.substring(7); // 去掉"Bearer "前缀提取纯令牌
            try {
                Map<String, Object> claims = JwtUtil.parseToken(token); // 解析JWT令牌
                String role = (String) claims.get("role"); // 获取用户角色
                if (normalUser.equals(role)) { // 角色为普通用户则放行
                    request.setAttribute("claims", claims); // 将载荷存入request供后续使用
                    return true; // 放行
                } else { // 角色不符返回403禁止访问（如管理员试图访问用户接口）
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            } catch (Exception e) { // 令牌解析失败返回401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 无令牌返回401
        return false;
    }
}
