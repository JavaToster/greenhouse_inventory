package com.example.inventory.store;

import com.example.inventory.models.Cluster;
import com.example.inventory.repositories.postgres.ClusterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClusterStore implements GenericStore<Cluster, UUID> {
    private final ClusterRepository clusterRepository;

    public long count(){
        return clusterRepository.count();
    }

    @Override
    public Cluster save(Cluster cluster){
        return clusterRepository.save(cluster);
    }

    public List<Cluster> findAll(){
        return clusterRepository.findAllWithDetails();
    }


    public List<Cluster> findByOwner(long ownerId){
        return clusterRepository.findByOwnerIdWithDetails(ownerId);
    }

    @Override
    public Cluster findById(UUID id){
        return clusterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Кластера с таким id не существует"));
    }

    public List<Cluster> findByWorker(long workerId){
        return clusterRepository.findByWorkerIdsContainingWithDetails(workerId);
    }
}