package com.example.inventory.controllers;

import com.example.inventory.DTO.cluster.ClustersWithUserDetailsDTO;
import com.example.inventory.DTO.cluster.RegisterNewClusterDTO;
import com.example.inventory.DTO.cluster.WorkerAssignmentDTO;
import com.example.inventory.DTO.device.DevicesTempSecretDTO;
import com.example.inventory.security.principals.UserPrincipal;
import com.example.inventory.services.ClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Clusters", description = "Endpoints for managing greenhouse clusters and their workers")
@SecurityRequirement(name = "bearerAuth")
public class ClusterController {
    private final ClusterService clusterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'WORKER') and principal instanceof T(com.example.inventory.security.principals.UserPrincipal)")
    @Operation(summary = "Get clusters filtered by owner or worker")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clusters returned successfully")
    })
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
    @Operation(summary = "Add a worker to a cluster")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Worker added to cluster successfully")
    })
    public ResponseEntity<Void> addWorkerToCluster(
            @AuthenticationPrincipal UserPrincipal authentication,
            @PathVariable("clusterId") UUID clusterId,
            @Valid @RequestBody WorkerAssignmentDTO dto
    ) throws BadRequestException {
        clusterService.addWorkerToCluster(authentication.telegramId(), clusterId, dto.workerId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{clusterId}/workers")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') and principal instanceof T(com.example.inventory.security.principals.UserPrincipal)")
    @Operation(summary = "Remove a worker from a cluster")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Worker removed from cluster successfully")
    })
    public ResponseEntity<Void> removeWorkerFromCluster(
            @AuthenticationPrincipal UserPrincipal authentication,
            @PathVariable("clusterId") UUID clusterId,
            @Valid @RequestBody WorkerAssignmentDTO workerAssignmentDTO
    ) throws BadRequestException {
        clusterService.removeWorkerFromCluster(authentication.telegramId(), clusterId, workerAssignmentDTO.workerId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTALLER', 'ADMIN')")
    @Operation(summary = "Register a new cluster")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cluster registered successfully")
    })
    public ResponseEntity<DevicesTempSecretDTO> registerNewCluster(@Valid @RequestBody RegisterNewClusterDTO registerNewClusterDTO) throws BadRequestException{
        DevicesTempSecretDTO devicesTempSecretDTO = clusterService.registerNewCluster(registerNewClusterDTO);
        return ResponseEntity.ok(devicesTempSecretDTO);
    }
}