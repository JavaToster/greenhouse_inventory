package com.example.inventory.configurations.feign;

import com.example.inventory.DTO.error.ErrorResponseDTO;
import feign.Response;
import feign.codec.ErrorDecoder;
import jakarta.persistence.EntityNotFoundException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class UserClientErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder errorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String defaultMessage = "Something went wrong in external service";
        if (response.body() != null){
            try(InputStream inputStream = response.body().asInputStream()){
                ErrorResponseDTO errorResponseDTO = objectMapper.readValue(inputStream, ErrorResponseDTO.class);
                if (errorResponseDTO.message() != null){
                    defaultMessage = errorResponseDTO.message();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return switch (response.status()){
            case 404 -> new EntityNotFoundException(defaultMessage);
            default -> errorDecoder.decode(methodKey, response);
        };
    }
}
