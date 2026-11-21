package cl.brem.apigateway.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

	@Test
	void optionsConOriginLocalhost_devuelveAccessControlAllowOrigin() throws Exception {
		// Arrange
		CorsFilter filtro = new CorsConfig().corsFilter();
		MockHttpServletRequest solicitud = new MockHttpServletRequest("OPTIONS", "/auth/login");
		solicitud.addHeader("Origin", "http://localhost:3000");
		solicitud.addHeader("Access-Control-Request-Method", "POST");
		solicitud.addHeader("Access-Control-Request-Headers", "Authorization,Content-Type");
		MockHttpServletResponse respuesta = new MockHttpServletResponse();
		FilterChain cadena = (req, res) -> { };

		// Act
		filtro.doFilter(solicitud, respuesta, cadena);

		// Assert
		assertNotNull(respuesta.getHeader("Access-Control-Allow-Origin"));
		assertEquals("http://localhost:3000", respuesta.getHeader("Access-Control-Allow-Origin"));
	}

}
