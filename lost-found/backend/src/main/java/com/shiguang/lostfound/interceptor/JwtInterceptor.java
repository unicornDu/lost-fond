package com.shiguang.lostfound.interceptor;

import com.shiguang.lostfound.config.JwtProperties;
import com.shiguang.lostfound.context.UserContext;
import com.shiguang.lostfound.entity.User;
import com.shiguang.lostfound.mapper.UserMapper;
import com.shiguang.lostfound.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：请求头带令牌则解析并写入 UserContext（不带也放行，具体鉴权在业务层）。
 * 审查 2.24①：带 token 时查一次用户 status，封禁者等同匿名（封禁实时生效）。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties props;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String header = request.getHeader(props.getHeader());
        if (header != null && header.startsWith(props.getPrefix())) {
            String token = header.substring(props.getPrefix().length()).trim();
            Claims claims = jwtUtil.parse(token);
            if (claims != null) {
                Long uid = Long.valueOf(claims.getSubject());
                // 审查 2.24①：封禁实时性——带 token 请求查一次 status，被封禁则等同匿名（不写入上下文）
                User u = userMapper.selectById(uid);
                if (u != null && u.getStatus() != null && u.getStatus() == 0) {
                    return true; // 放行但视为未登录，业务层 requireUserId 会抛 401
                }
                Integer role = (u != null ? u.getRole() : claims.get("role", Integer.class));
                UserContext.set(uid, role == null ? 0 : role);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
