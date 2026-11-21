package cl.brem.apigateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"jwt.secret=secreto-de-prueba-brem-2026-mas-de-32-caracteres",
		"servicios.auth.url=http://localhost:8091",
		"servicios.taller.url=http://localhost:8081"
})
class ApiGatewayRoutingIntegrationTest {

	private static final String SECRETO = "secreto-de-prueba-brem-2026-mas-de-32-caracteres";

	private static final AtomicReference<Captura> CAPTURA = new AtomicReference<>();

	private static final HttpServer STUB;

	static {
		try {
			STUB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			STUB.createContext("/", exchange -> {
				Captura captura = new Captura(
						exchange.getRequestURI().getPath(),
						exchange.getRequestHeaders().getFirst("X-User-Username"),
						exchange.getRequestHeaders().getFirst("X-User-Roles"),
						exchange.getRequestHeaders().getFirst("X-User-Id"),
						exchange.getRequestHeaders().getFirst("X-User-Nombre"));
				CAPTURA.set(captura);

				String cuerpo = "path=" + captura.path()
						+ "|username=" + captura.username()
						+ "|roles=" + captura.roles()
						+ "|id=" + captura.id()
						+ "|nombre=" + captura.nombre();
				byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
				exchange.sendResponseHeaders(200, bytes.length);
				exchange.getResponseBody().write(bytes);
				exchange.close();
			});
			STUB.start();
		} catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	@LocalServerPort
	private int puerto;

	@DynamicPropertySource
	static void propiedadesDelStub(DynamicPropertyRegistry registro) {
		registro.add("servicios.auth.url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
	}

	@AfterAll
	static void detenerStub() {
		STUB.stop(0);
	}

	@Test
	void rutaAuthConTokenValido_conservaPathYReenviaHeadersVerificados() throws Exception {
		// Arrange
		Date ahora = new Date();
		String token = Jwts.builder()
				.subject("admin1@brem.cl")
				.claim("roles", List.of("ADMIN"))
				.claim("userId", 1L)
				.claim("nombre", "Admin")
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + 60_000L))
				.signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
				.compact();
		HttpRequest peticion = HttpRequest.newBuilder(
						URI.create("http://localhost:" + puerto + "/auth/usuarios?pagina=1"))
				.header("Authorization", "Bearer " + token)
				.GET()
				.build();

		// Act
		HttpResponse<String> respuesta = HttpClient.newHttpClient()
				.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		// Assert
		assertEquals(200, respuesta.statusCode());
		Captura captura = CAPTURA.get();
		assertNotNull(captura);
		assertEquals("/auth/usuarios", captura.path());
		assertEquals("admin1@brem.cl", captura.username());
		assertEquals("ADMIN", captura.roles());
		assertEquals("1", captura.id());
		assertEquals("Admin", captura.nombre());
		assertTrue(respuesta.body().contains("path=/auth/usuarios"));
	}

	private record Captura(String path, String username, String roles, String id, String nombre) {
	}

}
