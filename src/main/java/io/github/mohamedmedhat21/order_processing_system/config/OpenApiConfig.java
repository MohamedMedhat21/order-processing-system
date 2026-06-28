package io.github.mohamedmedhat21.order_processing_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openAPI() {
		final String bearerScheme = "bearerAuth";
		return new OpenAPI()
				.info(new Info()
						.title("Order Processing System API")
						.description("Concurrent order processing — e-commerce backend")
						.version("v1"))
				.addSecurityItem(new SecurityRequirement().addList(bearerScheme))
				.components(new Components().addSecuritySchemes(bearerScheme,
						new SecurityScheme()
								.name(bearerScheme)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
