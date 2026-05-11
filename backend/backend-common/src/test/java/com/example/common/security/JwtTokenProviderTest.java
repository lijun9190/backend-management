package com.example.common.security;

import com.example.common.model.security.LoginUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * JWT令牌提供者测试，使用动态生成的RSA密钥对验证RS256非对称加密功能。
 */
class JwtTokenProviderTest {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private JwtTokenProvider provider;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // 动态生成RSA 2048密钥对用于测试
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        this.provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "privateKey", privateKey);
        ReflectionTestUtils.setField(provider, "publicKey", publicKey);
        ReflectionTestUtils.setField(provider, "accessExpireSeconds", 3600L);
    }

    @Test
    void shouldCreateAndParseAccessToken() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        loginUser.setNickname("超级管理员");

        String token = provider.createAccessToken(loginUser, "session-1", 3);

        Assertions.assertNotNull(token);
        Assertions.assertTrue(provider.validateAccessToken(token));
        Assertions.assertEquals(Long.valueOf(1L), provider.getUserId(token));
        Assertions.assertEquals("admin", provider.getUsername(token));
        Assertions.assertEquals("session-1", provider.getSessionId(token));
        Assertions.assertEquals(Integer.valueOf(3), provider.getAccessTokenVersion(token));
        Assertions.assertEquals("access", provider.getTokenType(token));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() throws Exception {
        // 使用另一组密钥签名的token，当前公钥验证应失败
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair otherKeyPair = gen.generateKeyPair();

        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "privateKey", otherKeyPair.getPrivate());
        ReflectionTestUtils.setField(otherProvider, "publicKey", otherKeyPair.getPublic());
        ReflectionTestUtils.setField(otherProvider, "accessExpireSeconds", 3600L);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");

        String tokenFromOther = otherProvider.createAccessToken(loginUser, "s1", 1);

        // 用当前provider的公钥验证另一组密钥签发的token，应该失败
        Assertions.assertFalse(provider.validateAccessToken(tokenFromOther));
    }

    @Test
    void shouldRejectEmptyAndNullToken() {
        Assertions.assertFalse(provider.validateAccessToken(null));
        Assertions.assertFalse(provider.validateAccessToken(""));
        Assertions.assertFalse(provider.validateAccessToken("   "));
    }

    @Test
    void shouldRejectTamperedToken() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");

        String token = provider.createAccessToken(loginUser, "session-1", 1);

        // 篡改token内容（修改中间一段字符）
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            String tampered = parts[0] + "." + parts[1] + "X" + "." + parts[2];
            Assertions.assertFalse(provider.validateAccessToken(tampered));
        }
    }

    /**
     * 验证JWT组件可以从外部文件路径加载RSA密钥，支持生产环境通过挂载文件提供私钥。
     */
    @Test
    void shouldLoadKeysFromExternalFilePath() throws Exception {
        Path privateKeyFile = tempDir.resolve("jwt-signing-key.pem");
        Path publicKeyFile = tempDir.resolve("jwt-public-key.pem");
        writePem(privateKeyFile, "PRIVATE KEY", privateKey);
        writePem(publicKeyFile, "PUBLIC KEY", publicKey);

        JwtTokenProvider fileProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(fileProvider, "privateKeyPath", privateKeyFile.toUri().toString());
        ReflectionTestUtils.setField(fileProvider, "publicKeyPath", publicKeyFile.toUri().toString());
        ReflectionTestUtils.setField(fileProvider, "accessExpireSeconds", 3600L);

        fileProvider.init();

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("admin");
        String token = fileProvider.createAccessToken(loginUser, "session-1", 1);

        Assertions.assertTrue(fileProvider.validateAccessToken(token));
        Assertions.assertEquals(Long.valueOf(1L), fileProvider.getUserId(token));
    }

    /**
     * 将测试生成的RSA密钥写成PEM格式，便于覆盖真实文件加载链路。
     *
     * @param path  PEM文件路径
     * @param label PEM头尾标识
     * @param key   RSA密钥对象
     */
    private void writePem(Path path, String label, Key key) throws Exception {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(key.getEncoded());
        String content = "-----BEGIN " + label + "-----\n"
                + encoded
                + "\n-----END " + label + "-----\n";
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
