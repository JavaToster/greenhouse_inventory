package com.example.inventory.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "clusters")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cluster {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @OneToMany(mappedBy = "cluster")
    List<Device> devices;

    @ElementCollection
    @CollectionTable(
            name = "clusters_workers",
            joinColumns = @JoinColumn(name = "cluster_id"),
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @Column(name = "worker_id")
    private Set<Long> workerIds = new HashSet<>();

    @Transient
    private List<UUID> taskIds;

    public void addWorker(Long workerId){
        this.workerIds.add(workerId);
    }

    public void removeWorker(Long workerId){
        this.workerIds.remove(workerId);
    }
}
