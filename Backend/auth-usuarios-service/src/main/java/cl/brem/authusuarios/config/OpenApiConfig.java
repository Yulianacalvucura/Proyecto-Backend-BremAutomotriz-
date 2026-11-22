package cl.brem.authusuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Value("${APP_PUBLIC_URL:http://localhost:9090}")
	private String urlPublica = "http://localhost:9090";

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Brem Automotriz - Auth Usuarios")
						.version("1.0.0")
						.description("Gestión de usuarios y emisión de JWT del backend Brem Automotriz"))
				.servers(List.of(new Server().url(urlPublica).description("API Gateway")));
	}

}
