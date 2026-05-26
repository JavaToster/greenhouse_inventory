package com.example.inventory.services;

import com.example.inventory.DTO.device.ClusterDevicesTempSecretsDTO;
import com.example.inventory.DTO.device.DevicesSecretWrapper;
import com.example.inventory.store.DeviceStore;
import com.example.inventory.DTO.auth.DeviceAuthRequestDTO;
import com.example.inventory.models.Cluster;
import com.example.inventory.models.Device;
import com.example.inventory.repositories.redis.RedisRepository;
import com.example.inventory.security.EncryptionUtil;
import com.example.inventory.util.enums.DeviceStatus;
import com.example.inventory.util.redis.RedisKeyCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.inventory.services.token.DeviceTokenService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {
    private final DeviceStore deviceStore;
    private static final long CHALLENGE_TTL_IN_SECONDS = 30;
    private final RedisRepository redisRepository;
    private final RedisKeyCreator redisKeyCreator;
    private final EncryptionUtil encryptionUtil;
    private final DeviceTokenService deviceTokenService;

    public String generateChallenge(String deviceId) {
        log.debug("Generating challenge for device {}", deviceId);
        String challenge = UUID.randomUUID().toString();
        redisRepository.saveWithTTLInSeconds(redisKeyCreator.createChallengeKey(deviceId), challenge, CHALLENGE_TTL_IN_SECONDS);
        return challenge;
    }

    public String verify(DeviceAuthRequestDTO deviceAuthRequestDTO){
        log.info("Authentication attempt for device {}", deviceAuthRequestDTO.getDeviceId());
        Device device = deviceStore.findById(deviceAuthRequestDTO.getDeviceId());

        String issuedChallenge = redisRepository.findByKey(redisKeyCreator.createChallengeKey(deviceAuthRequestDTO.getDeviceId().toString()), String.class);

        if(issuedChallenge == null){
            throw new BadCredentialsException("No challenge issued or expired");
        }

        if(!issuedChallenge.equals(deviceAuthRequestDTO.getChallenge())){
            throw new BadCredentialsException("Challenge mismatch");
        }

        validateSignature(deviceAuthRequestDTO.getSignature(), issuedChallenge, encryptionUtil.decrypt(device.getSecret()));

        redisRepository.remove(redisKeyCreator.createChallengeKey(deviceAuthRequestDTO.getDeviceId().toString()));
        log.info("Device {} successfully authenticated", device.getId());

        return deviceTokenService.generate(device.getId(), device.getCluster().getId());
    }

    private void validateSignature(String clientSig, String data, String secret){
        String expected = hmacSha256(data, secret);
        if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                clientSig.getBytes(StandardCharsets.UTF_8))){
            throw new BadCredentialsException("Invalid signature");
        }
    }

    private String hmacSha256(String data, String secret){
        try{
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Device> createNewDevices(Cluster cluster, int count){
        log.info("Creating {} new devices for cluster {}", count, cluster.getId());
        return IntStream.range(0, count)
                .mapToObj(i -> createNewDevice(cluster))
                .toList();
    }

    public Device createNewDevice(Cluster cluster) {
        Device device = new Device();
        device.setId(UUID.randomUUID());
        device.setCluster(cluster);
        device.setStatus(DeviceStatus.PENDING_ACTIVAT);

        String rawSecret = generateSecret();
        device.setSecret(encryptionUtil.encrypt(rawSecret));
        device.setRawSecret(rawSecret);

        return device;
    }

    private String generateSecret(){
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Transactional
    public UUID remove(UUID deviceId) {
        log.info("Removing device {}", deviceId);
        deviceStore.remove(deviceId);
        return deviceId;
    }

    @Transactional
    public List<ClusterDevicesTempSecretsDTO> getRawKeysAndActivate(UUID token) {
        log.info("Activating devices with token {}", token);
        DevicesSecretWrapper wrapper = redisRepository.findByKey(redisKeyCreator.createClusterDevicesTempSecretsKey(token), DevicesSecretWrapper.class);

        if (wrapper == null || wrapper.getSecrets() == null) {
            throw new AccessDeniedException("Secrets not found or already activated.");
        }

        redisRepository.remove(redisKeyCreator.createClusterDevicesTempSecretsKey(token));
        deviceStore.updateStatusByClusterId(wrapper.getClusterId(), DeviceStatus.ACTIVE);

        log.info("Devices for cluster {} activated", wrapper.getClusterId());
        return wrapper.getSecrets();
    }
}
