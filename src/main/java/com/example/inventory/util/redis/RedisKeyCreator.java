package com.example.inventory.util.redis;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisKeyCreator {
    private static final String CHALLENGE_PREFIX = "challenge:";
    private static final String CLUSTER_DEVICES_TEMP_SECRETS_PREFIX = "cluster-devices-temp-secrets-prefix:";

    public String createChallengeKey(String challengeId){
        return CHALLENGE_PREFIX+challengeId;
    }

    public String createClusterDevicesTempSecretsKey(UUID id) {
        return CLUSTER_DEVICES_TEMP_SECRETS_PREFIX+id;
    }
}
