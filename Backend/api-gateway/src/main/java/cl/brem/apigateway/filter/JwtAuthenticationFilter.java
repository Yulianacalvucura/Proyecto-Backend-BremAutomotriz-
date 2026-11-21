package cl.brem.apigateway.filter;

import cl.brem.apigateway.exception.ErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private static final int LONGITUD_MINIMA_SECRETO = 32;

	private static final String PREFIJO_BEARER = "Bearer ";

	private static final Set<String> RUTAS_PUBLICAS = Set.of(
			"/auth/swagger-ui.html",
			"/auth/v3/api-docs",
			"/taller/v3/api-docs",
			"/swagger-ui.html",
			"/v3/api-docs",
			"/actuator/health");

	private final SecretKey key;

	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret, ObjectMapper objectMapper) {
		if (secret == null || secret.length() < LONGITUD_MINIMA_SECRETO) {
			throw new IllegalStateException("jwt.secret debe tener al menos " + LONGITUD_MINIMA_SECRETO
					+ " caracteres (configura JWT_SECRET con un valor seguro)");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		HttpServletRequestWrapperWithHeaders peticionSegura =
				new HttpServletRequestWrapperWithHeaders(request);

		if (esPublica(request)) {
			filterChain.doFilter(peticionSegura, response);
			return;
		}

		String autorizacion = request.getHeader("Authorization");
		if (autorizacion == null || !autorizacion.startsWith(PREFIJO_BEARER)
				|| autorizacion.substring(PREFIJO_BEARER.length()).isBlank()) {
			log.warn("Token no proporcionado en {}", request.getRequestURI());
			responder401(response, "Token no proporcionado");
			return;
		}

		String token = autorizacion.substring(PREFIJO_BEARER.length());
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();

			String roles = extraerRoles(claims);
			if (roles == null || roles.isBlank()) {
				log.warn("Token sin claim de roles rechazado en {}", request.getRequestURI());
				responder401(response, "Token inválido o expirado");
				return;
			}

			agregarSiExiste(peticionSegura, "X-User-Username", claims.getSubject());
			agregarSiExiste(peticionSegura, "X-User-Roles", roles);
			agregarSiExiste(peticionSegura, "X-User-Id", aTexto(claims.get("userId")));
			agregarSiExiste(peticionSegura, "X-User-Nombre", aTexto(claims.get("nombre")));

			filterChain.doFilter(peticionSegura, response);
		} catch (JwtException | IllegalArgumentException ex) {
			log.warn("Token rechazado en {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
			responder401(response, "Token inválido o expirado");
		}
	}

	private boolean esPublica(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String ruta = request.getRequestURI();
		if ("POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(ruta)) {
			return true;
		}
		if (RUTAS_PUBLICAS.contains(ruta)) {
			return true;
		}
		return ruta.startsWith("/auth/swagger-ui/")
				|| ruta.startsWith("/auth/v3/api-docs/")
				|| ruta.startsWith("/taller/v3/api-docs/")
				|| ruta.startsWith("/swagger-ui/")
				|| ruta.startsWith("/v3/api-docs/");
	}

	private String extraerRoles(Claims claims) {
		Object valor = claims.get("roles");
		if (valor == null) {
			return null;
		}
		if (valor instanceof List<?> lista) {
			return lista.stream()
					.filter(Objects::nonNull)
					.map(String::valueOf)
					.collect(Collectors.joining(","));
		}
		return String.valueOf(valor);
	}

	private String aTexto(Object valor) {
		return valor == null ? null : String.valueOf(valor);
	}

	private void agregarSiExiste(HttpServletRequestWrapperWithHeaders peticion, String nombre, String valor) {
		if (valor != null && !valor.isBlank()) {
			peticion.agregarCabeceraVerificada(nombre, valor);
		}
	}

	private void responder401(HttpServletResponse response, String mensaje) throws IOException {
		HttpStatus estado = HttpStatus.UNAUTHORIZED;
		ErrorResponse cuerpo = ErrorResponse.builder()
				.status(estado.value())
				.error("No autorizado")
				.mensaje(mensaje)
				.build();

		response.setStatus(estado.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), cuerpo);
	}

}
