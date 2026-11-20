package cl.brem.authusuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Brem Automotriz – Servicio de Autenticación")
                        .version("1.0")
                        .description("Login JWT, registro y gestión de usuarios"))
                .servers(List.of(
                        new Server().url("http://localhost:9090").description("Gateway")
                ));
    }
}
