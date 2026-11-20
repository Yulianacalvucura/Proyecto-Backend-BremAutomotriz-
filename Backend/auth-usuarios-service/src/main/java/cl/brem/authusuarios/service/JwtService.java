package cl.brem.authusuarios.service;

import cl.brem.authusuarios.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secreto;

    @PostConstruct
    void validarSecreto() {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
    }

    /**
     * Token HS256 con 24 h de vigencia. El gateway lee estos mismos claims:
     * subject = email, roles = [ROL], userId = id, nombre = nombre.
     */
    public String generarToken(Usuario usuario) {
        long unDiaEnMilisegundos = 1000L * 60 * 60 * 24;

        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("roles", List.of(usuario.getRol().name()))
                .claim("userId", usuario.getId())
                .claim("nombre", usuario.getNombre())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + unDiaEnMilisegundos))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
