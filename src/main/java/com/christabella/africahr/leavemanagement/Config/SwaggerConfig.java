package com.christabella.africahr.leavemanagement.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leave Management Service API")
                        .version("1.0.0")
                        .description("API for handling leave requests, balances, types, and admin approvals. Authenticated using JWT Bearer tokens."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperations().forEach(operation -> {
                operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

                ApiResponses apiResponses = operation.getResponses();
                if (apiResponses == null) {
                    apiResponses = new ApiResponses();
                    operation.setResponses(apiResponses);
                }

                ApiResponse successResponse = new ApiResponse()
                        .description("Successful operation")
                        .content(new Content().addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiResponse"))));
                apiResponses.addApiResponse("200", successResponse);
            });
        });
    }

    @Bean
    public OpenApiCustomizer addSchemas() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.addSchemas("ApiResponse", new Schema<>()
                    .type("object")
                    .addProperty("message", new Schema<>().type("string"))
                    .addProperty("data", new Schema<>().type("object"))
                    .addProperty("errors", new Schema<>().type("array").items(new Schema<>().type("string"))));
        };
    }
}

