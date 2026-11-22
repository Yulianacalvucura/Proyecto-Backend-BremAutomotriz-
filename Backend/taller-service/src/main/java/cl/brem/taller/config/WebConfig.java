package cl.brem.taller.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${CORS_ORIGINS:http://localhost:*,http://127.0.0.1:*}")
	private String origenesPermitidos = "http://localhost:*,http://127.0.0.1:*";

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOriginPatterns(Arrays.stream(origenesPermitidos.split(","))
						.map(String::trim)
						.filter(origen -> !origen.isBlank())
						.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("Authorization", "Content-Type", "X-User-Username", "X-User-Roles", "X-User-Id")
				.allowCredentials(false);
	}

}
