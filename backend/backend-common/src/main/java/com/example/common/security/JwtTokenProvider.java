package com.example.common.security;

import com.example.common.model.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类，负责生成和解析 token。
 */
@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret:backend-manager-secret-key}")
    private String secret;

    @Value("${security.jwt.expire-seconds:7200}")
    private long expireSeconds;

/**
 * 创建JWT令牌的方法
 * @param loginUser 登录用户信息对象，包含用户ID、用户名和昵称等
 * @return 返回生成的JWT令牌字符串
 */
    public String createToken(LoginUser loginUser) {
    // 获取当前时间作为令牌签发时间
        Date now = new Date();
    // 计算令牌过期时间：当前时间加上设置的过期秒数
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000);
    // 使用Jwts构建器创建JWT令牌
        return Jwts.builder()
            // 设置主题为用户ID
                .setSubject(String.valueOf(loginUser.getUserId()))
            // 添加自定义声明：用户名
                .claim("username", loginUser.getUsername())
            // 添加自定义声明：昵称
                .claim("nickname", loginUser.getNickname())
            // 设置签发时间
                .setIssuedAt(now)
            // 设置过期时间
                .setExpiration(expireAt)
            // 使用HS512算法和密钥进行签名
                .signWith(SignatureAlgorithm.HS512, secret.getBytes(StandardCharsets.UTF_8))
            // 压缩生成令牌字符串
                .compact();
    }

/**
 * 从JWT令牌中解析声明信息
 * @param token JWT令牌字符串
 * @return 解析后的Claims对象，包含令牌中的所有声明信息
 */
    public Claims getClaims(String token) {
    // 使用JWT解析器设置签名密钥并解析令牌
        return Jwts.parser()
            // 设置签名密钥，将字符串转换为UTF-8编码的字节数组
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
            // 解析JWT令牌并获取主体部分（即Claims）
                .parseClaimsJws(token)
            // 获取Claims主体
                .getBody();
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getUsername(String token) {
        Object username = getClaims(token).get("username");
        return username == null ? null : username.toString();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
