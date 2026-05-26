package com.example.inventory.repositories.postgres;

import com.example.inventory.models.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, UUID> {
    List<Cluster> findByOwnerId(Long ownerId);

    List<Cluster> findByWorkerIdsContaining(Long workerId);
}
