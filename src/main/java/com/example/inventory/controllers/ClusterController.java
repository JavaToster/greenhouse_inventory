package com.example.inventory.controllers;

import com.example.inventory.DTO.cluster.ClustersWithUserDetailsDTO;
import com.example.inventory.DTO.cluster.RegisterNewClusterDTO;
import com.example.inventory.DTO.cluster.WorkerAssigmentDTO;
import com.example.inventory.DTO.device.DevicesTempSecretDTO;
import com.example.inventory.security.principals.UserPrincipal;
import com.example.inventory.services.ClusterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clusters")
public class ClusterController {
    private final ClusterService clusterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'WORKER') and principal instanceof T(com.example.inventory.security.principals.UserPrincipal)")
    public ResponseEntity<List<ClustersWithUserDetailsDTO>> getClusters(
            @AuthenticationPrincipal UserPrincipal authentication,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            @RequestParam(value = "workerId", required = false) Long workerId
    )
    {
        List<ClustersWithUserDetailsDTO> clusters = clusterService.findClustersWithUserDetails(authentication, ownerId, workerId);
        return ResponseEntity.ok(clusters);
    }

    @PostMapping("/{clusterId}/workers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') and principal instanceof T(com.example.inventory.security.principals.UserPrincipal)")
    public ResponseEntity<?> addWorkerToCluster(
            @AuthenticationPrincipal UserPrincipal authentication,
            @PathVariable("clusterId") UUID clusterId,
            @Valid @RequestBody WorkerAssigmentDTO dto
    ) throws BadRequestException {
        clusterService.addWorkerToCluster(authentication.telegramId(), clusterId, dto.workerId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clusterId}/workers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') and principal instanceof T(com.example.inventory.security.principals.UserPrincipal)")
    public ResponseEntity<?> removeWorkerFromCluster(
            @AuthenticationPrincipal UserPrincipal authentication,
            @PathVariable("clusterId") UUID clusterId,
            @Valid @RequestBody WorkerAssigmentDTO workerAssigmentDTO
    ) throws BadRequestException {
        clusterService.removeWorkerFromCluster(authentication.telegramId(), clusterId, workerAssigmentDTO.workerId());
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTALLER', 'ADMIN')")
    public ResponseEntity<DevicesTempSecretDTO> registerNewCluster(@Valid @RequestBody RegisterNewClusterDTO registerNewClusterDTO) throws BadRequestException{
        DevicesTempSecretDTO devicesTempSecretDTO = clusterService.registerNewCluster(registerNewClusterDTO);
        return ResponseEntity.ok(devicesTempSecretDTO);
    }
}
