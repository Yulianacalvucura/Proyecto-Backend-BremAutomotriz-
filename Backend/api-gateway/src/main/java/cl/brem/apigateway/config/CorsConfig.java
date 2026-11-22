package cl.brem.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

	private static final String ORIGENES_POR_DEFECTO = "http://localhost:*,http://127.0.0.1:*";

	@Value("${CORS_ORIGINS:http://localhost:*,http://127.0.0.1:*}")
	private String origenesPermitidos = ORIGENES_POR_DEFECTO;

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOriginPatterns(obtenerOrigenes());
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuracion.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return new CorsFilter(fuente);
	}

	private List<String> obtenerOrigenes() {
		return Arrays.stream(origenesPermitidos.split(","))
				.map(String::trim)
				.filter(origen -> !origen.isBlank())
				.toList();
	}

}
