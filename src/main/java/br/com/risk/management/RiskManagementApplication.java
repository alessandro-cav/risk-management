package br.com.risk.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.com.risk.management.config.jwt.JWTConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SpringBootApplication
@SecurityScheme(name = JWTConstants.BEARER_AUTHENTICATION, type = SecuritySchemeType.HTTP, bearerFormat = JWTConstants.JWT, scheme = JWTConstants.SCHEME_BEARER)
@OpenAPIDefinition(info = @Info(title = JWTConstants.TITULO, version = JWTConstants.VERSAO), security = {
		@SecurityRequirement(name = JWTConstants.BEARER_AUTHENTICATION) })
public class RiskManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(RiskManagementApplication.class, args);
	}

}
