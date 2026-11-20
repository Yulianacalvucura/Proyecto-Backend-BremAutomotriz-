package cl.brem.authusuarios.config;

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
import java.util.stream.Collectors;

/**
 * Igual que en los microservicios de Perlita: el gateway ya validó el JWT y
 * manda el usuario y sus roles en headers. Confiamos en esos headers porque
 * el servicio no está expuesto directamente al cliente.
 */
public class RequestHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String username = request.getHeader("X-User-Username");
        String rolesStr = request.getHeader("X-User-Roles");

        if (username != null && rolesStr != null && !rolesStr.trim().isEmpty()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
                    .map(rol -> new SimpleGrantedAuthority(rol.trim().toUpperCase()))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
