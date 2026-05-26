package com.example.inventory.store;

import com.example.inventory.models.Device;
import com.example.inventory.repositories.postgres.DeviceRepository;
import com.example.inventory.util.enums.DeviceStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceStore implements GenericStore<Device, UUID> {
    private final DeviceRepository deviceRepository;

    public long count(){
        return deviceRepository.count();
    }

    public long count(DeviceStatus deviceStatus){
        return deviceRepository.countByStatus(deviceStatus);
    }

    public List<Device> saveAll(List<Device> devices){
        return deviceRepository.saveAll(devices);
    }

    public void updateStatusByClusterId(UUID clusterId, DeviceStatus status){
        deviceRepository.updateStatusByClusterId(clusterId, status);
    }

    public Device findById(UUID id){
        return deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Девайса с таким id не существует"));
    }

    @Override
    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    public List<Device> findByClusterId(UUID clusterId) {
        return deviceRepository.findByClusterId(clusterId);
    }

    public void remove(UUID id){
        deviceRepository.deleteById(id);
    }
}
