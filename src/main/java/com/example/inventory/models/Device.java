package com.example.inventory.models;

import com.example.inventory.util.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Data
@Entity
@Table(name = "devices")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Device implements Persistable<UUID> {
    @Id
    @Column(name = "device_id", nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "secret", nullable = false)
    private String secret;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeviceStatus status;

    @ManyToOne
    @JoinColumn(name = "cluster_id", referencedColumnName = "id")
    private Cluster cluster;

    @Transient
    @ToString.Exclude
    private String rawSecret;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew(){
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew(){
        this.isNew = false;
    }
}
