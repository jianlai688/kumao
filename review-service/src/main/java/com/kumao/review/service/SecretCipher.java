package com.kumao.review.service;

import com.kumao.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 模型 API Key 的对称加密与掩码。
 *
 * <p>数据库只保存 AES-GCM 密文，接口与日志只使用掩码。加密口令来自
 * {@code AI_CONFIG_SECRET}，更换口令会导致已保存密钥无法解密，需要重新录入。</p>
 */
@Component
public class SecretCipher {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(@Value("${kumao.ai.config-secret:kumao-ai-config-dev-secret-change-me}") String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化模型密钥加密器失败", e);
        }
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new BizException("保存模型密钥失败");
        }
    }

    public String decrypt(String storedValue) {
        if (!StringUtils.hasText(storedValue)) return null;
        // 兼容早期直接写入的明文配置。
        if (!storedValue.startsWith(PREFIX)) return storedValue;
        try {
            byte[] payload = Base64.getDecoder().decode(storedValue.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException("模型密钥无法解密，请重新录入 API Key");
        }
    }

    /** 只暴露首尾少量字符，避免管理员在页面上复制完整密钥。 */
    public String mask(String plainText) {
        if (!StringUtils.hasText(plainText)) return null;
        String value = plainText.trim();
        if (value.length() <= 8) return "****";
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
