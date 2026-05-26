package com.example.inventory.DTO.cluster;

import com.example.inventory.DTO.device.DeviceInfoDTO;
import com.example.inventory.DTO.user.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClustersWithUserDetailsDTO {
    private UUID id;
    private String name;
    private String description;

    private UserInfoDTO owner;
    private List<UserInfoDTO> workers;
    private List<DeviceInfoDTO> devices;

}
