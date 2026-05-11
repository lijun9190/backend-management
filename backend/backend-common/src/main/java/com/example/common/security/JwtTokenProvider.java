package com.example.common.security;

import com.example.common.constant.CommonConstants;
import com.example.common.model.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * JWT令牌提供者，使用RS256非对称加密算法生成和验证访问令牌。
 * 私钥仅用于签名（auth服务持有），公钥用于验证（所有服务共享）。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    /** 私钥文件路径，仅auth服务配置，用于签名token */
    @Value("${security.jwt.private-key-path:}")
    private String privateKeyPath;

    /** 公钥文件路径，所有服务配置，用于验证token */
    @Value("${security.jwt.public-key-path:}")
    private String publicKeyPath;

    /** 访问令牌过期时间（秒） */
    @Value("${security.jwt.access-expire-seconds:${security.jwt.expire-seconds:1800}}")
    private long accessExpireSeconds;

    /** RSA私钥，用于签名token */
    private PrivateKey privateKey;

    /** RSA公钥，用于验证token */
    private PublicKey publicKey;

    /**
     * 初始化方法，从PEM文件加载RSA密钥对。
     * 私钥可选（仅auth服务需要），公钥必须存在。
     */
    @PostConstruct
    public void init() {
        if (StringUtils.hasText(privateKeyPath)) {
            this.privateKey = loadPrivateKey(privateKeyPath);
            log.info("已加载RSA私钥: {}", privateKeyPath);
        }
        if (StringUtils.hasText(publicKeyPath)) {
            this.publicKey = loadPublicKey(publicKeyPath);
            log.info("已加载RSA公钥: {}", publicKeyPath);
        } else if (privateKey != null) {
            // 如果只配置了私钥，尝试从私钥文件中提取公钥
            KeyPair keyPair = loadKeyPair(privateKeyPath);
            if (keyPair != null) {
                this.publicKey = keyPair.getPublic();
                log.info("从私钥文件中提取RSA公钥: {}", privateKeyPath);
            }
        }
        if (this.publicKey == null) {
            throw new IllegalStateException("RSA公钥未配置，请设置 security.jwt.public-key-path");
        }
    }

    /**
     * 为登录用户创建访问令牌，使用RSA私钥签名。
     *
     * @param loginUser         登录用户信息
     * @param sessionId         会话ID
     * @param accessTokenVersion 访问令牌版本号
     * @return 签名后的JWT令牌字符串
     */
    public String createAccessToken(LoginUser loginUser, String sessionId, Integer accessTokenVersion) {
        if (privateKey == null) {
            throw new IllegalStateException("RSA私钥未配置，无法签发token");
        }
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + accessExpireSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim("username", loginUser.getUsername())
                .claim("nickname", loginUser.getNickname())
                .claim("sid", sessionId)
                .claim("typ", CommonConstants.ACCESS_TOKEN_TYPE)
                .claim("av", accessTokenVersion)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 从JWT令牌中解析Claims，使用RSA公钥验证签名。
     *
     * @param token JWT令牌字符串
     * @return 解析后的Claims对象
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从令牌中获取用户ID。
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    /**
     * 从令牌中获取用户名。
     *
     * @param token JWT令牌
     * @return 用户名，不存在时返回null
     */
    public String getUsername(String token) {
        Object username = getClaims(token).get("username");
        return username == null ? null : username.toString();
    }

    /**
     * 从令牌中获取会话ID。
     *
     * @param token JWT令牌
     * @return 会话ID，不存在时返回null
     */
    public String getSessionId(String token) {
        Object sessionId = getClaims(token).get("sid");
        return sessionId == null ? null : sessionId.toString();
    }

    /**
     * 从令牌中获取访问令牌版本号。
     *
     * @param token JWT令牌
     * @return 版本号，不存在时返回null
     */
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

    /**
     * 从令牌中获取令牌类型。
     *
     * @param token JWT令牌
     * @return 令牌类型，不存在时返回null
     */
    public String getTokenType(String token) {
        Object tokenType = getClaims(token).get("typ");
        return tokenType == null ? null : tokenType.toString();
    }

    /**
     * 验证访问令牌的有效性，包括签名验证、令牌类型检查和过期时间检查。
     *
     * @param token JWT令牌
     * @return 令牌有效返回true，否则返回false
     */
    public boolean validateAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = getClaims(token);
            Object tokenType = claims.get("typ");
            return CommonConstants.ACCESS_TOKEN_TYPE.equals(tokenType == null ? null : tokenType.toString())
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 获取访问令牌过期时间（秒）。
     *
     * @return 过期时间
     */
    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    /**
     * 从PEM文件加载RSA私钥，支持PKCS#8和PKCS#1格式。
     *
     * @param path PEM文件路径，支持classpath路径、classpath:前缀、file:前缀和本地绝对路径
     * @return RSA私钥对象
     */
    private PrivateKey loadPrivateKey(String path) {
        String pemContent = readPemContent(path);
        try {
            // 尝试PKCS#8格式（标准Java格式）
            String base64 = extractBase64(pemContent, "PRIVATE KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            // 回退到PKCS#1格式（OpenSSL默认格式），通过Bouncy Castle解析
            try (PEMParser pemParser = new PEMParser(new java.io.StringReader(pemContent))) {
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                Object object = pemParser.readObject();
                if (object instanceof PEMKeyPair) {
                    return converter.getKeyPair((PEMKeyPair) object).getPrivate();
                }
                if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                    return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
                }
                throw new IllegalStateException("不支持的PEM密钥格式: " + object.getClass().getName());
            } catch (Exception ex) {
                throw new IllegalStateException("无法加载RSA私钥: " + path, ex);
            }
        }
    }

    /**
     * 从PEM文件加载RSA公钥。
     *
     * @param path PEM文件路径，支持classpath路径、classpath:前缀、file:前缀和本地绝对路径
     * @return RSA公钥对象
     */
    private PublicKey loadPublicKey(String path) {
        String pemContent = readPemContent(path);
        try {
            String base64 = extractBase64(pemContent, "PUBLIC KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("无法加载RSA公钥: " + path, e);
        }
    }

    /**
     * 从PEM文件中加载RSA密钥对（包含私钥和公钥）。
     *
     * @param path PEM文件路径，支持classpath路径、classpath:前缀、file:前缀和本地绝对路径
     * @return RSA密钥对，加载失败时返回null
     */
    private KeyPair loadKeyPair(String path) {
        String pemContent = readPemContent(path);
        try (PEMParser pemParser = new PEMParser(new java.io.StringReader(pemContent))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();
            if (object instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) object);
            }
            return null;
        } catch (Exception e) {
            log.warn("无法从PEM文件加载密钥对: {}", path, e);
            return null;
        }
    }

    /**
     * 从资源路径中读取PEM文件的完整内容。
     *
     * @param path PEM资源路径
     * @return PEM文件内容字符串
     */
    private String readPemContent(String path) {
        try {
            Resource resource = resolvePemResource(path);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("无法读取PEM文件: " + path, e);
        }
    }

    /**
     * 解析PEM密钥资源路径，默认兼容原有classpath相对路径，同时支持外部文件挂载。
     *
     * @param path 配置中的密钥路径
     * @return Spring资源对象
     */
    private Resource resolvePemResource(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalStateException("PEM文件路径不能为空");
        }
        String trimmedPath = path.trim();
        if (trimmedPath.startsWith("classpath:") || trimmedPath.startsWith("file:")) {
            return RESOURCE_LOADER.getResource(trimmedPath);
        }
        try {
            Path localPath = Paths.get(trimmedPath);
            if (localPath.isAbsolute()) {
                return RESOURCE_LOADER.getResource(localPath.toUri().toString());
            }
        } catch (InvalidPathException ignored) {
            // 非本地文件路径时按classpath资源处理，保持旧配置兼容。
        }
        return RESOURCE_LOADER.getResource("classpath:" + trimmedPath);
    }

    /**
     * 从PEM内容中提取指定类型的Base64编码密钥数据。
     *
     * @param pemContent PEM文件内容
     * @param keyType    密钥类型标识（如"PRIVATE KEY"或"PUBLIC KEY"）
     * @return Base64编码的密钥字符串
     */
    private String extractBase64(String pemContent, String keyType) {
        StringBuilder sb = new StringBuilder();
        boolean inKey = false;
        for (String line : pemContent.split("\n")) {
            if (line.contains("BEGIN " + keyType)) {
                inKey = true;
                continue;
            }
            if (line.contains("END " + keyType)) {
                break;
            }
            if (inKey) {
                sb.append(line.trim());
            }
        }
        return sb.toString();
    }
}
