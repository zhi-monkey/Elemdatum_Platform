package org.dlut.adv.mineai.core.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class RtspPasswordCryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private RtspPasswordCryptoUtil() {
    }

    public static String encrypt(String plainText, String aesKey) {
        try {
            byte[] keyBytes = readKeyBytes(aesKey);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("RTSP密码加密失败", e);
        }
    }

    public static String decrypt(String cipherTextBase64, String aesKey) {
        try {
            byte[] all = Base64.getDecoder().decode(cipherTextBase64);
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(all, IV_LENGTH, all.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(readKeyBytes(aesKey), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("RTSP密码解密失败", e);
        }
    }

    private static byte[] readKeyBytes(String aesKey) {
        if (aesKey == null || aesKey.trim().isEmpty()) {
            throw new IllegalStateException("缺少配置项 mineai.rtsp.password-aes-key");
        }
        String keyText = aesKey.trim();
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyText);
        } catch (IllegalArgumentException e) {
            keyBytes = keyText.getBytes(StandardCharsets.UTF_8);
        }
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalStateException("AES密钥长度必须为16/24/32字节（支持Base64或明文）");
        }
        return keyBytes;
    }
}
