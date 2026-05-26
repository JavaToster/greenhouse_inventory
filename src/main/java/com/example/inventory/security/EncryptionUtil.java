package com.example.inventory.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {
    @Value("${spring.security.devices.secret.encryption.key}")
    private String key;

    @Value("${spring.security.devices.secret.encryption.algorithm}")
    private String algorithm;

    @Value("${spring.security.devices.secret.encryption.iv-length-bytes}")
    private int ivLengthBytes;

    @Value("${spring.security.devices.secret.encryption.tag-length-bits}")
    private int tagLengthBits;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String raw){
        try{
            SecretKey secretKey = buildSecretKey();
            byte[] iv = new byte[ivLengthBytes];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(tagLengthBits, iv));
            byte[] cipherText = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encrypted){
        try {
            SecretKey secretKey = buildSecretKey();
            byte[] data = Base64.getDecoder().decode(encrypted);
            ByteBuffer buffer = ByteBuffer.wrap(data);

            byte[] iv = new byte[ivLengthBytes];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(tagLengthBits, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey buildSecretKey() {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
