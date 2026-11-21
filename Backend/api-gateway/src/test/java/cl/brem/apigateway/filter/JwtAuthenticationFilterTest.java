package cl.brem.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

	private static final String SECRETO = "secreto-de-prueba-brem-2026-mas-de-32-caracteres";

	private static final String OTRO_SECRETO = "otro-secreto-de-prueba-brem-2026-mas-de-32-caracteres";

	private JwtAuthenticationFilter filtro;

	private record Resultado(MockHttpServletResponse respuesta, Map<String, String> cabeceras, boolean continuo) {
	}

	@BeforeEach
	void setUp() {
		filtro = new JwtAuthenticationFilter(SECRETO, new ObjectMapper());
	}

	@Test
	void tokenValido_reenviaCabecerasVerificadasAlDestino() throws Exception {
		// Arrange
		String token = generarToken("jefe@brem.cl", "JEFE", 7L, "Jefe Brem", 60_000L, SECRETO);
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", "Bearer " + token);

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assertTrue(resultado.continuo());
		assertEquals(200, resultado.respuesta().getStatus());
		assertEquals("jefe@brem.cl", resultado.cabeceras().get("X-User-Username"));
		assertEquals("JEFE", resultado.cabeceras().get("X-User-Roles"));
		assertEquals("7", resultado.cabeceras().get("X-User-Id"));
		assertEquals("Jefe Brem", resultado.cabeceras().get("X-User-Nombre"));
	}

	@Test
	void sinHeaderAuthorization_devuelve401ConMensajeTokenNoProporcionado() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token no proporcionado");
		assertFalse(resultado.continuo());
	}

	@Test
	void headerSinPrefijoBearer_devuelve401() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", generarToken("a@b.cl", "ADMIN", 1L, "A", 60_000L, SECRETO));

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token no proporcionado");
	}

	@Test
	void tokenConFirmaIncorrecta_devuelve401() throws Exception {
		// Arrange
		String token = generarToken("a@b.cl", "ADMIN", 1L, "A", 60_000L, OTRO_SECRETO);
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", "Bearer " + token);

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token inválido o expirado");
	}

	@Test
	void tokenExpirado_devuelve401() throws Exception {
		// Arrange
		String token = generarToken("a@b.cl", "ADMIN", 1L, "A", -60_000L, SECRETO);
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", "Bearer " + token);

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token inválido o expirado");
	}

	@Test
	void tokenConFormatoBasura_devuelve401() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", "Bearer este-no-es-un-jwt");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token inválido o expirado");
	}

	@Test
	void tokenSinClaimDeRol_devuelve401() throws Exception {
		// Arrange
		Date ahora = new Date();
		String token = Jwts.builder()
				.subject("sin-rol@brem.cl")
				.claim("userId", 3L)
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + 60_000L))
				.signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
				.compact();
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", "Bearer " + token);

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token inválido o expirado");
	}

	@Test
	void tokenJefeConHeaderFalsificadoAdmin_destinoRecibeJefe() throws Exception {
		// Arrange
		String token = generarToken("jefe@brem.cl", "JEFE", 7L, "Jefe Brem", 60_000L, SECRETO);
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");
		solicitud.addHeader("Authorization", "Bearer " + token);
		solicitud.addHeader("X-User-Roles", "ADMIN");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assertTrue(resultado.continuo());
		assertEquals("JEFE", resultado.cabeceras().get("X-User-Roles"));
	}

	@Test
	void rutaPublicaConHeaderFalsificadoAdmin_noLlegaAlDestino() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("POST", "/auth/login");
		solicitud.addHeader("X-User-Roles", "ADMIN");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assertTrue(resultado.continuo());
		assertNull(resultado.cabeceras().get("X-User-Roles"));
	}

	@Test
	void postLoginSinToken_pasa() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("POST", "/auth/login");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assertTrue(resultado.continuo());
		assertEquals(200, resultado.respuesta().getStatus());
	}

	@Test
	void getAuthUsuariosSinToken_devuelve401() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("GET", "/auth/usuarios");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assert401(resultado.respuesta(), "Token no proporcionado");
	}

	@Test
	void rutasDeSwaggerYApiDocsSinToken_pasan() throws Exception {
		// Arrange
		List<String> rutas = List.of(
				"/swagger-ui/index.html",
				"/v3/api-docs",
				"/auth/swagger-ui.html",
				"/auth/v3/api-docs",
				"/taller/v3/api-docs",
				"/actuator/health");

		// Act - Assert
		for (String ruta : rutas) {
			Resultado resultado = filtrar(solicitud("GET", ruta));
			assertTrue(resultado.continuo(), "La ruta pública " + ruta + " debía pasar sin token");
			assertEquals(200, resultado.respuesta().getStatus());
		}
	}

	@Test
	void optionsEnCualquierRutaSinToken_pasa() throws Exception {
		// Arrange
		MockHttpServletRequest solicitud = solicitud("OPTIONS", "/ordenes/5");

		// Act
		Resultado resultado = filtrar(solicitud);

		// Assert
		assertTrue(resultado.continuo());
	}

	@Test
	void secretoCorto_lanzaIllegalStateException() {
		// Arrange - Act - Assert
		assertThrows(IllegalStateException.class, () -> new JwtAuthenticationFilter("corto", new ObjectMapper()));
	}

	private Resultado filtrar(MockHttpServletRequest solicitud) throws Exception {
		MockHttpServletResponse respuesta = new MockHttpServletResponse();
		Map<String, String> cabeceras = new HashMap<>();
		AtomicBoolean continuo = new AtomicBoolean(false);
		FilterChain cadena = (req, res) -> {
			continuo.set(true);
			HttpServletRequest peticion = (HttpServletRequest) req;
			for (String nombre : List.of("X-User-Username", "X-User-Roles", "X-User-Id", "X-User-Nombre")) {
				cabeceras.put(nombre, peticion.getHeader(nombre));
			}
		};
		filtro.doFilter(solicitud, respuesta, cadena);
		return new Resultado(respuesta, cabeceras, continuo.get());
	}

	private MockHttpServletRequest solicitud(String metodo, String ruta) {
		return new MockHttpServletRequest(metodo, ruta);
	}

	private String generarToken(String email, String rol, long userId, String nombre, long milisegundosExpiracion,
			String secreto) {
		Date ahora = new Date();
		return Jwts.builder()
				.subject(email)
				.claim("roles", List.of(rol))
				.claim("userId", userId)
				.claim("nombre", nombre)
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + milisegundosExpiracion))
				.signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
				.compact();
	}

	private String contenido(MockHttpServletResponse respuesta) {
		return new String(respuesta.getContentAsByteArray(), StandardCharsets.UTF_8);
	}

	private void assert401(MockHttpServletResponse respuesta, String mensaje) {
		assertEquals(401, respuesta.getStatus());
		assertTrue(respuesta.getContentType().startsWith("application/json"));
		assertTrue(contenido(respuesta).contains("\"status\":401"));
		assertTrue(contenido(respuesta).contains("\"error\":\"No autorizado\""));
		assertTrue(contenido(respuesta).contains("\"mensaje\":\"" + mensaje + "\""));
		assertFalse(contenido(respuesta).contains("\"timestamp\""));
	}

}
