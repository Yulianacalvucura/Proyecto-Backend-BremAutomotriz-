package cl.brem.authusuarios.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers(
								"/swagger-ui/**", "/swagger-ui.html",
								"/v3/api-docs/**", "/v3/api-docs",
								"/auth/swagger-ui/**", "/auth/swagger-ui.html",
								"/auth/v3/api-docs/**", "/auth/v3/api-docs",
								"/actuator/health").permitAll()
						.requestMatchers("/auth/**").hasAuthority("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(excepciones -> excepciones
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler))
				.addFilterBefore(requestHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

}
