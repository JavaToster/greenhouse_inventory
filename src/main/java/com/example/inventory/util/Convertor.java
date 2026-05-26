package com.example.inventory.util;

import com.example.inventory.DTO.cluster.ClusterInfoDTO;
import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.models.Cluster;
import com.example.inventory.models.Device;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Convertor {
    private final ModelMapper modelMapper;

    public List<ClusterInfoDTO> convertToClusterInfoDTO(List<Cluster> clusters) {
        return clusters.stream()
                .map(this::convertToClusterInfoDTO)
                .toList();
    }

    public DeviceInfoDTO convertToDeviceInfoDTO(Device device){
        return modelMapper.map(device, DeviceInfoDTO.class);
    }

    public List<DeviceInfoDTO> convertToDeviceInfoDTO(List<Device> devices){
        return devices.stream()
                .map(this::convertToDeviceInfoDTO)
                .toList();
    }

    public ClusterInfoDTO convertToClusterInfoDTO(Cluster cluster){
        ClusterInfoDTO clusterInfoDTO = modelMapper.map(cluster, ClusterInfoDTO.class);

        clusterInfoDTO.setOwnerId(cluster.getOwnerId());
        clusterInfoDTO.setDevices(convertToDeviceInfoDTO(cluster.getDevices()));
        clusterInfoDTO.setWorkerIds(cluster.getWorkerIds());

        return clusterInfoDTO;
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
