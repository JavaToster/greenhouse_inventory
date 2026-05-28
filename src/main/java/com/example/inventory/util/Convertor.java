package com.example.inventory.util;

import com.example.inventory.DTO.cluster.ClusterInfoDTO;
import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.models.Cluster;
import com.example.inventory.models.Device;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Convertor {
    public List<ClusterInfoDTO> convertToClusterInfoDTO(List<Cluster> clusters) {
        return clusters.stream()
                .map(this::convertToClusterInfoDTO)
                .toList();
    }

    public DeviceInfoDTO convertToDeviceInfoDTO(Device device){
        return new DeviceInfoDTO(device.getId(), device.getStatus());
    }

    public List<DeviceInfoDTO> convertToDeviceInfoDTO(List<Device> devices){
        return devices.stream()
                .map(this::convertToDeviceInfoDTO)
                .toList();
    }

    public ClusterInfoDTO convertToClusterInfoDTO(Cluster cluster){
        return new ClusterInfoDTO(
                cluster.getId(),
                cluster.getName(),
                cluster.getDescription(),
                cluster.getOwnerId(),
                convertToDeviceInfoDTO(cluster.getDevices()),
                cluster.getWorkerIds()
        );
    }

    // public List<TaskInfoDTO> convertToTaskInfoDTO(List<Task> tasks){
    //     return tasks.stream()
    //             .map(this::convertToTaskInfoDTO)
    //             .toList();
    // }

    // public TaskInfoDTO convertToTaskInfoDTO(Task task) {
    //     return modelMapper.map(task, TaskInfoDTO.class);
    // }
}
