package com.adminpro.interceptor;

import com.adminpro.common.Result;
import com.adminpro.common.ResultCode;
import com.adminpro.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * JWT 鉴权拦截器：校验 Authorization: Bearer <token>，
 * 并校验 Redis 中的登录态（单点登录，新登录顶掉旧 token）
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_KEY_PREFIX = "adminpro:token:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        if (token == null || token.isBlank()) {
            writeUnauthorized(response);
            return false;
        }
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String redisToken = stringRedisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
            if (!Objects.equals(token, redisToken)) {
                writeUnauthorized(response);
                return false;
            }
            request.setAttribute("userId", userId);
            request.setAttribute("username", claims.get("username", String.class));
            return true;
        } catch (Exception e) {
            writeUnauthorized(response);
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ResultCode.UNAUTHORIZED)));
    }
}
