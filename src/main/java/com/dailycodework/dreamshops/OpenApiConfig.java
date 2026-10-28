package com.dailycodework.dreamshops;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Describes the API for Swagger UI, including the bearer token, so its "Authorize" button works. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI dreamShopsApi() {
        return new OpenAPI()
                .info(new Info().title("DreamShops API").version("v1")
                        .description("Products, categories, images, carts and orders. Log in with POST /api/v1/auth/login "
                                + "and paste the token into Authorize."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
