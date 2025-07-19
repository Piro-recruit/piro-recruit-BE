package com.pirogramming.recruit.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI apiInfo() {
		return new OpenAPI()
			.info(new Info()
				.title("Recruit API")
				.description("Recruit REST API 문서입니다.")
				.version("v1.0.0")
				.contact(new Contact()
					.name("김규일")
					.email("rlarbdlf222@gmail.com")
					.url("https://github.com/Piro-recruit/piro-recruit-BE"))
				.license(new License()
					.name("MIT License")
					.url("https://opensource.org/licenses/MIT"))
			)
			.addSecurityItem(new SecurityRequirement().addList("JWT"))
			.components(new Components().addSecuritySchemes("JWT", new SecurityScheme()
				.name("bearerAuth")
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")));
	}
}
