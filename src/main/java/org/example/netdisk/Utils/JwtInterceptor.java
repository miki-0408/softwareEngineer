package org.example.netdisk.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.PCOI.Service.Inter.LogService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final LogService logService;

    public JwtInterceptor(LogService logService) {
        this.logService = logService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            String userId = (String)TokenProcess.getAttributeFromToken(token, "userId");
            String operation = request.getRequestURI();
            String original = (String) request.getAttribute("jakarta.servlet.error.request_uri");
            if (original != null)
            {
                operation = original;
            }
            logService.logMethodExecution(userId, operation);
            token = token.substring(7);
            System.out.println("Received usertoken: " + token);
            Map<String, Object> claims = JwtUtil.parseToken(token);
            request.setAttribute("claims", claims);
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}