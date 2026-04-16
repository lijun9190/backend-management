package com.example.common.security;

import com.example.common.constant.CommonConstants;
import com.example.common.model.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret:backend-manager-secret-key}")
    private String secret;

    @Value("${security.jwt.access-expire-seconds:${security.jwt.expire-seconds:1800}}")
    private long accessExpireSeconds;

    public String createAccessToken(LoginUser loginUser, String sessionId, Integer accessTokenVersion) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + accessExpireSeconds * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(loginUser.getUserId()))
                .claim("username", loginUser.getUsername())
                .claim("nickname", loginUser.getNickname())
                .claim("sid", sessionId)
                .claim("typ", CommonConstants.ACCESS_TOKEN_TYPE)
                .claim("av", accessTokenVersion)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(SignatureAlgorithm.HS512, secret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public String getUsername(String token) {
        Object username = getClaims(token).get("username");
        return username == null ? null : username.toString();
    }

    public String getSessionId(String token) {
        Object sessionId = getClaims(token).get("sid");
        return sessionId == null ? null : sessionId.toString();
    }

    public Integer getAccessTokenVersion(String token) {
        Object version = getClaims(token).get("av");
        if (version instanceof Integer) {
            return (Integer) version;
        }
        if (version instanceof Number) {
            return ((Number) version).intValue();
        }
        return version == null ? null : Integer.valueOf(version.toString());
    }

    public String getTokenType(String token) {
        Object tokenType = getClaims(token).get("typ");
        return tokenType == null ? null : tokenType.toString();
    }

    public boolean validateAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = getClaims(token);
            return CommonConstants.ACCESS_TOKEN_TYPE.equals(getTokenType(token))
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }
}
