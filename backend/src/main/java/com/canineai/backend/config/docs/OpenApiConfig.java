package com.canineai.backend.config.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide JWT Bearer Token to authorize REST API endpoints access")
                        )
                )
                .info(new Info()
                        .title("CanineAI Dental CBCT Analysis Platform API")
                        .version("1.0.0")
                        .description("REST API Backend services powering Android app and Web clients workflows.")
                        .contact(new Contact()
                                .name("Metro Dental Diagnostics Tech Support")
                                .email("tech-support@metrodiagnostics.com")
                        )
                        .license(new License()
                                .name("Proprietary Medical EMR License")
                        )
                );
    }
}
