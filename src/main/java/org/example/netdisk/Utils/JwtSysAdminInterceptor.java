package org.example.netdisk.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

import static org.example.PCOI.Service.Support.Enum.systemAdmin;

public class JwtSysAdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler){
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            System.out.println("Received systoken: " + token);
            try {
                Map<String, Object> claims = JwtUtil.parseToken(token);
                Integer role = (Integer) claims.get("role");
                if (role.equals(systemAdmin)) {
                    request.setAttribute("claims", claims);
                    return true;
                } else {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}