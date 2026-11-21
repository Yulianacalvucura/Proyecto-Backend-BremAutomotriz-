package cl.brem.authusuarios.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Brem Automotriz - Auth Usuarios",
				version = "1.0.0",
				description = "Gestión de usuarios y emisión de JWT del backend Brem Automotriz"),
		servers = @Server(url = "http://localhost:9090", description = "API Gateway"))
public class OpenApiConfig {

}
