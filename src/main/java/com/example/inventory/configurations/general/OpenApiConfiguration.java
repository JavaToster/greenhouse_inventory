package com.example.inventory.configurations.general;

import com.example.inventory.DTO.error.ErrorResponseDTO;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        Schema<?> errorResponseSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new io.swagger.v3.core.converter.AnnotatedType(ErrorResponseDTO.class))
                .schema;

        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("REST API for managing greenhouse clusters, devices and device authentication")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas("ErrorResponseDTO", errorResponseSchema));
    }

    @Bean
    public OperationCustomizer globalErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            Schema<?> errorSchemaRef = new Schema<>().$ref("#/components/schemas/ErrorResponseDTO");

            if (!responses.containsKey("400")) {
                Content content = new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType()
                                .schema(errorSchemaRef)
                                .example(new ErrorResponseDTO(400, "Validation failed: field 'clusterId' is invalid"))
                );
                responses.addApiResponse("400", new ApiResponse().description("Validation failed / Bad Request").content(content));
            }

            if (!responses.containsKey("401")) {
                Content content = new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType()
                                .schema(errorSchemaRef)
                                .example(new ErrorResponseDTO(401, "Full authentication is required to access this resource"))
                );
                responses.addApiResponse("401", new ApiResponse().description("Authentication required").content(content));
            }

            if (!responses.containsKey("403")) {
                Content content = new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType()
                                .schema(errorSchemaRef)
                                .example(new ErrorResponseDTO(403, "Access denied: insufficient privileges"))
                );
                responses.addApiResponse("403", new ApiResponse().description("Access denied").content(content));
            }

            if (!responses.containsKey("404")) {
                Content content = new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType()
                                .schema(errorSchemaRef)
                                .example(new ErrorResponseDTO(404, "Requested resource not found"))
                );
                responses.addApiResponse("404", new ApiResponse().description("Resource not found").content(content));
            }

            if (!responses.containsKey("409")) {
                Content content = new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType()
                                .schema(errorSchemaRef)
                                .example(new ErrorResponseDTO(409, "Conflict, please change data"))
                );
                responses.addApiResponse("409", new ApiResponse().description("Conflict").content(content));
            }

            return operation;
        };
    }
}