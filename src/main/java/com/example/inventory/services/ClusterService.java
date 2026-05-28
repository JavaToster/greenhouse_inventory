package com.example.inventory.services;

import com.example.inventory.DTO.cluster.ClustersWithUserDetailsDTO;
import com.example.inventory.DTO.user.UserInfoBatchDTO;
import com.example.inventory.DTO.user.UserInfoDTO;
import com.example.inventory.security.principals.UserPrincipal;
import com.example.inventory.clients.UserClient;
import com.example.inventory.store.DeviceStore;
import com.example.inventory.store.ClusterStore;
import com.example.inventory.DTO.cluster.ClusterInfoDTO;
import com.example.inventory.DTO.cluster.RegisterNewClusterDTO;
import com.example.inventory.DTO.device.ClusterDevicesTempSecretsDTO;
import com.example.inventory.DTO.device.DevicesSecretWrapper;
import com.example.inventory.DTO.device.DevicesTempSecretDTO;
import com.example.inventory.models.Cluster;
import com.example.inventory.models.Device;
import com.example.inventory.repositories.redis.RedisRepository;
import com.example.inventory.util.Convertor;
import com.example.inventory.util.enums.Role;
import com.example.inventory.util.redis.RedisKeyCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {
    private final ClusterStore clusterStore;
    private final DeviceStore deviceStore;
    private final DeviceService deviceService;
    private final Convertor convertor;
    private final RedisRepository redisRepository;
    private final RedisKeyCreator redisKeyCreator;
    private final UserClient userClient;

    private static final int CLUSTER_DEVICES_TEMP_SECRETS_TTL_IN_MINUTES = 5;

    @Transactional
    public DevicesTempSecretDTO registerNewCluster(RegisterNewClusterDTO registerNewClusterDTO) {
        log.info("Registering new cluster '{}' for owner id={}", registerNewClusterDTO.name(), registerNewClusterDTO.ownerId());

        Cluster cluster = new Cluster();
        cluster.setName(registerNewClusterDTO.name());
        cluster.setOwnerId(registerNewClusterDTO.ownerId());
        clusterStore.save(cluster);

        log.debug("Creating {} initial devices for cluster id={}", registerNewClusterDTO.devicesCount(), cluster.getId());
        List<Device> devicesOfCluster = deviceService.createNewDevices(cluster, registerNewClusterDTO.devicesCount());
        cluster.setDevices(devicesOfCluster);
        deviceStore.saveAll(devicesOfCluster);

        List<ClusterDevicesTempSecretsDTO> tempSecrets = devicesOfCluster.stream()
                .map(d -> new ClusterDevicesTempSecretsDTO(d.getId(), d.getRawSecret()))
                .toList();

        UUID secretsToken = UUID.randomUUID();
        String redisKey = redisKeyCreator.createClusterDevicesTempSecretsKey(secretsToken);

        log.debug("Saving temporary secrets to Redis with key='{}', TTL={} min", redisKey, CLUSTER_DEVICES_TEMP_SECRETS_TTL_IN_MINUTES);
        redisRepository.saveWithTTLInMinutes(redisKey, new DevicesSecretWrapper(cluster.getId(), tempSecrets), CLUSTER_DEVICES_TEMP_SECRETS_TTL_IN_MINUTES);

        log.info("Cluster id={} registered successfully with {} devices", cluster.getId(), devicesOfCluster.size());
        return new DevicesTempSecretDTO(cluster.getId().toString(), secretsToken.toString());
    }

    private List<ClusterInfoDTO> findAllClusters() {
        log.debug("Fetching all clusters from store");
        return convertor.convertToClusterInfoDTO(clusterStore.findAll());
    }

    private List<ClusterInfoDTO> findByOwnerId(long ownerId) {
        log.debug("Fetching clusters for owner id={}", ownerId);
        return convertor.convertToClusterInfoDTO(clusterStore.findByOwner(ownerId));
    }

    @Transactional
    public void addWorkerToCluster(long ownerId, UUID clusterId, long workerId) throws BadRequestException, AccessDeniedException {
        log.info("Attempt to add worker id={} to cluster id={} by owner id={}", workerId, clusterId, ownerId);
        Cluster cluster = clusterStore.findById(clusterId);
        checkOwner(cluster, ownerId);

        if (cluster.getWorkerIds().contains(workerId)) {
            log.warn("Failed to add worker: user id={} is already a worker in cluster id={}", workerId, clusterId);
            throw new BadRequestException("User is already a worker in this cluster");
        }

        log.debug("Validating user id={} via UserClient", workerId);
        isWorker(userClient.getUser(workerId));

        cluster.addWorker(workerId);
        clusterStore.save(cluster);
        log.info("Worker id={} successfully added to cluster id={}", workerId, clusterId);
    }

    @Transactional
    public void removeWorkerFromCluster(long ownerId, UUID clusterId, long workerId) throws AccessDeniedException, BadRequestException {
        log.info("Attempt to remove worker id={} from cluster id={} by owner id={}", workerId, clusterId, ownerId);
        Cluster cluster = clusterStore.findById(clusterId);
        checkOwner(cluster, ownerId);

        if (!cluster.getWorkerIds().contains(workerId)) {
            log.warn("Failed to remove worker: user id={} is not a worker in cluster id={}", workerId, clusterId);
            throw new BadRequestException("User is not a worker in this cluster");
        }

        cluster.removeWorker(workerId);
        clusterStore.save(cluster);
        log.info("Worker id={} successfully removed from cluster id={}", workerId, clusterId);
    }

    private List<ClusterInfoDTO> findByWorker(long workerId) {
        log.debug("Fetching clusters for worker id={}", workerId);
        return convertor.convertToClusterInfoDTO(clusterStore.findByWorker(workerId));
    }

    private void checkOwner(Cluster cluster, long ownerId){
        if(cluster.getOwnerId() != ownerId){
            log.warn("Access denied: user id={} is not the owner of cluster id={} (actual owner id={})", ownerId, cluster.getId(), cluster.getOwnerId());
            throw new AccessDeniedException("User is not the owner of this cluster");
        }
    }

    private void isWorker(UserInfoDTO worker) throws BadRequestException {
        if (worker.role() != Role.ROLE_WORKER){
            log.warn("Validation failed: user id={} has role={}, expected ROLE_WORKER", worker.telegramId(), worker.role());
            throw new BadRequestException("User is not a worker");
        }
    }

    private List<ClusterInfoDTO> findClusters(UserPrincipal userPrincipal, Long ownerId, Long workerId) {
        log.debug("Filtering clusters by switch-hierarchy. Principal role={}, telegramId={}", userPrincipal.role(), userPrincipal.telegramId());
        return switch (userPrincipal.role()){
            case ROLE_ADMIN -> {
                if(ownerId != null){
                    yield  findByOwnerId(ownerId);
                }else if(workerId != null){
                    yield  findByWorker(workerId);
                }else{
                    yield  findAllClusters();
                }
            }
            case ROLE_OWNER -> findByOwnerId(userPrincipal.telegramId());
            case ROLE_WORKER -> findByWorker(userPrincipal.telegramId());
            default -> {
                log.warn("Access denied: role={} cannot access clusters information", userPrincipal.role());
                throw new AccessDeniedException("You can't to get clusters information");
            }
        };
    }

    public List<ClustersWithUserDetailsDTO> findClustersWithUserDetails(UserPrincipal principal, Long ownerId, Long workerId){
        log.info("Request to fetch clusters with detailed user info by user id={}, role={}", principal.telegramId(), principal.role());
        List<ClusterInfoDTO> clustersWithoutUserInfo = findClusters(principal, ownerId, workerId);

        if(clustersWithoutUserInfo.isEmpty()){
            log.debug("No clusters found for the given criteria");
            return Collections.emptyList(); // Исправлен пропущенный return
        }

        Set<Long> ownerAndWorkersIds = clustersWithoutUserInfo.stream()
                .flatMap(c -> {
                    Set<Long> ids = new HashSet<>(c.workerIds());
                    if(c.ownerId() != null) ids.add(c.ownerId());
                    return ids.stream();
                }).collect(Collectors.toSet());

        log.debug("Aggregated {} unique user IDs to fetch from User-Service", ownerAndWorkersIds.size());
        List<UserInfoDTO> usersInfo = fetchUsersInBatches(ownerAndWorkersIds);

        Map<Long, UserInfoDTO> userInfoDTOMap = usersInfo.stream()
                .collect(Collectors.toMap(UserInfoDTO::telegramId, u -> u));

        log.debug("Mapping {} clusters to rich DTOs with users details", clustersWithoutUserInfo.size());
        return clustersWithoutUserInfo.stream()
                .map(cluster -> {
                    List<UserInfoDTO> workers = cluster.workerIds().stream()
                            .map(userInfoDTOMap::get)
                            .filter(Objects::nonNull)
                            .toList();

                    return new ClustersWithUserDetailsDTO(
                            cluster.id(),
                            cluster.name(),
                            cluster.description(),
                            userInfoDTOMap.get(cluster.ownerId()),
                            workers,
                            cluster.devices()
                    );
                }).toList();
    }

    private List<UserInfoDTO> fetchUsersInBatches(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.debug("User IDs set is empty, skipping batch fetch");
            return Collections.emptyList();
        }

        List<Long> allUserIdsList = new ArrayList<>(userIds);
        List<UserInfoDTO> accumulatedUsers = new ArrayList<>();
        int batchSize = 500;

        log.debug("Starting partitioned user fetching. Total IDs: {}, Batch size: {}", allUserIdsList.size(), batchSize);
        for (int i = 0; i < allUserIdsList.size(); i += batchSize) {
            List<Long> subList = allUserIdsList.subList(i, Math.min(i + batchSize, allUserIdsList.size()));

            log.debug("Fetching partition chunk: index range [{} - {}]", i, i + subList.size());
            List<UserInfoDTO> partitionResult = userClient.getUsers(new UserInfoBatchDTO(new HashSet<>(subList)));

            if (partitionResult != null) {
                log.debug("Received {} users from current partition chunk", partitionResult.size());
                accumulatedUsers.addAll(partitionResult);
            } else {
                log.warn("Received null response from UserClient for partition chunk range [{} - {}]", i, i + subList.size());
            }
        }

        log.debug("Successfully accumulated {} total user detailed objects from external client", accumulatedUsers.size());
        return accumulatedUsers;
    }
}
