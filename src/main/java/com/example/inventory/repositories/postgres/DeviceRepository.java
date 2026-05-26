package com.example.inventory.repositories.postgres;

import com.example.inventory.models.Device;
import com.example.inventory.util.enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Device d
    set d.status = :status
    where d.cluster.id = :clusterId
""")
    void updateStatusByClusterId(UUID clusterId, DeviceStatus status);

    long countByStatus(DeviceStatus status);

    List<Device> findByClusterId(UUID clusterId);
}
