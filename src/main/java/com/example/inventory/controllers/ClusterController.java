package com.example.inventory.controllers;

import com.example.inventory.DTO.cluster.ClusterInfoDTO;
import com.example.inventory.DTO.cluster.ClustersWithUserDetailsDTO;
import com.example.inventory.DTO.cluster.RegisterNewClusterDTO;
import com.example.inventory.DTO.cluster.WorkerAssigmentDTO;
import com.example.inventory.DTO.device.DevicesTempSecretDTO;
import com.example.inventory.security.UserPrincipal;
import com.example.inventory.services.ClusterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clusters")
public class ClusterController {
    private final ClusterService clusterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'WORKER')")
    public ResponseEntity<List<ClustersWithUserDetailsDTO>> getClusters(
            Authentication authentication,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            @RequestParam(value = "workerId", required = false) Long workerId
    )
    {
        List<ClustersWithUserDetailsDTO> clusters = clusterService.findClustersWithUserDetails((UserPrincipal) authentication.getPrincipal(), ownerId, workerId);
        return ResponseEntity.ok(clusters);
    }

    @PostMapping("/{clusterId}/workers")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> addWorkerToCluster(
            Authentication authentication,
            @PathVariable("clusterId") UUID clusterId,
            @Valid @RequestBody WorkerAssigmentDTO dto
    ) throws BadRequestException {
        clusterService.addWorkerToCluster(((UserPrincipal) authentication.getPrincipal()).telegramId(), clusterId, dto.getWorkerId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clusterId}/workers")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> removeWorkerFromCluster(
            Authentication authentication,
            @PathVariable("clusterId") UUID clusterId,
            @Valid @RequestBody WorkerAssigmentDTO workerAssigmentDTO
    ) throws BadRequestException {
        clusterService.removeWorkerFromCluster(((UserPrincipal) authentication.getPrincipal()).telegramId(), clusterId, workerAssigmentDTO.getWorkerId());
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTALLER', 'ADMIN')")
    public ResponseEntity<DevicesTempSecretDTO> registerNewCluster(@Valid @RequestBody RegisterNewClusterDTO registerNewClusterDTO){
        DevicesTempSecretDTO devicesTempSecretDTO = clusterService.registerNewCluster(registerNewClusterDTO);
        return ResponseEntity.ok(devicesTempSecretDTO);
    }
}
