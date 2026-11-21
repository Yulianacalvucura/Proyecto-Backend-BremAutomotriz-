package cl.brem.taller.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Confía en los headers X-User-Username y X-User-Roles únicamente porque el
 * api-gateway los inyecta DESPUÉS de validar el JWT.
 *
 * IMPORTANTE EN PRODUCCIÓN: el puerto 8081 no debe ser accesible directamente
 * desde la red; cualquiera podría falsificar estos headers y obtener rol ADMIN.
 * Solo el gateway (puerto 9090) debe poder alcanzar este servicio.
 */
public class RequestHeaderAuthenticationFilter extends OncePerRequestFilter {

	public static final String HEADER_USERNAME = "X-User-Username";

	public static final String HEADER_ROLES = "X-User-Roles";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String username = request.getHeader(HEADER_USERNAME);
		String rolesHeader = request.getHeader(HEADER_ROLES);

		if (username != null && !username.isBlank()
				&& rolesHeader != null && !rolesHeader.isBlank()
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			List<SimpleGrantedAuthority> autoridades = Arrays.stream(rolesHeader.split(","))
					.map(String::trim)
					.filter(rol -> !rol.isEmpty())
					.map(SimpleGrantedAuthority::new)
					.toList();

			UsernamePasswordAuthenticationToken autenticacion =
					new UsernamePasswordAuthenticationToken(username, null, autoridades);
			SecurityContextHolder.getContext().setAuthentication(autenticacion);
		}

		filterChain.doFilter(request, response);
	}

}
