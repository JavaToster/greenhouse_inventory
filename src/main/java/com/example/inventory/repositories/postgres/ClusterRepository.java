package com.example.inventory.repositories.postgres;

import com.example.inventory.models.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, UUID> {

    @Query("SELECT DISTINCT c FROM Cluster c " +
            "LEFT JOIN FETCH c.devices " +
            "LEFT JOIN FETCH c.workerIds")
    List<Cluster> findAllWithDetails();

    @Query("SELECT DISTINCT c FROM Cluster c " +
            "LEFT JOIN FETCH c.devices " +
            "LEFT JOIN FETCH c.workerIds " +
            "WHERE c.ownerId = :ownerId")
    List<Cluster> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    @Query("SELECT DISTINCT c FROM Cluster c " +
            "LEFT JOIN FETCH c.devices " +
            "LEFT JOIN FETCH c.workerIds " +
            "WHERE :workerId MEMBER OF c.workerIds")
    List<Cluster> findByWorkerIdsContainingWithDetails(@Param("workerId") Long workerId);
}