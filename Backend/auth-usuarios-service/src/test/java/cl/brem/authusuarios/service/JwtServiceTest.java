package cl.brem.authusuarios.service;

import cl.brem.authusuarios.model.Rol;
import cl.brem.authusuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

	private static final String SECRETO_DE_PRUEBA = "secreto-de-prueba-con-mas-de-32-caracteres-1234567890";

	private final JwtService jwtService = new JwtService(SECRETO_DE_PRUEBA, 3_600_000L);

	@Test
	void tokenGeneradoContieneSubjectRolesUserIdYNombre() {
		// Arrange
		Usuario usuario = Usuario.builder()
				.id(7L)
				.nombre("Jefe de Taller")
				.email("jefe@brem.cl")
				.rol(Rol.JEFE)
				.build();

		// Act
		String token = jwtService.generarToken(usuario);
		Claims claims = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(SECRETO_DE_PRUEBA.getBytes(StandardCharsets.UTF_8)))
				.build()
				.parseSignedClaims(token)
				.getPayload();

		// Assert
		assertEquals("jefe@brem.cl", claims.getSubject());
		assertEquals(List.of("JEFE"), claims.get("roles", List.class));
		assertEquals(7L, ((Number) claims.get("userId")).longValue());
		assertEquals("Jefe de Taller", claims.get("nombre", String.class));
		assertNotNull(claims.getIssuedAt());
		assertNotNull(claims.getExpiration());
		assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
	}

	@Test
	void secretoMenorA32CaracteresLanzaIllegalStateExceptionAlCrearElServicio() {
		// Arrange - Act - Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> new JwtService("secreto-corto", 3_600_000L));

		assertTrue(ex.getMessage().contains("32"));
	}

	@Test
	void tokenConExpiracionVencidaNoEsValidoAlParsearlo() {
		// Arrange
		JwtService servicioConExpiracionVencida = new JwtService(SECRETO_DE_PRUEBA, -60_000L);
		Usuario usuario = Usuario.builder()
				.id(1L)
				.nombre("Admin")
				.email("admin1@brem.cl")
				.rol(Rol.ADMIN)
				.build();
		String tokenVencido = servicioConExpiracionVencida.generarToken(usuario);

		// Act - Assert
		assertThrows(ExpiredJwtException.class,
				() -> Jwts.parser()
						.verifyWith(Keys.hmacShaKeyFor(SECRETO_DE_PRUEBA.getBytes(StandardCharsets.UTF_8)))
						.build()
						.parseSignedClaims(tokenVencido));
	}

}
