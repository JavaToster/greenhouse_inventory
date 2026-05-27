package com.example.inventory.clients;

import com.example.inventory.DTO.user.UserInfoBatchDTO;
import com.example.inventory.DTO.user.UserInfoDTO;
import com.example.inventory.configurations.feign.UserClientErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "auth-user-service",
        path = "/api/users",
        url = "${app.clients.user.url}",
        configuration = UserClientErrorDecoder.class
)
public interface UserClient {
    @GetMapping("/{telegramId}")
    UserInfoDTO getUser(@PathVariable("telegramId") long telegramId);

    @PostMapping("/batch")
    List<UserInfoDTO> getUsers(@RequestBody UserInfoBatchDTO dto);
}
