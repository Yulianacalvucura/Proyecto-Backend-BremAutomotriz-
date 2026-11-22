package cl.brem.taller.config;

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
						.title("API BREM - Servicio de Taller")
						.version("1.0")
						.description("Gestión de clientes, vehículos y órdenes de trabajo"))
				.servers(List.of(
						new Server().url(urlPublica).description("Gateway")));
	}

}
