package cl.brem.taller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] RUTAS_CLIENTES = {"/clientes", "/clientes/**"};

	private static final String[] RUTAS_VEHICULOS = {"/vehiculos", "/vehiculos/**"};

	private static final String[] RUTAS_ORDENES = {"/ordenes", "/ordenes/**"};

	@Bean
	public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
		return new RestAuthenticationEntryPoint(objectMapper);
	}

	@Bean
	public CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
		return new CustomAccessDeniedHandler(objectMapper);
	}

	@Bean
	public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
		return new RequestHeaderAuthenticationFilter();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter,
			RestAuthenticationEntryPoint restAuthenticationEntryPoint,
			CustomAccessDeniedHandler customAccessDeniedHandler) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(reglas -> reglas
						.requestMatchers(
								"/swagger-ui/**", "/swagger-ui.html",
								"/v3/api-docs/**", "/v3/api-docs",
								"/taller/swagger-ui/**", "/taller/swagger-ui.html",
								"/taller/v3/api-docs/**", "/taller/v3/api-docs",
								"/actuator/health").permitAll()
						.requestMatchers(HttpMethod.GET, RUTAS_CLIENTES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.POST, RUTAS_CLIENTES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.PUT, RUTAS_CLIENTES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.GET, RUTAS_VEHICULOS).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.POST, RUTAS_VEHICULOS).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.PUT, RUTAS_VEHICULOS).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.GET, RUTAS_ORDENES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.POST, RUTAS_ORDENES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.PUT, RUTAS_ORDENES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.PATCH, RUTAS_ORDENES).hasAnyAuthority("ADMIN", "JEFE")
						.requestMatchers(HttpMethod.DELETE, RUTAS_ORDENES).hasAnyAuthority("ADMIN", "JEFE")
						.anyRequest().authenticated())
				.exceptionHandling(excepciones -> excepciones
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler))
				.addFilterBefore(requestHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

}
