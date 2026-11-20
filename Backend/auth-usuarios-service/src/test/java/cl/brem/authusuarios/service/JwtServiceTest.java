package cl.brem.authusuarios.service;

import cl.brem.authusuarios.model.Rol;
import cl.brem.authusuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRETO = "secreto-brem-automotriz-super-seguro-1234567890";

    @Test
    void generarToken_shouldIncluirClaimsDelGatewayYVigenciaDe24Horas() {
        // Arrange
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secreto", SECRETO);
        Usuario usuario = new Usuario(3L, "Jefe de Taller", "jefe@brem.cl", "$2b$hash", Rol.JEFE, true, null);

        // Act
        String token = jwtService.generarToken(usuario);

        // Assert
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("jefe@brem.cl", claims.getSubject());
        assertEquals(List.of("JEFE"), claims.get("roles"));
        assertEquals(3, ((Number) claims.get("userId")).intValue());
        assertEquals("Jefe de Taller", claims.get("nombre"));

        long horas = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / (1000L * 60 * 60);
        assertEquals(24, horas);
    }

    @Test
    void validarSecreto_whenMenorA32Caracteres_shouldLanzarIllegalState() {
        // Arrange
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secreto", "corto");

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtService::validarSecreto);
        assertTrue(ex.getMessage().contains("32 caracteres"));
    }
}
