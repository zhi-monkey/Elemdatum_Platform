package org.dlut.adv.mineai.core.utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaCryptoUtil {

    private static final String ALGORITHM = "RSA";

    private RsaCryptoUtil() {
    }

    public static String decryptByPrivateKey(String cipherTextBase64, String privateKeyBase64) {
        try {
            byte[] inputBytes = Base64.getDecoder().decode(cipherTextBase64.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance(ALGORITHM)
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(inputBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("RSA私钥解密失败", e);
        }
    }

    public static String encryptByPublicKey(String plainText, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("RSA公钥加密失败", e);
        }
    }
}
