package com.example.inventory.services;

import com.example.inventory.DTO.device.ClusterDevicesTempSecretsDTO;
import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.DTO.device.DevicesSecretWrapper;
import com.example.inventory.security.jwt.DeviceJwtTokenIssuer;
import com.example.inventory.store.ClusterStore;
import com.example.inventory.store.DeviceStore;
import com.example.inventory.DTO.auth.DeviceAuthRequestDTO;
import com.example.inventory.models.Cluster;
import com.example.inventory.models.Device;
import com.example.inventory.repositories.redis.RedisRepository;
import com.example.inventory.security.EncryptionUtil;
import com.example.inventory.util.Convertor;
import com.example.inventory.util.enums.DeviceStatus;
import com.example.inventory.util.enums.Role;
import com.example.inventory.util.redis.RedisKeyCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {
    private final DeviceStore deviceStore;
    private static final long CHALLENGE_TTL_IN_SECONDS = 30;
    private static final long ATTEMPT_TTL_IN_MINUTES = 5;
    private static final int MAX_AUTH_ATTEMPTS = 5;
    private static final int DEVICE_BLOCK_TTL_IN_MINUTES = 5;
    private final RedisRepository redisRepository;
    private final RedisKeyCreator redisKeyCreator;
    private final EncryptionUtil encryptionUtil;
    private final DeviceJwtTokenIssuer deviceJwtTokenIssuer;
    private final ClusterStore clusterStore;
    private final Convertor convertor;

    public String generateChallenge(UUID deviceId) throws BadRequestException {
        if (!deviceStore.isExist(deviceId)){
            log.warn("Challenge generation failed: device id={} does not exist in store", deviceId);
            throw new BadRequestException("Device not exist!");
        }

        log.info("Request to generate challenge for device id={}", deviceId);
        String challenge = UUID.randomUUID().toString();
        String redisKey = redisKeyCreator.createChallengeKey(deviceId);

        log.debug("Saving challenge to Redis with key='{}', TTL={} sec", redisKey, CHALLENGE_TTL_IN_SECONDS);
        redisRepository.saveWithTTLInSeconds(redisKey, challenge, CHALLENGE_TTL_IN_SECONDS);

        return challenge;
    }

    public String verify(DeviceAuthRequestDTO deviceAuthRequestDTO) {
        checkDeviceIsBlocked(deviceAuthRequestDTO.deviceId());

        log.info("Authentication attempt for device id={}", deviceAuthRequestDTO.deviceId());
        Device device = deviceStore.findById(deviceAuthRequestDTO.deviceId());

        if (device.getStatus() != DeviceStatus.ACTIVE) {
            log.warn("Auth failed for device id={}: device is not active", device.getId());

            throw new BadCredentialsException("Device is not active");
        }

        String deviceChallengeKey = redisKeyCreator.createChallengeKey(deviceAuthRequestDTO.deviceId());
        String issuedChallenge = redisRepository.getAndDelete(deviceChallengeKey, String.class);

        checkChallenge(issuedChallenge, deviceAuthRequestDTO);

        log.debug("Decrypting secret for signature validation on device id={}", device.getId());
        try{
            validateSignature(deviceAuthRequestDTO.signature(), issuedChallenge, encryptionUtil.decrypt(device.getSecret()));
        }catch(BadCredentialsException exc){
            handleFailedAttempt(device.getId());
            throw exc;
        }
        clearAuthAttempts(device.getId());

        log.info("Device id={} successfully authenticated", device.getId());
        return deviceJwtTokenIssuer.generate(device.getId(), device.getCluster().getId());
    }

    private void validateSignature(String clientSig, String data, String secret) {
        String expected = hmacSha256(data, secret);

        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                clientSig.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Signature validation failed for client signature format check.");
            throw new BadCredentialsException("Invalid signature");
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Crypto algorithm failure during HMAC-SHA256 computation", e);
            throw new RuntimeException(e);
        }
    }

    public List<Device> createNewDevices(Cluster cluster, int count) {
        log.info("Generating a batch of {} new devices for cluster id={}", count, cluster.getId());
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

        log.debug("Initialized single device id={} for cluster id={} with status={}",
                device.getId(), cluster.getId(), device.getStatus());
        return device;
    }

    private String generateSecret() {
        log.debug("Generating 32-byte secure random secret token");
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Transactional
    public UUID remove(UUID deviceId) {
        log.info("Request to remove device id={}", deviceId);
        deviceStore.remove(deviceId);
        log.info("Device id={} successfully removed from store", deviceId);
        return deviceId;
    }

    @Transactional
    public List<ClusterDevicesTempSecretsDTO> getRawKeysAndActivate(UUID token) {
        log.info("Request to activate cluster devices using activation token='{}'", token);
        String redisKey = redisKeyCreator.createClusterDevicesTempSecretsKey(token);

        DevicesSecretWrapper wrapper = redisRepository.getAndDelete(redisKey, DevicesSecretWrapper.class);

        if (wrapper == null || wrapper.secrets() == null) {
            log.warn("Activation failed: token='{}' is invalid, expired or already processed", token);
            throw new AccessDeniedException("Secrets not found or already activated.");
        }

        log.debug("Updating database status to ACTIVE for all devices in cluster id={}", wrapper.clusterId());
        deviceStore.updateStatusByClusterId(wrapper.clusterId(), DeviceStatus.ACTIVE);

        log.info("All devices for cluster id={} successfully activated", wrapper.clusterId());
        return wrapper.secrets();
    }

    public List<DeviceInfoDTO> findByClusterAndCheckOwner(UUID clusterId, long userId, Role role) {
        log.info("Fetching devices for cluster id=[{}] requested by user telegramId={}", clusterId, userId);
        Cluster cluster = clusterStore.findById(clusterId);

        checkOwner(cluster, userId, role);

        Set<Device> devices = cluster.getDevices();
        log.debug("Found {} devices for cluster id=[{}]", devices.size(), clusterId);

        return convertor.convertToDeviceInfoDTO(devices);
    }

    private void checkOwner(Cluster cluster, long userId, Role role) {
        if (role == Role.ROLE_ADMIN){
            return;
        }

        if (Objects.equals(cluster.getOwnerId(), userId)){
            log.warn("Security Alert: User telegramId={} attempted to access cluster id=[{}] but is NOT the owner", userId, cluster.getId());
            throw new AccessDeniedException("You aren't owner this cluster!");
        }
        log.debug("Ownership verified for user telegramId={} on cluster id=[{}]", userId, cluster.getId());
    }

    private void handleFailedAttempt(UUID deviceId) {
        String key = redisKeyCreator.createDeviceAuthAttemptKey(deviceId);

        long attempts = redisRepository.increment(key);

        if (attempts == 1) {
            redisRepository.setExpireInMinutes(key, ATTEMPT_TTL_IN_MINUTES);
        }

        if (attempts == MAX_AUTH_ATTEMPTS) {
            redisRepository.saveWithTTLInMinutes(
                    redisKeyCreator.createDeviceBlockedKey(deviceId),
                    true,
                    DEVICE_BLOCK_TTL_IN_MINUTES
            );
            clearAuthAttempts(deviceId);
            log.warn("Device {} blocked for {} minutes after {} failed attempts",
                    deviceId,
                    DEVICE_BLOCK_TTL_IN_MINUTES,
                    MAX_AUTH_ATTEMPTS);
        }
    }

    private void clearAuthAttempts(UUID deviceId) {
        redisRepository.remove(redisKeyCreator.createDeviceAuthAttemptKey(deviceId));
    }

    private void checkDeviceIsBlocked(UUID deviceId) {
        if (redisRepository.exists(redisKeyCreator.createDeviceBlockedKey(deviceId))) {
            log.warn("Authentication attempt for device id={} blocked due to too many failed attempts", deviceId);
            throw new BadCredentialsException("Too many authentication attempts. Try again later.");
        }
    }

    private void checkChallenge(String issuedChallenge, DeviceAuthRequestDTO deviceAuthRequestDTO){
        if (issuedChallenge == null) {
            log.warn("Auth failed for device id={}: challenge expired or never issued", deviceAuthRequestDTO.deviceId());
            throw new BadCredentialsException("No challenge issued or expired");
        }

        if (!issuedChallenge.equals(deviceAuthRequestDTO.challenge())) {
            log.warn("Auth failed for device id={}: challenge mismatch", deviceAuthRequestDTO.deviceId());
            throw new BadCredentialsException("Challenge mismatch");
        }
    }
}