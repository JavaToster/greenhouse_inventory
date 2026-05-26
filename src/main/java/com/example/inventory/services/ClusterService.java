package com.example.inventory.services;

import com.example.inventory.DTO.cluster.ClustersWithUserDetailsDTO;
import com.example.inventory.DTO.user.UserInfoBatchDTO;
import com.example.inventory.DTO.user.UserInfoDTO;
import com.example.inventory.security.UserPrincipal;
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
        log.info("Registering new cluster '{}' for owner {}", registerNewClusterDTO.getName(), registerNewClusterDTO.getOwnerId());

        Cluster cluster = new Cluster();
        cluster.setName(registerNewClusterDTO.getName());
        cluster.setOwnerId(registerNewClusterDTO.getOwnerId());
        clusterStore.save(cluster);

        List<Device> devicesOfCluster = deviceService.createNewDevices(cluster, registerNewClusterDTO.getDevicesCount());
        cluster.setDevices(devicesOfCluster);
        deviceStore.saveAll(devicesOfCluster);

        List<ClusterDevicesTempSecretsDTO> tempSecrets = devicesOfCluster.stream()
                .map(d -> new ClusterDevicesTempSecretsDTO(d.getId(), d.getRawSecret()))
                .toList();

        UUID secretsToken = UUID.randomUUID();
        redisRepository.saveWithTTLInMinutes(redisKeyCreator.createClusterDevicesTempSecretsKey(secretsToken), new DevicesSecretWrapper(cluster.getId(), tempSecrets), CLUSTER_DEVICES_TEMP_SECRETS_TTL_IN_MINUTES);

        log.info("Cluster {} registered successfully", cluster.getId());
        return new DevicesTempSecretDTO(cluster.getId().toString(), secretsToken.toString());
    }

    private List<ClusterInfoDTO> findAllClusters() {
        return convertor.convertToClusterInfoDTO(clusterStore.findAll());
    }

    private List<ClusterInfoDTO> findByOwnerId(long ownerId) {
        return convertor.convertToClusterInfoDTO(clusterStore.findByOwner(ownerId));
    }

    @Transactional
    public void addWorkerToCluster(long ownerId, UUID clusterId, long workerId) throws BadRequestException, AccessDeniedException {
        log.info("Adding worker {} to cluster {}", workerId, clusterId);
        Cluster cluster = clusterStore.findById(clusterId);
        checkOwner(cluster, ownerId);

        if (cluster.getWorkerIds().contains(workerId)) {
            throw new BadRequestException("User is already a worker in this cluster");
        }
        
        isWorker(userClient.getUser(workerId));

        cluster.addWorker(workerId);
        clusterStore.save(cluster);
        log.info("Worker {} added to cluster {}", workerId, clusterId);
    }

    @Transactional
    public void removeWorkerFromCluster(long ownerId, UUID clusterId, long workerId) throws AccessDeniedException, BadRequestException {
        log.info("Removing worker {} from cluster {}", workerId, clusterId);
        Cluster cluster = clusterStore.findById(clusterId);
        checkOwner(cluster, ownerId);

        if (!cluster.getWorkerIds().contains(workerId)) {
            throw new BadRequestException("User is not a worker in this cluster");
        }

        cluster.removeWorker(workerId);
        clusterStore.save(cluster);
        log.info("Worker {} removed from cluster {}", workerId, clusterId);
    }

    private List<ClusterInfoDTO> findByWorker(long workerId) {
        return convertor.convertToClusterInfoDTO(clusterStore.findByWorker(workerId));
    }

    private void checkOwner(Cluster cluster, long ownerId){
        if(cluster.getOwnerId() != ownerId){
            throw new AccessDeniedException("User is not the owner of this cluster");
        }
    }

    private void isWorker(UserInfoDTO worker) throws BadRequestException {
        if (worker.getRole() != Role.ROLE_WORKER){
            throw new BadRequestException("User is not a worker");
        }
    }

    private List<ClusterInfoDTO> findClusters(UserPrincipal userPrincipal, Long ownerId, Long workerId) {
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
            default -> throw new AccessDeniedException("You can't to get clusters information");
        };
    }

    public List<ClustersWithUserDetailsDTO> findClustersWithUserDetails(UserPrincipal principal, Long ownerId, Long workerId){
        List<ClusterInfoDTO> clustersWithoutUserInfo = findClusters(principal, ownerId, workerId);

        if(clustersWithoutUserInfo.isEmpty()){
            Collections.emptyList();
        }

        Set<Long> ownerAndWorkersIds = clustersWithoutUserInfo.stream()
                .flatMap(c -> {
                    Set<Long> ids = new HashSet<>(c.getWorkerIds());
                    if(c.getOwnerId() != null) ids.add(c.getOwnerId());
                    return ids.stream();
                }).collect(Collectors.toSet());

        List<UserInfoDTO> usersInfo = fetchUsersInBatches(ownerAndWorkersIds);

        Map<Long, UserInfoDTO> userInfoDTOMap = usersInfo.stream()
                .collect(Collectors.toMap(UserInfoDTO::getTelegramId, u -> u));

        return clustersWithoutUserInfo.stream()
                .map(cluster -> {
                    ClustersWithUserDetailsDTO clusterWithDetails = new ClustersWithUserDetailsDTO();
                    clusterWithDetails.setId(cluster.getId());
                    clusterWithDetails.setName(cluster.getName());
                    clusterWithDetails.setDescription(cluster.getDescription());
                    clusterWithDetails.setDevices(cluster.getDevices());

                    clusterWithDetails.setOwner(userInfoDTOMap.get(cluster.getOwnerId()));

                    List<UserInfoDTO> workers = cluster.getWorkerIds().stream()
                            .map(userInfoDTOMap::get)
                            .filter(Objects::nonNull)
                            .toList();

                    clusterWithDetails.setWorkers(workers);
                    return clusterWithDetails;
                }).toList();
    }
    private List<UserInfoDTO> fetchUsersInBatches(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> allUserIdsList = new ArrayList<>(userIds);
        List<UserInfoDTO> accumulatedUsers = new ArrayList<>();
        int batchSize = 500;

        for (int i = 0; i < allUserIdsList.size(); i += batchSize) {
            List<Long> subList = allUserIdsList.subList(i, Math.min(i + batchSize, allUserIdsList.size()));

            List<UserInfoDTO> partitionResult = userClient.getUsers(new UserInfoBatchDTO(new HashSet<>(subList)));

            if (partitionResult != null) {
                accumulatedUsers.addAll(partitionResult);
            }
        }

        return accumulatedUsers;
    }
}
