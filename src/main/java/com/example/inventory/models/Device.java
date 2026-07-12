package com.example.inventory.models;

import com.example.inventory.util.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;

import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Getter
@Setter
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id", referencedColumnName = "id")
    private Cluster cluster;

    @Transient
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
