package cl.brem.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuracion.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return new CorsFilter(fuente);
	}

}
