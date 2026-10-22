package com.shiguang.lostfound.util;

import com.shiguang.lostfound.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发 / 解析登录令牌。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String account, Integer role) {
        return generate(userId, account, role, props.getExpireMinutes());
    }

    /** 审查 2.40：支持自定义有效期（"记住我"用 remember-minutes）。 */
    public String generate(Long userId, String account, Integer role, long expireMinutes) {
        long now = System.currentTimeMillis();
        long exp = now + expireMinutes * 60 * 1000;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("account", account)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key())
                .compact();
    }

    /** 解析令牌，失败返回 null。 */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
